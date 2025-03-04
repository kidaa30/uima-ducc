/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.ducc.agent.config;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.impl.DefaultClassResolver;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.uima.ducc.agent.NodeAgent;
import org.apache.uima.ducc.agent.event.AgentEventListener;
import org.apache.uima.ducc.agent.launcher.Launcher;
import org.apache.uima.ducc.agent.launcher.ManagedProcess;
import org.apache.uima.ducc.agent.processors.DefaultNodeInventoryProcessor;
import org.apache.uima.ducc.agent.processors.DefaultNodeMetricsProcessor;
import org.apache.uima.ducc.agent.processors.DefaultProcessMetricsProcessor;
import org.apache.uima.ducc.agent.processors.LinuxNodeMetricsProcessor;
import org.apache.uima.ducc.agent.processors.LinuxProcessMetricsProcessor;
import org.apache.uima.ducc.agent.processors.NodeInventoryProcessor;
import org.apache.uima.ducc.agent.processors.NodeMetricsProcessor;
import org.apache.uima.ducc.agent.processors.ProcessMetricsProcessor;
import org.apache.uima.ducc.common.NodeIdentity;
import org.apache.uima.ducc.common.config.CommonConfiguration;
import org.apache.uima.ducc.common.config.DuccBlastGuardPredicate;
import org.apache.uima.ducc.common.utils.DuccLogger;
import org.apache.uima.ducc.common.utils.Utils;
import org.apache.uima.ducc.transport.DuccExchange;
import org.apache.uima.ducc.transport.DuccTransportConfiguration;
import org.apache.uima.ducc.transport.agent.NodeMetricsConfiguration;
import org.apache.uima.ducc.transport.dispatcher.DuccEventDispatcher;
import org.apache.uima.ducc.transport.event.common.IDuccProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.thoughtworks.xstream.XStream;
//import org.apache.uima.ducc.agent.event.AgentPingEvent;

@Configuration
@Import({ DuccTransportConfiguration.class, CommonConfiguration.class,
    NodeMetricsConfiguration.class })
public class AgentConfiguration {
  DuccLogger logger = new DuccLogger(this.getClass(), "Agent");

  // fetch the name of an endpoint where the JM expects incoming requests
  // @Value("#{ systemProperties['IP'] }")
  public String ip = System.getenv("IP");

  public String nodeName = System.getenv("NodeName");

  private CamelContext camelContext;

  private RouteBuilder metricsRouteBuilder;
  
  private RouteBuilder inventoryRouteBuilder;

  // public static String agentPingEnpoint = "activemq:topic:agent.ping.endpoint";
  // public static String agentPingSelectorName="agent_ip";
  // public static String agentPingSelector;

  // private DuccEventDispatcher agentPingDispatcher;

  @Value("#{ systemProperties['ducc.agent.launcher.thread.pool.size'] }")
  String launcherThreadPoolSize;

  @Value("#{ systemProperties['ducc.agent.launcher.process.stop.timeout'] }")
  public String processStopTimeout;

  @Value("#{ systemProperties['ducc.agent.node.inventory.publish.rate.skip'] }")
  public String inventoryPublishRateSkipCount;

  // Get comma separated list of processes to ignore while detecting rogue processes
  @Value("#{ systemProperties['ducc.agent.rogue.process.exclusion.filter'] }")
  public String processExclusionList;

  // Get comma separated list of users to ignore while detecting rogue processes
  @Value("#{ systemProperties['ducc.agent.rogue.process.user.exclusion.filter'] }")
  public String userExclusionList;

  @Autowired
  DuccTransportConfiguration agentTransport;

  @Autowired
  NodeMetricsConfiguration nodeMetrics;

  @Autowired
  CommonConfiguration common;

  /**
   * Creates {@code AgentEventListener} that will handle incoming messages.
   * 
   * @param agent
   *          - {@code NodeAgent} instance to initialize the listener
   * 
   * @return {@code AgentEventListener} instance
   */
  public AgentEventListener agentDelegateListener(NodeAgent agent) {
    return new AgentEventListener(agent);
  }

  /**
   * Creates Camel Router to generate Node Metrics at regular intervals.
   * 
   * @param targetEndpointToReceiveNodeMetricsUpdate
   *          - where to post NodeMetrics
   * @param nodeMetricsPublishRate
   *          - how to publish NodeMetrics
   * @return - {@code RouteBuilder} instance
   * 
   * @throws Exception
   */
  private RouteBuilder routeBuilderForNodeMetricsPost(final NodeAgent agent,
          final String targetEndpointToReceiveNodeMetricsUpdate, final int nodeMetricsPublishRate)
          throws Exception {
    final Processor nmp = nodeMetricsProcessor(agent);
    final Predicate blastFilter = new DuccBlastGuardPredicate(agent.getLogger());
    final Processor cp = new ConfirmProcessor();
    return new RouteBuilder() {
      public void configure() {
        onException(Exception.class).handled(true).process(new ErrorProcessor());
        from("timer:nodeMetricsTimer?fixedRate=true&period=" + nodeMetricsPublishRate)
                .routeId("NodeMetricsPostRoute")

                // This route uses a filter to prevent sudden bursts of messages which
                // may flood DUCC daemons causing chaos. The filter disposes any message
                // that appears in a window of 1 sec or less.
                .filter(blastFilter).process(nmp).to(targetEndpointToReceiveNodeMetricsUpdate)
                .process(cp);
      }
    };
  }

  /**
   * Creates Camel Router to generate Node Metrics at regular intervals.
   * 
   * @param targetEndpointToReceiveNodeMetricsUpdate
   *          - where to post NodeMetrics
   * @param nodeMetricsPublishRate
   *          - how to publish NodeMetrics
   * @return - {@code RouteBuilder} instance
   * 
   * @throws Exception
   */
  private RouteBuilder routeBuilderForNodeInventoryPost(final NodeAgent agent,
          final String targetEndpointToReceiveNodeInventoryUpdate,
          final int nodeInventoryPublishRate) throws Exception {
    final Processor nmp = nodeInventoryProcessor(agent);
    return new RouteBuilder() {
      public void configure() {
        final Predicate bodyNotNull = body().isNotNull();

        final Predicate blastGuard = new DuccBlastGuardPredicate(agent.getLogger());
        onException(Exception.class).maximumRedeliveries(0).handled(true)
                .process(new ErrorProcessor());
        
        from("timer:nodeInventoryTimer?fixedRate=true&period=" + nodeInventoryPublishRate)
                .routeId("NodeInventoryPostRoute")
                // This route uses a filter to prevent sudden bursts of messages which
                // may flood DUCC daemons causing chaos. The filter disposes any message
                // that appears in a window of 1 sec or less.
                .filter(blastGuard)
                // add inventory to the body of the message
                .process(nmp)
                // filter out messages with no body. Since this route is on a timer
                // it keeps generating flow of messages. However, the agent only
                // publishes inventory if there is a change or configured number of
                // epochs has passed. Otherwise, the agent puts null in the body of
                // the message and this route should just throw it away.
                .filter(bodyNotNull).to(targetEndpointToReceiveNodeInventoryUpdate);
      }
    };
  }

  /**
   * Creates Camel Router to handle incoming messages
   * 
   * @param delegate
   *          - {@code AgentEventListener} to delegate messages to
   * 
   * @return {@code RouteBuilder} instance
   */
  public synchronized RouteBuilder routeBuilderForIncomingRequests(final NodeAgent agent,
          final AgentEventListener delegate) {
    return new RouteBuilder() {
      public void configure() {
        onException(Throwable.class).maximumRedeliveries(0)
                .handled(false)
                .process(new ErrorProcessor());
        from(common.agentRequestEndpoint).routeId("IncomingRequestsRoute")
        // .process(new DebugProcessor())
                .bean(delegate);
      }
    };
  }

  /**
   * Creates Camel Router to handle incoming messages
   * 
   * @param delegate
   *          - {@code AgentEventListener} to delegate messages to
   * 
   * @return {@code RouteBuilder} instance
   */
  public synchronized RouteBuilder routeBuilderForManagedProcessStateUpdate(final NodeAgent agent,
          final AgentEventListener delegate) {
    return new RouteBuilder() {

      // Custom filter to select messages that are targeted for this agent
      // Checks the node list in a message to determine if this agent is
      // the target.
      Predicate filter = new DuccNodeFilter(agent);

      public void configure() {
        onException(Throwable.class).maximumRedeliveries(0).handled(true)
                .process(new ErrorProcessor()).stop();

        from(common.managedProcessStateUpdateEndpoint).routeId("ManageProcessStateUpdateRoute")
        // .process(new StateUpdateDebugProcessor(logger))
                .choice().when(filter).bean(delegate).end();
      }
    };
  }

  // private RouteBuilder routeBuilderForNodePing(final NodeAgent agent, final String
  // targetEndpoint) throws Exception {
  // return new RouteBuilder() {
  // PingProcessor pingProcessor = new PingProcessor(agent);
  // public void configure() {
  // System.out.println("Agent Listening on Ping Endpoint:"+targetEndpoint);
  // onException(Exception.class).handled(true).process(new ErrorProcessor());
  // from(targetEndpoint)
  // .routeId("NodePingRoute")
  // .process( pingProcessor);
  // }
  // };
  // }

  public class DebugProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
      String methodName = "process";
      // if ( logger.isLevelEnabled(Level.TRACE) ) {
      XStreamDataFormat xStreamDataFormat = new XStreamDataFormat();
      XStream xStream = xStreamDataFormat.getXStream(new DefaultClassResolver());
      String marshalledEvent = xStream.toXML(exchange.getIn().getBody());
      logger.info(methodName, null, marshalledEvent);
      // }
    }

  }

  public static class ConfirmProcessor implements Processor {
    boolean first = true;

    public void process(Exchange exchange) throws Exception {
      // if ( first ) {
      // synchronized(this) {
      // this.wait(20000);
      // }
      // first = false;
      // }

      // XStreamDataFormat xStreamDataFormat = new XStreamDataFormat();
      // XStream xStream = xStreamDataFormat.getXStream(new DefaultClassResolver());
      // String marshalledEvent = xStream.toXML(exchange.getIn().getBody());
      //
      // System.out.println("Agent Published Metrics:\n"+
      // marshalledEvent);

    }
  }

  public static class StateUpdateDebugProcessor implements Processor {
    DuccLogger logger;

    StateUpdateDebugProcessor(DuccLogger logger) {
      this.logger = logger;
    }

    public void process(Exchange exchange) throws Exception {
      Map<String, Object> map = exchange.getIn().getHeaders();
      StringBuffer sb = new StringBuffer();
      for (Entry<String, Object> entry : map.entrySet()) {
        sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
      }
      logger.info("StateUpdateDebugProcessor.process", null, "Headers:\n\t" + sb.toString());

    }
  }

  public class ErrorProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
      // the caused by exception is stored in a property on the exchange
      Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
      logger.error("ErrorProcessor.process", null, caused);
      // assertNotNull(caused);
      // here you can do what you want, but Camel regard this exception as handled, and
      // this processor as a failurehandler, so it wont do redeliveries. So this is the
      // end of this route. But if we want to route it somewhere we can just get a
      // producer template and send it.

      // send it to our mock endpoint
      // exchange.getContext().createProducerTemplate().send("mock:myerror", exchange);
    }
  }

  public static class TransportProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
      try {
        System.out.println(">>> Agent Received Message of type:"
                + exchange.getIn().getBody().getClass().getName());
      } catch (Exception e) {
        e.printStackTrace();
      }
      // Destination replyTo = exchange.getIn().getHeader("JMSReplyTo",
      // Destination.class);
      // System.out.println("... transport - value of replyTo:" +
      // replyTo);
    }

  }

  // public static class PingProcessor implements Processor {
  // private NodeAgent agent;
  //
  // PingProcessor(NodeAgent agent ) {
  // this.agent = agent;
  // }
  // public void process(Exchange exchange) throws Exception {
  // try {
  // agent.ping((AgentPingEvent)exchange.getIn().getBody());
  // } catch( Exception e ) {
  // e.printStackTrace();
  // }
  // }
  //
  // }
  private NodeIdentity nodeIdentity() throws Exception {
    NodeIdentity ni = null;

    if (ip != null) {
      // Inject IP to enable deployment of multiple Agents on the same node with
      // different identity
      ni = new NodeIdentity(ip, nodeName); // this should only be used for simulation
    } else {
      ni = new NodeIdentity();
    }
    return ni;
  }

  private Launcher launcher() {
    return new Launcher();
  }

  public DuccEventDispatcher getCommonProcessDispatcher(CamelContext camelContext) throws Exception {
    return agentTransport.duccEventDispatcher(logger, common.managedServiceEndpoint, camelContext);
  }

  public DuccEventDispatcher getORDispatcher(CamelContext camelContext) throws Exception {
	    return agentTransport.duccEventDispatcher(logger, common.nodeInventoryEndpoint, camelContext);
  }

  public int getNodeInventoryPublishDelay() {
	  return Integer.parseInt(common.nodeInventoryPublishRate);
  }
  @Bean
  public NodeAgent nodeAgent() throws Exception {
    try {
      camelContext = common.camelContext();
      camelContext.disableJMX();

      NodeAgent agent = new NodeAgent(nodeIdentity(), launcher(), camelContext, this);
      // optionally configures Camel Context for JMS. Checks the 'agentRequestEndpoint' to
      // to determine type of transport. If the the endpoint starts with "activemq:", a
      // special ActiveMQ component will be activated to enable JMS transport
      agentTransport.configureJMSTransport(logger,common.agentRequestEndpoint, camelContext);
      AgentEventListener delegateListener = agentDelegateListener(agent);

      if (common.managedProcessStateUpdateEndpointType != null
              && common.managedProcessStateUpdateEndpointType.equalsIgnoreCase("socket")) {
        String agentSocketParams = "";
        if (common.managedProcessStateUpdateEndpointParams != null) {
          agentSocketParams = "?" + common.managedProcessStateUpdateEndpointParams;
        }
        int agentPort = Utils.findFreePort();
        common.managedProcessStateUpdateEndpoint = "mina:tcp://localhost:" + agentPort
                + agentSocketParams;
        // Remember the agent port since we need to tell JPs where to send their state updates
        System.setProperty(NodeAgent.ProcessStateUpdatePort, String.valueOf(agentPort));
      }
      camelContext
              .addRoutes(this.routeBuilderForManagedProcessStateUpdate(agent, delegateListener));
      camelContext.addRoutes(this.routeBuilderForIncomingRequests(agent, delegateListener));
      
      inventoryRouteBuilder = 
    		  (this.routeBuilderForNodeInventoryPost(agent,
    	              common.nodeInventoryEndpoint, Integer.parseInt(common.nodeInventoryPublishRate)));
      
      camelContext.addRoutes(inventoryRouteBuilder);
      metricsRouteBuilder = this.routeBuilderForNodeMetricsPost(agent, common.nodeMetricsEndpoint,
              Integer.parseInt(common.nodeMetricsPublishRate));
      camelContext.addRoutes(metricsRouteBuilder);
      
      
      
      logger.info("nodeAgent", null, "------- Agent Initialized - Identity Name:"
              + agent.getIdentity().getName() + " IP:" + agent.getIdentity().getIp()
              + " JP State Update Endpoint:" + common.managedProcessStateUpdateEndpoint);
      return agent;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Bean
  public NodeMetricsProcessor nodeMetricsProcessor(NodeAgent agent) throws Exception {
    if (Utils.isLinux()) {
      return new LinuxNodeMetricsProcessor(agent, "/proc/meminfo", "/proc/loadavg");
    } else {
      return new DefaultNodeMetricsProcessor(agent);
    }
  }

  public ProcessMetricsProcessor processMetricsProcessor(NodeAgent agent, IDuccProcess process,
          ManagedProcess managedProcess) throws Exception {
    if (Utils.isLinux()) {
      return new LinuxProcessMetricsProcessor(logger, process, agent, "/proc/" + process.getPID()
              + "/statm", "/proc/stat", "/proc/" + process.getPID() + "/stat", managedProcess);
    } else {
      return new DefaultProcessMetricsProcessor(process, agent);
    }

  }

  public NodeInventoryProcessor nodeInventoryProcessor(NodeAgent agent) {
    return new DefaultNodeInventoryProcessor(agent, inventoryPublishRateSkipCount);
  }

  public void stopInventoryRoute() {
	    stopRoute(inventoryRouteBuilder.getRouteCollection(),">>>> Agent Stopped Publishing Inventory");
  }
  
  public void stopMetricsRoute() {
    stopRoute(metricsRouteBuilder.getRouteCollection(),">>>> Agent Stopped Publishing Metrics");
//    try {
//      RoutesDefinition rsd = metricsRouteBuilder.getRouteCollection();
//      for (RouteDefinition rd : rsd.getRoutes()) {
//        camelContext.stopRoute(rd.getId());
//        camelContext.removeRoute(rd.getId());
//        logger.error(methodName, null, ">>>> Agent Stopped Metrics Publishing");
//      }
//
//    } catch (Exception e) {
//      logger.error(methodName, null, e);
//    }
  }

  public void stopRoute(RoutesDefinition rsd, String logMsg) {
	    String methodName = "stopRoute";
	    try {
	      for (RouteDefinition rd : rsd.getRoutes()) {
	        camelContext.stopRoute(rd.getId());
	        camelContext.removeRoute(rd.getId());
	        logger.info(methodName, null, logMsg); 
	      }

	    } catch (Exception e) {
	      logger.error(methodName, null, e);
	    }
	  }

  private class DuccNodeFilter implements Predicate {
    private NodeAgent agent = null;

    public DuccNodeFilter(NodeAgent agent) {
      this.agent = agent;
    }

    public synchronized boolean matches(Exchange exchange) {
      String methodName = "DuccNodeFilter.matches";
      boolean result = false;
      if (common.managedProcessStateUpdateEndpoint.startsWith("mina")) {
        // mina is a socket component with point-to-point semantics thus
        // the client always sends a message to the correct agent. No reason
        // to determine if this is a target agent.
        result = true;
      } else {
        try {
          String nodes = (String) exchange.getIn().getHeader(DuccExchange.TARGET_NODES_HEADER_NAME);
          logger.trace(methodName, null, ">>>>>>>>> Agent: [" + agent.getIdentity().getIp()
                  + "] Received a Message. Is Agent target for message:" + result
                  + ". Target Agents:" + nodes);
          result = Utils.isTargetNodeForMessage(nodes, agent.getIdentity().getNodeIdentities());
        } catch (Throwable e) {
          e.printStackTrace();
          logger.error(methodName, null, e, new Object[] {});
        }
      }
      return result;
    }
  }
  // public DuccEventDispatcher getAgentPingDispatcher() {
  // return agentPingDispatcher;
  // }
}

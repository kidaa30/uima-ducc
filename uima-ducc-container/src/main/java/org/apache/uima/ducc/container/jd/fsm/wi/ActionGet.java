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
package org.apache.uima.ducc.container.jd.fsm.wi;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.ducc.common.jd.files.workitem.IWorkItemStateKeeper;
import org.apache.uima.ducc.container.common.MessageBuffer;
import org.apache.uima.ducc.container.common.MetaCasHelper;
import org.apache.uima.ducc.container.common.Standardize;
import org.apache.uima.ducc.container.common.fsm.iface.IAction;
import org.apache.uima.ducc.container.common.fsm.iface.IEvent;
import org.apache.uima.ducc.container.common.fsm.iface.IFsm;
import org.apache.uima.ducc.container.common.logger.IComponent;
import org.apache.uima.ducc.container.common.logger.ILogger;
import org.apache.uima.ducc.container.common.logger.Logger;
import org.apache.uima.ducc.container.jd.JobDriver;
import org.apache.uima.ducc.container.jd.JobDriverHelper;
import org.apache.uima.ducc.container.jd.blacklist.JobProcessBlacklist;
import org.apache.uima.ducc.container.jd.cas.CasManager;
import org.apache.uima.ducc.container.jd.log.LoggerHelper;
import org.apache.uima.ducc.container.jd.mh.RemoteWorkerProcess;
import org.apache.uima.ducc.container.jd.mh.iface.IOperatingInfo.CompletionType;
import org.apache.uima.ducc.container.jd.mh.iface.remote.IRemoteWorkerProcess;
import org.apache.uima.ducc.container.jd.mh.iface.remote.IRemoteWorkerThread;
import org.apache.uima.ducc.container.jd.wi.IProcessStatistics;
import org.apache.uima.ducc.container.jd.wi.IWorkItem;
import org.apache.uima.ducc.container.jd.wi.WiTracker;
import org.apache.uima.ducc.container.net.iface.IMetaCas;
import org.apache.uima.ducc.container.net.iface.IMetaCasTransaction;
import org.apache.uima.ducc.container.net.iface.IMetaCasTransaction.Hint;
import org.apache.uima.ducc.container.net.iface.IMetaCasTransaction.JdState;
import org.apache.uima.ducc.container.net.impl.TransactionHelper;

public class ActionGet implements IAction {

	private static Logger logger = Logger.getLogger(ActionGet.class, IComponent.Id.JD.name());

	private ConcurrentHashMap<IRemoteWorkerProcess, Long> warnedJobDiscontinued = new ConcurrentHashMap<IRemoteWorkerProcess, Long>();
	private ConcurrentHashMap<IRemoteWorkerProcess, Long> warnedProcessDiscontinued = new ConcurrentHashMap<IRemoteWorkerProcess, Long>();
	private ConcurrentHashMap<IRemoteWorkerProcess, Long> warnedExhausted = new ConcurrentHashMap<IRemoteWorkerProcess, Long>();
	
	@Override
	public String getName() {
		return ActionGet.class.getName();
	}
	
	@Override
	public void engage(Object objectData) {
		String location = "engage";
		logger.trace(location, ILogger.null_id, "enter");
		IActionData actionData = (IActionData) objectData;
		try {
			if(actionData != null) {
				IRemoteWorkerThread rwt = actionData.getRemoteWorkerThread();
				WiTracker tracker = WiTracker.getInstance();
				IWorkItem wi = tracker.find(rwt);
				IFsm fsm = wi.getFsm();
				IMetaCasTransaction trans = actionData.getMetaCasTransaction();
				IRemoteWorkerProcess rwp = new RemoteWorkerProcess(trans);
				//
				JobDriver jd = JobDriver.getInstance();
				JobDriverHelper jdh = JobDriverHelper.getInstance();
				jd.advanceJdState(JdState.Active);
				CasManager cm = jd.getCasManager();
				IMetaCas metaCas = null;
				JobProcessBlacklist jobProcessBlacklist = JobProcessBlacklist.getInstance();
				if(cm.getCasManagerStats().isExhausted()) {
					if(!warnedExhausted.containsKey(rwp)) {
						MessageBuffer mbx = LoggerHelper.getMessageBuffer(actionData);
						mbx.append(Standardize.Label.node.get()+rwp.getNodeName());
						mbx.append(Standardize.Label.pid.get()+rwp.getPid());
						mbx.append(Standardize.Label.text.get()+"all CASes processed");
						logger.debug(location, ILogger.null_id, mbx.toString());
						warnedExhausted.put(rwp, new Long(System.currentTimeMillis()));
					}
					TransactionHelper.addResponseHint(trans, Hint.Exhausted);
				}
				else if(cm.getCasManagerStats().isKillJob()) {
					if(!warnedJobDiscontinued.containsKey(rwp)) {
						MessageBuffer mb = LoggerHelper.getMessageBuffer(actionData);
						mb.append(Standardize.Label.node.get()+rwp.getNodeName());
						mb.append(Standardize.Label.pid.get()+rwp.getPid());
						mb.append(Standardize.Label.text.get()+"job discontinued");
						logger.warn(location, ILogger.null_id, mb.toString());
						warnedJobDiscontinued.put(rwp, new Long(System.currentTimeMillis()));
					}
					TransactionHelper.addResponseHint(trans, Hint.Killed);
				}
				else if(jobProcessBlacklist.includes(rwp)) {
					if(!warnedProcessDiscontinued.containsKey(rwp)) {
						MessageBuffer mb = LoggerHelper.getMessageBuffer(actionData);
						mb.append(Standardize.Label.node.get()+rwp.getNodeName());
						mb.append(Standardize.Label.pid.get()+rwp.getPid());
						mb.append(Standardize.Label.text.get()+"process discontinued");
						logger.warn(location, ILogger.null_id, mb.toString());
						warnedProcessDiscontinued.put(rwp, new Long(System.currentTimeMillis()));
					}
					TransactionHelper.addResponseHint(trans, Hint.Blacklisted);
				}
				else {
					metaCas = cm.getMetaCas();
				}
				wi.setMetaCas(metaCas);
				trans.setMetaCas(metaCas);
				IWorkItemStateKeeper wisk = jd.getWorkItemStateKeeper();
				MetaCasHelper metaCasHelper = new MetaCasHelper(metaCas);
				IProcessStatistics pStats = jdh.getProcessStatistics(rwp);
				//
				IEvent event = null;
				//
				if(metaCas != null) {
					int seqNo = metaCasHelper.getSystemKey();
					String wiId = metaCas.getUserKey();
					String node = rwt.getNodeAddress();
					String pid = ""+rwt.getPid();
					String tid = ""+rwt.getTid();
					wisk.start(seqNo, wiId, node, pid, tid);
					wisk.queued(seqNo);
					pStats.dispatch(wi);
					//
					wi.setTodGet();
					event = WiFsm.CAS_Available;
					MessageBuffer mb = LoggerHelper.getMessageBuffer(actionData);
					JobDriver.getInstance().getMessageHandler().incGets();
					logger.info(location, ILogger.null_id, mb.toString());
				}
				else {
					event = WiFsm.CAS_Unavailable;
					MessageBuffer mb = LoggerHelper.getMessageBuffer(actionData);
					mb.append("No CAS found for processing");
					logger.debug(location, ILogger.null_id, mb.toString());
					if(cm.getCasManagerStats().isExhausted()) {
						if(!warnedExhausted.containsKey(rwp)) {
							MessageBuffer mbx = LoggerHelper.getMessageBuffer(actionData);
							mbx.append(Standardize.Label.node.get()+rwp.getNodeName());
							mbx.append(Standardize.Label.pid.get()+rwp.getPid());
							mbx.append(Standardize.Label.text.get()+"all CASes processed");
							logger.debug(location, ILogger.null_id, mbx.toString());
							warnedExhausted.put(rwp, new Long(System.currentTimeMillis()));
						}
						TransactionHelper.addResponseHint(trans, Hint.Exhausted);
					}
				}
				//
				fsm.transition(event, actionData);
			}
			else {
				MessageBuffer mb = LoggerHelper.getMessageBuffer(actionData);
				mb.append("No action data found for processing");
				logger.warn(location, ILogger.null_id, mb.toString());
			}
		}
		catch(Exception e) {
			logger.error(location, ILogger.null_id, e);
			JobDriver.getInstance().killJob(CompletionType.Exception);
		}
		logger.trace(location, ILogger.null_id, "exit");
	}

}

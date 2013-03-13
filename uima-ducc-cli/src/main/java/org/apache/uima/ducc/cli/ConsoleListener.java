
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
package org.apache.uima.ducc.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.ducc.common.NodeIdentity;
import org.apache.uima.ducc.common.Pair;

class ConsoleListener
    implements Runnable
{
    private ServerSocket sock;
    private CliBase submit;
    private Map<Integer, Pair<StdioReader, StdioWriter>> listeners = new HashMap<Integer, Pair<StdioReader, StdioWriter>>();

    private int          console_listener_port;
    private String       console_host_address;

    private boolean      in_shutdown = false;
    private boolean      start_stdin = false;

    private IConsoleCallback consoleCb;
    // private int          callers;   // number of remote processes we expect to listen for

    boolean debug = false;
    ConsoleListener(CliBase submit, IConsoleCallback consoleCb)
        throws Exception
    {
        this.submit = submit;
        this.sock = new ServerSocket(0);
        this.console_listener_port  = sock.getLocalPort();
        this.consoleCb = consoleCb;
        
        NodeIdentity ni = new NodeIdentity();
        this.console_host_address = ni.getIp();            

        debug = submit.isDebug();
        // this.callers = 1;         // assume we'll get at least one listener, else we would not have been called.
    }

    String getConsoleHostAddress()
    {
        return console_host_address;
    }

    int getConsolePort()
    {
        return console_listener_port;
    }

//     /**
//      * The caller knows there may be more than one remote process calling us but
//      * we've no clue when or if they will show up.  We assume here they do, and 
//      * rely on some external influence to correct us if not.
//      */
//     synchronized void incrementCallers()
//     {
//         callers++;
//     }

//     synchronized void waitForCompletion()
//     {
//         try {
//             while ( (callers > 0) && ( !in_shutdown) ) {
//                 wait();
//             }
// 		} catch (InterruptedException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
//     }

//     private synchronized void releaseWait()
//     {
//         callers--;
//         notify();
//     }

    synchronized boolean isShutdown()
    {
        return in_shutdown;
    }

    void shutdown()
    {
        if ( debug ) System.out.println("Console handler: Shutdown starts");
        in_shutdown = true;
        try {
            sock.close();
            for ( Pair<StdioReader, StdioWriter> handler: listeners.values() ) {
                handler.first().close();
                handler.second().close();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void delete(int port)
    {
        int count;
        synchronized(this) {
            listeners.remove(port);
            count = listeners.size();
        }

        if ( debug ) System.out.println("Console handler: Removed handler for port " + port + ", size = "  + listeners.size());
        if ( count == 0 ) {
            shutdown();
        }
    }

    void startStdin(boolean start_stdin)
    {
        this.start_stdin = start_stdin;
    }

    public void run()
    {
        if ( debug ) System.out.println("Listening on " + console_host_address + " " + console_listener_port);

        while ( true ) {
            try {                    
                Socket s = sock.accept();
                StdioReader sr = new StdioReader(s, this);
                StdioWriter sw = new StdioWriter(s, this);
                int p = s.getPort();
                synchronized(this) {
                    listeners.put(p, new Pair<StdioReader, StdioWriter>(sr, sw));
                }

                Thread t = new Thread(sr, "STDOUT");
                t.start();                

                if ( start_stdin ) {
                    // generally started only for AP (ducclet)
                    Thread tt = new Thread(sw, "STDIN");
                    tt.start();             
                }   
            } catch (Throwable t) {
                if ( ! in_shutdown ) shutdown();
                if ( debug ) System.out.println("console listener returns");
                submit.consoleExits();
                return;
            } 
        }
    }

    class StdioReader
        implements Runnable
    {
        Socket sock;
        InputStream is;
        boolean done = false;
        ConsoleListener cl;
        String remote_host;
        String logfile = "N/A";

        static final String console_tag = "1002 CONSOLE_REDIRECT ";
        int tag_len = 0;


        StdioReader(Socket sock, ConsoleListener cl)
        {
            this.sock = sock;
            this.cl = cl;

            InetAddress ia = sock.getInetAddress();
            remote_host = ia.getHostName();
            tag_len = console_tag.length();

            if ( debug ) System.out.println("===== Listener starting: " + remote_host + ":" + sock.getPort());
        }

        public void close()
            throws Throwable
        {
            if ( debug ) System.out.println("===== Listener completing: " + remote_host + ":" + sock.getPort());
            this.done = true;
            is.close();
            cl.delete(sock.getPort());
        }

        void doWrite(String line)
        {
            if ( line.startsWith(console_tag) ) {
                logfile = line.substring(tag_len);
                return;                                                                      // don't echo this
            }
            if ( logfile.equals("N/A") ) return;                                             // don't log until we get a log name
            if ( line.startsWith("1001 Command launching...") && ( !debug) ) return;        // don't log duccling noise
            consoleCb.stdout(remote_host, logfile, line);
        }
        
        /**
         * We received a buffer of bytes that needs to be put into a string and printed.  We want
         * to split along \n boundaries so we can insert the host name at the start of every line.
         *
         * Simple, except that the end of the buffer may not be \n, instead it could be the
         * start of another line.
         *
         * We want to save the partial lines as the start of the next line so they can all be
         * printed all nicely.
         */
        String partial = null;
        public void printlines(byte[] buf, int count)
        {
            String tmp = new String(buf, 0, count);
            String[] lines = tmp.split("\n");
            int len = lines.length - 1;
            if ( len < 0 ) {
                // this is a lone linend.  Spew the partial if it exists and just return.
                if ( partial != null ) {
                    doWrite(partial);
                    partial = null;
                }
                return;
            }


            if ( partial != null ) {
                // some leftover, it's the start of the first line of the new buffer.
                lines[0] = partial + lines[0];
                partial = null;
            }

            for ( int i = 0; i < len; i++ ) {
                // spew everything but the last line
                doWrite(lines[i]);
            }

            if ( tmp.endsWith("\n") ) {
                // if the last line ends with linend, there is no partial, just spew
                doWrite(lines[len]);
                partial = null;
            } else {
                // otherwise, wait for the next buffer
                partial = lines[len];
            }
        }

        public void run()
        {            
            byte[] buf = new byte[4096];
            try {
                is = sock.getInputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            
            try {
                int count = 0;
                while ( (count = is.read(buf)) > 0 ) {
                    printlines(buf, count);
                }
                if ( debug ) System.out.println(remote_host + ": EOF:  exiting");
            } catch ( Throwable t ) {
                t.printStackTrace();
            }
            try {
                close();
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class StdioWriter
        implements Runnable
    {
        Socket sock;
        OutputStream out;

        boolean done = false;
        ConsoleListener cl;

        boolean shutdown = false;

        StdioWriter(Socket sock, ConsoleListener cl)
        {
            this.sock = sock;
            this.cl = cl;
        }

        void close()
        {
            shutdown = true;
            try {
                if ( out != null ) {
                    out.close();
                }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        public void run()
        {
        	if ( debug ) System.out.println("STDIN LISTENER STARTS *******");
            try {
                out = sock.getOutputStream();
            } catch (Exception e) {
                System.out.println("Cannot acquire remote socket for stdin redirection: " + e.toString());
                return;
            }


            byte[] buf = new byte[4096];
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            int ch;
            int ndx = 0;
            int max = buf.length - 1;
            try {
				while ( ((ch = in.read()) != -1) && (!shutdown) ) {
				    buf[ndx++] = (byte) ch;
				    if ( (ch == '\n') || (ndx > max)) {
				        out.write(buf, 0, ndx);
				        ndx = 0;
				    }
				}
			} catch (IOException e) {
                System.out.println("Error in process stdin redirection - redirection ended. " + e.toString());
			}
            if ( debug ) System.out.println("***********STDIN returns");
        }
    }
}


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
package org.apache.uima.ducc.jd;

import org.apache.uima.ducc.jd.client.IWorkItemMonitor;
import org.apache.uima.ducc.jd.client.ThreadLocation;
import org.apache.uima.ducc.transport.event.common.IDuccWorkJob;
import org.apache.uima.ducc.transport.event.jd.v1.DriverStatusReportV1;


public interface IJobDriver extends IJobDriverAccess, IWorkItemMonitor {
	
	public void initialize(IDuccWorkJob job, DriverStatusReportV1 driverStatusReport) throws JobDriverTerminateException;
	public void run();
	
	public IDuccWorkJob getJob();
	public void setJob(IDuccWorkJob job);
	
	public boolean callbackRegister(String casId, String name);
	public void callbackUnregister(String casId);
	
	public boolean isLostCas(String casId);
	public ThreadLocation getLostCas(String casId);
}

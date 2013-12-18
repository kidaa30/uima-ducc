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
package org.apache.uima.ducc.transport.event.common;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.uima.ducc.common.utils.id.DuccId;
import org.apache.uima.ducc.transport.event.common.IDuccUnits.MemoryUnits;
import org.apache.uima.ducc.transport.event.jd.PerformanceMetricsSummaryMap;

/**
 * Data utilized by the work scheduler.
 */
public class DuccSchedulingInfo implements IDuccSchedulingInfo {
	
	/**
	 * please increment this sUID when removing or modifying a field 
	 */
	private static final long serialVersionUID = 1L;
	private String schedulingClass = defaultSchedulingClass;
	private String schedulingPriority = defaultSchedulingPriority;
	private String shareMemorySize = defaultShareMemorySize;
	private MemoryUnits shareMemoryUnits = defaultShareMemoryUnits;
	private String instancesCount = defaultInstancesCount;
	
	@Deprecated
	private String machinesCount = defaultMachinesCount;
	
	private String sharesMax = defaultSharesMax;
	private String sharesMin = defaultSharesMin;
	private String threadsPerShare = defaultThreadsPerShare;
	
	private String workItemsTotal = defaultWorkItemsTotal;
	private String workItemsCompleted = defaultWorkItemsCompleted;
	private String workItemsDispatched = defaultWorkItemsDispatched;
	private String workItemsError = defaultWorkItemsError;
	private String workItemsRetry = defaultWorkItemsRetry;
	private String workItemsLost = defaultWorkItemsLost;
	private String workItemsPreempt= defaultWorkItemsPreempt;
	
	private ConcurrentHashMap<Integer,DuccId> limboMap = new  ConcurrentHashMap<Integer,DuccId>();
	private ConcurrentHashMap<String,DuccId> casQueuedMap = new  ConcurrentHashMap<String,DuccId>();
	
	private IDuccPerWorkItemStatistics perWorkItemStatistics = null;
	private PerformanceMetricsSummaryMap performanceMetricsSummaryMap = null;
	
	private long mostRecentWorkItemStart = 0;
	
	@Deprecated
	private String workItemsPending = defaultWorkItemsPending;
	
	
	public String getSchedulingClass() {
		return schedulingClass;
	}

	
	public void setSchedulingClass(String schedulingClass) {
		if(schedulingClass != null) {
			this.schedulingClass = schedulingClass;
		}
	}

	
	public String getSchedulingPriority() {
		return schedulingPriority;
	}

	
	public void setSchedulingPriority(String schedulingPriority) {
		if(schedulingPriority != null) {
			this.schedulingPriority = schedulingPriority;
		}
	}


	
	public String getInstancesCount() {
		return instancesCount;
	}

	
	public void setInstancesCount(String instancesCount) {
		if(instancesCount != null) {
			this.instancesCount = instancesCount;
		}
	}
	
	@Deprecated
	
	public String getMachinesCount() {
		return machinesCount;
	}

	@Deprecated
	
	public void setMachinesCount(String machinesCount) {
		if(machinesCount != null) {
			this.machinesCount = machinesCount;
		}
	}
	
	
	public String getShareMemorySize() {
		return shareMemorySize;
	}

	
	public void setShareMemorySize(String size) {
		if(size != null) {
			this.shareMemorySize = size;
		}
	}

	
	public MemoryUnits getShareMemoryUnits() {
		return shareMemoryUnits;
	}

	
	public void setShareMemoryUnits(MemoryUnits units) {
		if(units != null) {
			this.shareMemoryUnits = units;
		}
	}
	
	
	public long getLongSharesMax() {
		long retVal = -1;
		try {
			retVal = Long.parseLong(sharesMax);
		}
		catch(Throwable t) {
		}
		return retVal;
	}
	
	
	public void setLongSharesMax(long shares) {
		this.sharesMax = ""+shares;
	}
	
	
	public String getSharesMax() {
		return sharesMax;
	}

	
	public void setSharesMax(String shares) {
		if(shares != null) {
			this.sharesMax = shares.trim();
		}
	}

	
	public String getSharesMin() {
		return this.sharesMin;
	}

	
	public void setSharesMin(String shares) {
		if(shares != null) {
			this.sharesMin = shares;
		}
	}
	
	
	public String getThreadsPerShare() {
		return threadsPerShare;
	}
	
	
	public int getIntThreadsPerShare() {
		return Integer.parseInt(threadsPerShare);
	}

	
	public void setThreadsPerShare(String number) {
		if(number != null) {
			this.threadsPerShare = number;
		}
	}
	
	
	public String getWorkItemsTotal() {
		return workItemsTotal;
	}

	
	public void setWorkItemsTotal(String number) {
		if(number != null) {
			this.workItemsTotal = number;
		}
	}
	
	
	public int getIntWorkItemsTotal() {
		return Integer.parseInt(workItemsTotal);
	}
	
	
	public String getWorkItemsCompleted() {
		return workItemsCompleted;
	}

	
	public void setWorkItemsCompleted(String number) {
		if(number != null) {
			this.workItemsCompleted = number;
		}
	}
	
	
	public int getIntWorkItemsCompleted() {
		return Integer.parseInt(workItemsCompleted);
	}
	
	
	public String getWorkItemsDispatched() {
		return workItemsDispatched;
	}

	
	public void setWorkItemsDispatched(String number) {
		if(number != null) {
			this.workItemsDispatched = number;
		}
	}

	
	public ConcurrentHashMap<Integer,DuccId> getLimboMap() {
		if(limboMap == null) {
			return new ConcurrentHashMap<Integer,DuccId>();
		}
		else {
			return limboMap;
		}
		
	}

	
	public void setLimboMap(ConcurrentHashMap<Integer,DuccId> map) {
		if(map != null) {
			this.limboMap = map;
		}
	}

	
	public ConcurrentHashMap<String,DuccId> getCasQueuedMap() {
		if(casQueuedMap == null) {
			return new ConcurrentHashMap<String,DuccId>();
		}
		else {
			return casQueuedMap;
		}
		
	}

	
	public void setCasQueuedMap(ConcurrentHashMap<String,DuccId> map) {
		if(map != null) {
			this.casQueuedMap = map;
		}
	}
	
	
	public String getWorkItemsError() {
		return workItemsError;
	}

	
	public void setWorkItemsError(String number) {
		if(number != null) {
			this.workItemsError = number;
		}
	}
	
	
	public int getIntWorkItemsError() {
		return Integer.parseInt(workItemsError);
	}
	
	
	public String getWorkItemsRetry() {
		return workItemsRetry;
	}

	
	public void setWorkItemsRetry(String number) {
		if(number != null) {
			this.workItemsRetry = number;
		}
	}
	
	
	public String getWorkItemsLost() {
		return workItemsLost;
	}

	
	public void setWorkItemsLost(String number) {
		if(number != null) {
			this.workItemsLost = number;
		}
	}
	
	
	public int getIntWorkItemsLost() {
		return Integer.parseInt(workItemsLost);
	}
	
	
	public String getWorkItemsPreempt() {
		if(workItemsPreempt == null) {
			workItemsPreempt = "0";
		}
		return workItemsPreempt;
	}

	
	public void setWorkItemsPreempt(String number) {
		if(number != null) {
			this.workItemsPreempt = number;
		}
	}
	
	
	public IDuccPerWorkItemStatistics getPerWorkItemStatistics() {
		return perWorkItemStatistics;
	}
	
	
	public void setPerWorkItemStatistics(IDuccPerWorkItemStatistics value) {
		perWorkItemStatistics = value;
	}
	
	
	public PerformanceMetricsSummaryMap getPerformanceMetricsSummaryMap() {
		return performanceMetricsSummaryMap;
	}
	
	
	public void setMostRecentWorkItemStart(long time) {
		mostRecentWorkItemStart = time;
	}
	
	
	public long getMostRecentWorkItemStart() {
		return mostRecentWorkItemStart;
	}
	
	
	@Deprecated
	
	public String getWorkItemsPending() {
		return workItemsPending;
	}
	
	@Deprecated
	
	public void setWorkItemsPending(String number) {
		if(number != null) {
			this.workItemsPending = number;
		}
	}

	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instancesCount == null) ? 0 : instancesCount.hashCode());
		result = prime * result
				+ ((machinesCount == null) ? 0 : machinesCount.hashCode());
		result = prime * result
				+ ((schedulingClass == null) ? 0 : schedulingClass.hashCode());
		result = prime
				* result
				+ ((schedulingPriority == null) ? 0 : schedulingPriority
						.hashCode());
		result = prime * result
				+ ((shareMemorySize == null) ? 0 : shareMemorySize.hashCode());
		result = prime
				* result
				+ ((shareMemoryUnits == null) ? 0 : shareMemoryUnits.hashCode());
		result = prime * result
				+ ((sharesMax == null) ? 0 : sharesMax.hashCode());
		result = prime * result
				+ ((sharesMin == null) ? 0 : sharesMin.hashCode());
		result = prime * result
				+ ((threadsPerShare == null) ? 0 : threadsPerShare.hashCode());
		result = prime
				* result
				+ ((workItemsCompleted == null) ? 0 : workItemsCompleted
						.hashCode());
		result = prime
				* result
				+ ((workItemsDispatched == null) ? 0 : workItemsDispatched
						.hashCode());
		result = prime * result
				+ ((workItemsError == null) ? 0 : workItemsError.hashCode());
		result = prime
				* result
				+ ((workItemsPending == null) ? 0 : workItemsPending.hashCode());
		result = prime * result
				+ ((workItemsRetry == null) ? 0 : workItemsRetry.hashCode());
		result = prime * result
				+ ((workItemsTotal == null) ? 0 : workItemsTotal.hashCode());
		return result;
	}

	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DuccSchedulingInfo other = (DuccSchedulingInfo) obj;
		if (instancesCount == null) {
			if (other.instancesCount != null)
				return false;
		} else if (!instancesCount.equals(other.instancesCount))
			return false;
		if (machinesCount == null) {
			if (other.machinesCount != null)
				return false;
		} else if (!machinesCount.equals(other.machinesCount))
			return false;
		if (schedulingClass == null) {
			if (other.schedulingClass != null)
				return false;
		} else if (!schedulingClass.equals(other.schedulingClass))
			return false;
		if (schedulingPriority == null) {
			if (other.schedulingPriority != null)
				return false;
		} else if (!schedulingPriority.equals(other.schedulingPriority))
			return false;
		if (shareMemorySize == null) {
			if (other.shareMemorySize != null)
				return false;
		} else if (!shareMemorySize.equals(other.shareMemorySize))
			return false;
		if (shareMemoryUnits != other.shareMemoryUnits)
			return false;
		if (sharesMax == null) {
			if (other.sharesMax != null)
				return false;
		} else if (!sharesMax.equals(other.sharesMax))
			return false;
		if (sharesMin == null) {
			if (other.sharesMin != null)
				return false;
		} else if (!sharesMin.equals(other.sharesMin))
			return false;
		if (threadsPerShare == null) {
			if (other.threadsPerShare != null)
				return false;
		} else if (!threadsPerShare.equals(other.threadsPerShare))
			return false;
		if (workItemsCompleted == null) {
			if (other.workItemsCompleted != null)
				return false;
		} else if (!workItemsCompleted.equals(other.workItemsCompleted))
			return false;
		if (workItemsDispatched == null) {
			if (other.workItemsDispatched != null)
				return false;
		} else if (!workItemsDispatched.equals(other.workItemsDispatched))
			return false;
		if (workItemsError == null) {
			if (other.workItemsError != null)
				return false;
		} else if (!workItemsError.equals(other.workItemsError))
			return false;
		if (workItemsPending == null) {
			if (other.workItemsPending != null)
				return false;
		} else if (!workItemsPending.equals(other.workItemsPending))
			return false;
		if (workItemsRetry == null) {
			if (other.workItemsRetry != null)
				return false;
		} else if (!workItemsRetry.equals(other.workItemsRetry))
			return false;
		if (workItemsTotal == null) {
			if (other.workItemsTotal != null)
				return false;
		} else if (!workItemsTotal.equals(other.workItemsTotal))
			return false;
		if (mostRecentWorkItemStart != other.mostRecentWorkItemStart)
			return false;
		return true;
	}
	
	// **********
	
//	
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((getSchedulingClass() == null) ? 0 : getSchedulingClass().hashCode());
//		result = prime * result + ((getSchedulingPriority() == null) ? 0 : getSchedulingPriority().hashCode());
//		result = prime * result + ((getSharesMax() == null) ? 0 : getSharesMax().hashCode());
//		result = prime * result + ((getSharesMin() == null) ? 0 : getSharesMin().hashCode());
//		result = prime * result + ((getShareMemorySize() == null) ? 0 : getShareMemorySize().hashCode());
//		result = prime * result + ((getShareMemoryUnits() == null) ? 0 : getShareMemoryUnits().hashCode());
//		result = prime * result + ((getThreadsPerShare() == null) ? 0 : getThreadsPerShare().hashCode());
//		result = prime * result + super.hashCode();
//		return result;
//	}
//	
//	public boolean equals(Object obj) {
//		boolean retVal = false;
//		if(this == obj) {
//			retVal = true;
//		}
//		else if(getClass() == obj.getClass()) {
//			DuccSchedulingInfo that = (DuccSchedulingInfo)obj;
//			if( 	Util.compare(this.getSchedulingClass(),that.getSchedulingClass()) 
//				&&	Util.compare(this.getSchedulingPriority(),that.getSchedulingPriority()) 
//				&&	Util.compare(this.getSharesMax(),that.getSharesMax()) 
//				&&	Util.compare(this.getSharesMin(),that.getSharesMin()) 
//			//	These don't change:
//			//	&&	Util.compare(this.getShareMemorySize(),that.getShareMemorySize()) 
//			//	&&	Util.compare(this.getShareMemoryUnits(),that.getShareMemoryUnits()) 
//			//	&&	Util.compare(this.getInstancesCount(),that.getInstancesCount()) 
//			//	&&	Util.compare(this.getThreadsPerShare(),that.getThreadsPerShare()) 
////				&&	super.equals(obj)
//				) 
//			{
//				retVal = true;
//			}
//		}
//		return retVal;
//	}
	
	
	
}

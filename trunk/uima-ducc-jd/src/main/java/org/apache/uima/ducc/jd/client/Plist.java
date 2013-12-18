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
package org.apache.uima.ducc.jd.client;

import java.util.TreeMap;

public class Plist {

	private String plist;
	
	private final String delimParams = ",";
	private final String delimValues = "[:=]";
	
	private TreeMap<String,String> parameterMap = new TreeMap<String,String>();
	
	public Plist(String plist) {
		this.plist = plist;
		if(plist != null) {
			init();
		}
	}
	
	private void init() {
		String[] pArray = plist.split(delimParams);
		for(int i=0; i<pArray.length; i++) {
			String[] parameter = pArray[i].split(delimValues,2);
			if(parameter.length == 2) {
				String key = parameter[0];
				String value = parameter[1];
				if((key != null) && (key.length() > 0) && (value != null) && (value.length() > 0)) {
					parameterMap.put(key,value);
				}
			}
		}
	}
	
	public TreeMap<String,String> getParameterMap() {
		return parameterMap;
	}
	
}

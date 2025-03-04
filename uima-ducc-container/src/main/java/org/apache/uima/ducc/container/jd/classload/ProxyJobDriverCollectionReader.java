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
package org.apache.uima.ducc.container.jd.classload;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.uima.ducc.container.common.FlagsExtendedHelper;
import org.apache.uima.ducc.container.common.MessageBuffer;
import org.apache.uima.ducc.container.common.Standardize;
import org.apache.uima.ducc.container.common.classloader.PrivateClassLoader;
import org.apache.uima.ducc.container.common.classloader.ProxyException;
import org.apache.uima.ducc.container.common.classloader.ProxyLogger;
import org.apache.uima.ducc.container.common.logger.IComponent;
import org.apache.uima.ducc.container.common.logger.ILogger;
import org.apache.uima.ducc.container.common.logger.Logger;
import org.apache.uima.ducc.container.jd.mh.MessageHandler;
import org.apache.uima.ducc.container.net.impl.MetaCas;

public class ProxyJobDriverCollectionReader {

	private static Logger logger = Logger.getLogger(ProxyJobDriverCollectionReader.class, IComponent.Id.JD.name());
	
	private URLClassLoader urlClassLoader = null;
	
	private String crXml = null;
	private String crCfg = null;
	
	private Class<?>[] nullClassArray = null;
	private Object[] nullObjectArray = null;
	
	private Class<?> class_JdUserCollectionReader = null;
	private Object instance_JdUserCollectionReader = null;
	
	private String name_getTotal = "getTotal";
	private Method method_getTotal = null;
	
	private Class<?> class_JdUserMetaCas = null;
	
	private String name_getJdUserMetaCas = "getJdUserMetaCas";
	private Method method_getJdUserMetaCas = null;
	
	private String name_getSeqNo = "getSeqNo";
	private String name_getDocumentText = "getDocumentText";
	private String name_getSerializedCas = "getSerializedCas";

	private String[] requiredClasses = { 
			"org.apache.uima.ducc.user.jd.JdUserCollectionReader", 
			"org.apache.uima.cas.CAS",
			};
	
	public ProxyJobDriverCollectionReader() throws ProxyException {
		try {
			initialize();
		}
		catch(Exception e) {
			ProxyLogger.loggifyUserException(e);
			throw new ProxyException();
		}
	}
	
	private void initialize() throws Exception {
		FlagsExtendedHelper feh = FlagsExtendedHelper.getInstance();
		String userClasspath = feh.getUserClasspath();
		URLClassLoader classLoader = createClassLoader(userClasspath);
		String crXml = feh.getCollectionReaderXml();
		String crCfg = feh.getCollectionReaderCfg();
		construct(classLoader, crXml, crCfg);
	}
	
	private URLClassLoader createClassLoader(String userClasspath) throws Exception {
		URLClassLoader retVal = null;
		retVal = PrivateClassLoader.create(userClasspath);
		return retVal;
	}
	
	public int getTotal() throws ProxyException {
		int retVal = -1;
		try {
			retVal = (Integer)method_getTotal.invoke(instance_JdUserCollectionReader, nullObjectArray);
		} 
		catch(Exception e) {
			ProxyLogger.loggifyUserException(e);
			throw new ProxyException();
		}
		return retVal;
	}
	
	public MetaCas getMetaCas() throws ProxyException {
		MetaCas retVal = null;
		try {
			method_getJdUserMetaCas = class_JdUserCollectionReader.getMethod(name_getJdUserMetaCas, nullClassArray);
			long stime = System.nanoTime();
			Object instance_metaCas = method_getJdUserMetaCas.invoke(instance_JdUserCollectionReader, nullObjectArray);
			MessageHandler.accumulateTimes("CR", stime);   // When debugging accumulate elapsed time spent in CR
			if(instance_metaCas != null) {
				Method method_getSeqNo = class_JdUserMetaCas.getMethod(name_getSeqNo, nullClassArray);
				Integer integer = (Integer)method_getSeqNo.invoke(instance_metaCas, nullObjectArray);
				int seqNo = integer.intValue();
				Method method_getSerializedCas = class_JdUserMetaCas.getMethod(name_getSerializedCas, nullClassArray);
				Object serializedCas = method_getSerializedCas.invoke(instance_metaCas, nullObjectArray);
				Method method_getDocumentText = class_JdUserMetaCas.getMethod(name_getDocumentText, nullClassArray);
				String docId = (String)method_getDocumentText.invoke(instance_metaCas, nullObjectArray);
				retVal = new MetaCas(seqNo, docId, serializedCas);
			}
		} 
		catch(Exception e) {
			ProxyLogger.loggifyUserException(e);
			throw new ProxyException();
		}
		return retVal;
	}
	
	private void construct(URLClassLoader classLoader, String crXml, String cfCfg) throws Exception {
		setup(classLoader, crXml, cfCfg);
		validate();
		prepare();
	}
	
	private void setup(URLClassLoader urlClassLoader, String crXml, String crCfg) throws Exception {
		String location = "setup";
		if(urlClassLoader == null) {
			Exception e = new Exception("missing URLClassLoader");
			logger.error(location, ILogger.null_id, e);
			throw e;
		}
		setURLClassLoader(urlClassLoader);
		if(crXml == null) {
			Exception e = new Exception("missing CollectionReader xml");
			logger.error(location, ILogger.null_id, e);
			throw e;
		}
		setCrXml(crXml);
		setCrCfg(crCfg);
	}
	
	private void validate() throws Exception {
		for(String className : requiredClasses) {
			loadClass(className);
		}
	}
	
	private void prepare() throws Exception {
		class_JdUserCollectionReader = urlClassLoader.loadClass("org.apache.uima.ducc.user.jd.JdUserCollectionReader");
		Constructor<?> constructor_JdUserCollectionReader = class_JdUserCollectionReader.getConstructor(String.class,String.class);
		instance_JdUserCollectionReader = constructor_JdUserCollectionReader.newInstance(new Object[] { crXml, crCfg });
		method_getTotal = class_JdUserCollectionReader.getMethod(name_getTotal, nullClassArray);
		class_JdUserMetaCas = urlClassLoader.loadClass("org.apache.uima.ducc.user.jd.JdUserMetaCas");
		method_getJdUserMetaCas = class_JdUserCollectionReader.getMethod(name_getJdUserMetaCas, nullClassArray);
	}
	
	private void setURLClassLoader(URLClassLoader value) {
		String location = "setURLClassLoader";
		logger.debug(location, ILogger.null_id, value);
		urlClassLoader = value;
	}
	
	private void setCrXml(String value) {
		crXml = value;
	}
	
	private void setCrCfg(String value) {
		crCfg = value;
	}
	
	private void loadClass(String className) throws Exception {
		String location = "loadClass";
		MessageBuffer mb = new MessageBuffer();
		mb.append(Standardize.Label.loading.get()+className);
		logger.debug(location, ILogger.null_id, mb.toString());
		URL[] urls = urlClassLoader.getURLs();
		for(URL url : urls) {
			logger.trace(location, ILogger.null_id, url);
		}
		Class<?> loadedClass = urlClassLoader.loadClass(className);
		mb= new MessageBuffer();
		mb.append(Standardize.Label.loaded.get()+loadedClass.getName());
		logger.trace(location, ILogger.null_id, mb.toString());
	}
}

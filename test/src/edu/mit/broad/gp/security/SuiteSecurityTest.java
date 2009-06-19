package edu.mit.broad.gp.security;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;
import org.junit.*;
import static org.junit.Assert.*;

public class SuiteSecurityTest {	
	AdminProxy adminProxy1;
	AdminProxy adminProxy2;
	TaskIntegratorProxy taskIntegratorProxy1;
	TaskIntegratorProxy taskIntegratorProxy2;
	
	String suiteName = "SuiteSecurityTestSuite";
	
	String username1 = "foo";
	String password="";
	String username2 = "bar";
	String url="http://127.0.0.1:8080/";
	
	String privateLsid = null;
	String publicLsid = null;
	
	public static String logFile = "c:/SuiteSecurityTest.log";

	{
		setupLog4jConfig();
	}
	
	@Before public void setUp() throws WebServiceException {
		try {		
		adminProxy1 = new AdminProxy( url,  username1,  password);
		adminProxy2 = new AdminProxy( url,  username2,  password);
	    taskIntegratorProxy1 = new TaskIntegratorProxy(url, username1, password);
	    taskIntegratorProxy2 = new TaskIntegratorProxy(url, username2, password);
		    
	    SuiteInfo suiteInfo;
	    ArrayList<String> modules = new ArrayList<String>();
	    modules.add("urn:lsid:broadinstitute.org:cancer.software.genepattern.module.analysis:00002:1");
	    suiteInfo = new SuiteInfo(null, suiteName+"private", "autogenerated",
	            username1, username1,  modules,  GPConstants.ACCESS_PRIVATE, new ArrayList());
	    
	    privateLsid = taskIntegratorProxy1.installSuite(suiteInfo);
	   
	    suiteInfo = new SuiteInfo(null, suiteName+"public", "autogenerated",
	            username1, username1,  modules,  GPConstants.ACCESS_PUBLIC, new ArrayList());
	   
	    publicLsid = taskIntegratorProxy1.installSuite(suiteInfo);
		   
	  
		} catch (WebServiceException wse){
			wse.printStackTrace();
			throw wse;
		}
	}
	
	@Test public void getSomeoneElsesPrivateSuite() throws WebServiceException {
			// the privatelsid belongs to username1/adminProxy1
			SuiteInfo si = null;
			try {
				 si = adminProxy2.getSuite(privateLsid);
			} catch (WebServiceException wse){
				// should get an exception if we don't own it
				assert(true);
			}
			assertTrue( si == null);
		
	}
	@Test public void getSomeoneElsesPublicSuite() throws WebServiceException {
		// the publiclsid belongs to username1/adminProxy1 and we should be able to
		// see it
	
		SuiteInfo si = adminProxy2.getSuite(publicLsid);
		assertTrue( si != null);
	
}
	@Test public void deleteSomeoneElsesPrivateSuite() throws WebServiceException {
		// the privatelsid belongs to username1/adminProxy1
		// delete it (try) as person 2
		try {
		taskIntegratorProxy2.deleteSuite(privateLsid);
		} catch (WebServiceException wse){
			// should get one if we don't own it
		}
		// check that it is still there or person 1
		SuiteInfo si = null;
		
		try {
			si = adminProxy1.getSuite(privateLsid);
		} catch (WebServiceException wse){
			//no rethrow, we expect this if it is working right
		}
		assertTrue( si != null);
	
	}
	@Test public void deleteSomeoneElsesPublicSuite() throws WebServiceException {
		// the publicLsid belongs to username1/adminProxy1
		// delete it (try) as person 2
		try {
			taskIntegratorProxy2.deleteSuite(publicLsid);
		} catch (WebServiceException wse){
			// should get one if we don't own it
		}
		
		// check that it is still there or person 1
		SuiteInfo si = null;
		
		try {
			si = adminProxy1.getSuite(publicLsid);
		} catch (WebServiceException wse){
			assert(false); // shouldn't have been deleted so shouldn't get here
		}
		assertTrue( si != null);
	
	}
	@Test public void getMyPrivateSuite() throws WebServiceException {
		
		SuiteInfo si = adminProxy1.getSuite(privateLsid);
		assertTrue( si != null);
		
	}
	@Test public void getMyPublicSuite() throws WebServiceException {
		
		SuiteInfo si = adminProxy1.getSuite(publicLsid);
		assertTrue( si != null);
		
	}
	@After public void cleanUp(){
		try {
			taskIntegratorProxy1.deleteSuite(privateLsid);
		} catch (WebServiceException wse){
		}
		try {
			taskIntegratorProxy1.deleteSuite(publicLsid);
		} catch (WebServiceException wse){
		}
	}
	
	 public static void setupLog4jConfig(){
	    	
	    	Properties log4jconfig = new Properties();
	    	log4jconfig.setProperty("log4j.rootLogger", "error, R");
	    	log4jconfig.setProperty("log4j.appender.R", "org.apache.log4j.RollingFileAppender");
	    	log4jconfig.setProperty("log4j.appender.R.File", logFile);
	    	log4jconfig.setProperty("log4j.appender.R.MaxFileSize", "10KB");
	    	log4jconfig.setProperty("log4j.appender.R.MaxBackupIndex", "2");
	    	log4jconfig.setProperty("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
	    	log4jconfig.setProperty("log4j.appender.R.layout.ConversionPattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] (%F:%L) - %m%n");
	    	   	
	    	PropertyConfigurator.configure(log4jconfig);
	    }
}

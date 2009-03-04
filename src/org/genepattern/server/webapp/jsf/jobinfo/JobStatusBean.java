/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/
package org.genepattern.server.webapp.jsf.jobinfo;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoManager.MyJobInfo;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

/**
 * @author pcarr
 */
public class JobStatusBean {
    private static Logger log = Logger.getLogger(JobStatusBean.class);
    
    private int jobNumber = -1;
    private MyJobInfo myJobInfo = null;

    public JobStatusBean() {
        String jobNumberParameter = null;

        try {
            jobNumberParameter = UIBeanHelper.getRequest().getParameter("jobNumber");
            jobNumberParameter = UIBeanHelper.decode(jobNumberParameter);
            jobNumber = Integer.parseInt(jobNumberParameter);
        }
        catch (NumberFormatException e1) {
            log.error("Invalid value for request parameter, 'jobNumber':  "+jobNumberParameter, e1);
            return;
        }
        
        String userId = UIBeanHelper.getUserId();

        HttpServletRequest request = UIBeanHelper.getRequest();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");
        
        JobInfoManager jobInfoManager = new JobInfoManager();
        this.myJobInfo = jobInfoManager.getJobInfo(cookie, contextPath, userId, jobNumber);
    }
    
    public MyJobInfo getJobInfo() {
        return myJobInfo;
    }

}

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

/**
 * Backing bean for the drop-down menu on the Job Results page.
 * Save values to database so they can be loaded in the next session.
 * 
 * @author pcarr
 */
public class JobResultsFilterBean {
    private String userId = null;
    private boolean showEveryonesJobs = false;
    private String selectedGroup = null;
    private Set<String> selectedGroups = new HashSet<String>();

    //cached values
    private int jobCount = -1;

    public JobResultsFilterBean() {
        //requires a valid user id
        setUserId(UIBeanHelper.getUserId());
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
        this.jobCount = -1;
        
        this.showEveryonesJobs = 
            Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showEveryonesJobs", String.valueOf(showEveryonesJobs)));
        this.selectedGroups.clear();
        this.selectedGroup = new UserDAO().getPropertyValue(userId, "jobResultsFilter", null);
        if (selectedGroup != null) {
            this.selectedGroups.add(selectedGroup);
            this.showEveryonesJobs = false;
        }
    }
    
    public Object getJobFilter() {
        if (selectedGroup != null) {
            return selectedGroup;
        }
        if (showEveryonesJobs) {
            return "#ALL_JOBS";
        }
        else {
            return "#MY_JOBS";
        }
    }

    public void setJobFilter(Object obj) {
        this.jobCount = -1;
        this.selectedGroup = null;
        this.selectedGroups.clear();
        this.showEveryonesJobs = false;
        
        String menuVal = null;
        if (obj instanceof String) {
            menuVal = (String) obj;
        }

        if (menuVal != null && menuVal.equals("#MY_JOBS")) {
            //this.showEveryonesJobs = false;
        }
        else if (menuVal != null && menuVal.equals("#ALL_JOBS")) {
            this.showEveryonesJobs = true;
        }
        else if (menuVal != null) {
            //this.showEveryonesJobs = false;
            selectedGroup = menuVal;
            selectedGroups.add(menuVal);
        }
        
        UserDAO userDao = new UserDAO();
        userDao.setProperty(UIBeanHelper.getUserId(), "showEveryonesJobs", String.valueOf(showEveryonesJobs));
        userDao.setProperty(UIBeanHelper.getUserId(), "jobResultsFilter", selectedGroup);
    }
    
    public List<SelectItem> getJobFilterMenu() {
        List<SelectItem> rval = new ArrayList<SelectItem>();
        rval.add(new SelectItem("#MY_JOBS", "My job results"));
        rval.add(new SelectItem("#ALL_JOBS", "All job results"));
        
        //add groups to the list
        String userId = UIBeanHelper.getUserId();
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        Set<String> groups = groupMembership.getGroups(userId);
        for(String group : groups) {
            rval.add(new SelectItem(group, "In group: " + group));
        }
        return rval;
    }
    
    public int getJobCount() {
        if (jobCount < 0) {
            if (selectedGroup != null) {                
                //get the count of jobs in the selected group
                this.jobCount = new AnalysisDAO().getNumJobsInGroups(selectedGroups);
                return this.jobCount;
            }
            else if (!showEveryonesJobs) {
                this.jobCount = new AnalysisDAO().getNumJobsByUser(userId);
                return this.jobCount;
            }

            boolean isAdmin = AuthorizationHelper.adminServer(); 
            if (isAdmin) {
                this.jobCount = new AnalysisDAO().getNumJobsTotal();
                return this.jobCount;
            }
            IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
            Set<String> groups = groupMembership.getGroups(userId);
            this.jobCount = new AnalysisDAO().getNumJobsByUser(userId, groups);
            return this.jobCount;
        }
        return jobCount;
    }
    
    //legacy support (not to be called from JSF pages, but rather from JobBean.java)
    public boolean isShowEveryonesJobs() {
        return showEveryonesJobs;
    }
    
    public String getSelectedGroup() {
        return selectedGroup;
    }
    
    public Set<String> getSelectedGroups() {
        return selectedGroups;
    }
}

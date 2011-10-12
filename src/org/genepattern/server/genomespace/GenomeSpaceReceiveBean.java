package org.genepattern.server.genomespace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.uploads.UploadFilesBean;
import org.genepattern.server.webapp.uploads.UploadFilesBean.DirectoryInfoWrapper;
import org.genepattern.webservice.TaskInfo;

public class GenomeSpaceReceiveBean {
    private static Logger log = Logger.getLogger(GenomeSpaceReceiveBean.class);
    
    private UploadFilesBean uploadBean = null;
    private List<GSReceivedFileWrapper> receivedFiles = null;
    
    private void cleanBean() {
        receivedFiles = null;
    }
    
    private List<GSReceivedFileWrapper> parseParameters() {
        List<GSReceivedFileWrapper> received = new ArrayList<GSReceivedFileWrapper>();
        HttpServletRequest request = UIBeanHelper.getRequest();
        String filesString = request.getParameter("files");
        if (filesString != null) {
            String[] fileParams = filesString.split(",");
            for (String param : fileParams) {
                GpFilePath file = GenomeSpaceFileManager.createFile(param);
                GSReceivedFileWrapper wrapped = new GSReceivedFileWrapper(file);
                if (file != null) received.add(wrapped);
            }
        }
        return received;
    }
    
    private void populateSelectItems() {
        for (GSReceivedFileWrapper i : receivedFiles) {
            SortedSet<TaskInfo> infos = getKindToModules().get(i.getFile().getKind());
            List<SelectItem> items = new ArrayList<SelectItem>();
            for (TaskInfo j : infos) {
                SelectItem item = new SelectItem();
                item.setLabel(j.getName());
                item.setValue(j.getLsid());
                items.add(item);
            }
            i.setModuleSelects(items);
        }
    }
    
    public List<GSReceivedFileWrapper> getReceivedFiles() {
        if (receivedFiles == null) {
            receivedFiles = parseParameters();
            populateSelectItems();
        }
        return receivedFiles;
    }
    
    private boolean initUploadBean() {
        if (uploadBean == null) {
            uploadBean = (UploadFilesBean) UIBeanHelper.getManagedBean("#{uploadFilesBean}");
        }
        return uploadBean != null ? true : false;
    }
    
    public List<SelectItem> getUploadDirectories() {
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        boolean addedUploadRoot = false;
        if (!initUploadBean()) {
            log.error("Unable to acquire reference to UploadFilesBean in GenomeSpaceReceiveBean.getUploadDirectories()");
            return selectItems;
        }
        for (DirectoryInfoWrapper dir : uploadBean.getDirectories()) {
            SelectItem item = new SelectItem();
            if (dir.getPath().equals("./")) {
                if (!addedUploadRoot) item.setLabel("Upload Directory");
                else continue;
                addedUploadRoot = true;
            }
            else {
                item.setLabel(dir.getPath());
            }
            item.setValue(dir.getPath());
            selectItems.add(item);
        }
        return selectItems;
    }
    
    public String getRootUploadDirectory() {
        return "Upload Directory";
    }
    
    public Map<String, SortedSet<TaskInfo>> getKindToModules() {
        if (!initUploadBean()) {
            log.error("Unable to acquire reference to UploadFilesBean in GenomeSpaceReceiveBean.getKindToModules()");
            return null;
        }
        return uploadBean.getKindToTaskInfo();
    }

    public String prepareLoadTask() {
        JobBean jobBean = (JobBean) UIBeanHelper.getManagedBean("#{jobsBean}");
        cleanBean();
        return jobBean.loadTask();
    }
    
    public String prepareSaveFile() {
        HttpServletRequest request = UIBeanHelper.getRequest();
        String directoryPath = request.getParameter("uploadDirectory");
        String fileUrl = request.getParameter("path");
        if (directoryPath == null || fileUrl == null) {
            log.error("directoryPath was null in prepareSaveFile(): " + fileUrl);
            UIBeanHelper.setErrorMessage("Unable to get the selected directory to save file");
            return null;
        }
        
        GenomeSpaceBean genomeSpaceBean = (GenomeSpaceBean) UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
        genomeSpaceBean.saveFileToUploads(fileUrl, directoryPath);

        cleanBean();
        return "home";
    }
    
    public class GSReceivedFileWrapper {
        private GpFilePath file = null;
        private List<SelectItem> moduleSelects = null;
        
        public GSReceivedFileWrapper(GpFilePath file) {
            this.file = file;
        }
        
        public GpFilePath getFile() {
            return file;
        }
        
        public List<SelectItem> getModuleSelects() {
            return moduleSelects;
        }
        
        public void setModuleSelects(List<SelectItem> moduleSelects) {
            this.moduleSelects = moduleSelects;
        }
        
        
    }
}
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.genepattern.server.TaskUtil;
import org.genepattern.server.TaskUtil.ZipFileType;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.util.AuthorizationManager;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.WebServiceErrorMessageException;
import org.genepattern.webservice.WebServiceException;

public class ImportBean {
    private String url;

    private UploadedFile zipFile;

    private static Logger log = Logger.getLogger(ImportBean.class);

    private SelectItem[] filePrivacyItems;

    private SelectItem[] urlPrivacyItems;

    private String selectedFilePrivacy;

    private String selectedUrlPrivacy;

    private static final String ALL_USERS = "all users";

    private String installName;

    private String statusMessage = "";

    public ImportBean() {
        filePrivacyItems = new SelectItem[2];
        filePrivacyItems[0] = new SelectItem(UIBeanHelper.getUserId());
        filePrivacyItems[1] = new SelectItem(ALL_USERS);
        selectedFilePrivacy = ALL_USERS;

        urlPrivacyItems = new SelectItem[2];
        urlPrivacyItems[0] = new SelectItem(UIBeanHelper.getUserId());
        urlPrivacyItems[1] = new SelectItem(ALL_USERS);
        selectedUrlPrivacy = ALL_USERS;
    }

    public String importUrl() {
        if (url == null) {
            String message = "Please provide a URL to a GenePattern zip file.";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            throw new ValidatorException(facesMessage);
        }
        String path = null;
        try {
            path = GenePatternAnalysisTask.downloadTask(url);
            return doImport(path, selectedUrlPrivacy.equals(ALL_USERS) ? GPConstants.ACCESS_PUBLIC
                    : GPConstants.ACCESS_PRIVATE);
        } catch (IOException e) {
            UIBeanHelper.setErrorMessage("Unable to install " + url + ".");
            log.error(e);
            return "error";
        }

    }

    public String importFile() {
        if (zipFile == null) {
            String message = "Please provide a GenePattern zip file.";
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            throw new ValidatorException(facesMessage);
        }

        File tmpDir = null;
        File file = null;
        try {
            tmpDir = File.createTempFile("upload", "zip");
            tmpDir.delete();
            tmpDir.mkdir();
            file = saveFile(zipFile, tmpDir);
            return doImport(file.getCanonicalPath(), selectedFilePrivacy.equals(ALL_USERS) ? GPConstants.ACCESS_PUBLIC
                    : GPConstants.ACCESS_PRIVATE);
        } catch (IOException e) {
            UIBeanHelper.setErrorMessage("Unable to install " + zipFile.getName() + ". Please ensure that "
                    + zipFile.getName() + " is a valid module or pipeline zip file.");
            log.error("Error importing zip", e);
            return "error";
        }
    }

    private String doImport(final String path, final int privacy) {
        ZipFileType zipFileType = TaskUtil.getZipFileType(new File(path));
        if (zipFileType.equals(ZipFileType.INVALID_ZIP)) {
            UIBeanHelper.setInfoMessage(new File(path).getName() + " is not a valid GenePattern zip file.");
            return "error";
        } else if (zipFileType.equals(ZipFileType.MODULE_ZIP) || zipFileType.equals(ZipFileType.PIPELINE_ZIP)
                || zipFileType.equals(ZipFileType.PIPELINE_ZIP_OF_ZIPS)) {
            return doTaskImport(path, privacy, zipFileType);
        } else {
            return doSuiteImport(path, privacy);
        }
    }

    private String doTaskImport(final String path, final int privacy, ZipFileType zipFileType) {
        AuthorizationManager authManager = new AuthorizationManager();
        final String username = UIBeanHelper.getUserId();
        boolean createModuleAllowed = authManager.checkPermission("createModule", username);
        boolean createPipelineAllowed = authManager.checkPermission("createPipeline", username);
        final LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);

        if (zipFileType.equals(ZipFileType.PIPELINE_ZIP) || zipFileType.equals(ZipFileType.PIPELINE_ZIP_OF_ZIPS)) {
            if (!createPipelineAllowed) {
                UIBeanHelper.setInfoMessage("You do not have permission to install pipelines on this server.");
                return "error";
            }
        } else if (!createModuleAllowed) {
            UIBeanHelper.setInfoMessage("You do not have permission to install modules on this server.");
            return "error";
        }

        final boolean doRecursive = createModuleAllowed; // TODO ask user?

        final TaskInstallBean installBean = (TaskInstallBean) UIBeanHelper.getManagedBean("#{taskInstallBean}");
        final String lsid = "" + System.currentTimeMillis();
        String name = new File(path).getName();
        installBean.setTasks(new String[] { lsid }, new String[] { name });

        new Thread() {
            public void run() {
                try {
                    HibernateUtil.beginTransaction();

                    taskIntegratorClient.importZipFromURL(path, privacy, doRecursive, new Status() {

                        public void beginProgress(String string) {
                        }

                        public void continueProgress(int percent) {
                        }

                        public void endProgress() {
                        }

                        public void statusMessage(String message) {
                            installBean.setStatus(lsid, null, message);
                        }

                    });
                    HibernateUtil.commitTransaction();
                    installBean.setStatus(lsid, "success");
                } catch (WebServiceException e) {
                    HibernateUtil.rollbackTransaction();
                    if (e instanceof WebServiceErrorMessageException) {
                        WebServiceErrorMessageException wseme = (WebServiceErrorMessageException) e;

                        installBean.setStatus(lsid, "error", wseme.getMessage());

                    } else {
                        installBean.setStatus(lsid, "error");

                    }

                    log.error(e);
                }
                new File(path).delete();
            }

        }.start();

        return "install task results";
    }

    private String doSuiteImport(final String path, final int privacy) {
        final AuthorizationManager authManager = new AuthorizationManager();
        final String username = UIBeanHelper.getUserId();

        boolean suiteInstallAllowed = authManager.checkPermission("createSuite", username);
        if (!suiteInstallAllowed) {
            UIBeanHelper.setErrorMessage("You do not have permission to install suites on this server.");
            return "error";
        }
        final LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);

        final SuiteInstallBean installBean = (SuiteInstallBean) UIBeanHelper.getManagedBean("#{suiteInstallBean}");
        final String lsid = "" + System.currentTimeMillis();
        final String name = new File(path).getName();
        installBean.setSuites(new String[] { lsid }, new String[] { name });

        new Thread() {
            public void run() {
                try {
                    HibernateUtil.beginTransaction();
                    taskIntegratorClient.importZipFromURL(path, privacy, authManager.checkPermission("createModule",
                            username), new Status() {

                        public void beginProgress(String string) {
                        }

                        public void continueProgress(int percent) {
                        }

                        public void endProgress() {
                        }

                        public void statusMessage(String message) {
                            installBean.setStatus(lsid, null, message);
                        }
                    });
                    HibernateUtil.commitTransaction();
                    installBean.setStatus(lsid, "success");
                } catch (WebServiceException e) {
                    HibernateUtil.rollbackTransaction();
                    if (e instanceof WebServiceErrorMessageException) {
                        WebServiceErrorMessageException wseme = (WebServiceErrorMessageException) e;
                        installBean.setStatus(lsid, null, wseme.getMessage());

                    } else {
                        installBean.setStatus(lsid, "error");
                    }

                    log.error(e);
                }
                new File(path).delete();
            }

        }.start();
        return "install suite results";

    }

    public static File saveFile(UploadedFile uploadedFile, File destinationDirectory) throws IOException {
        FileOutputStream out = null;
        InputStream in = null;
        try {

            String fileName = uploadedFile.getName();
            if (fileName != null) {
                fileName = FilenameUtils.getName(fileName);

            }
            File file = new File(destinationDirectory, fileName);
            out = new FileOutputStream(file);
            in = uploadedFile.getInputStream();
            int bytesRead = -1;
            byte[] b = new byte[10000];
            while ((bytesRead = in.read(b, 0, b.length)) != -1) {
                out.write(b, 0, bytesRead);
            }
            return file;

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException x) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException x) {
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UploadedFile getZipFile() {
        return zipFile;
    }

    public void setZipFile(UploadedFile zipFile) {
        this.zipFile = zipFile;
    }

    public SelectItem[] getUrlPrivacyItems() {
        return urlPrivacyItems;
    }

    public void setUrlPrivacyItems(SelectItem[] urlPrivacyItems) {
        this.urlPrivacyItems = urlPrivacyItems;
    }

    public SelectItem[] getFilePrivacyItems() {
        return filePrivacyItems;
    }

    public void setFilePrivacyItems(SelectItem[] filePrivacyItems) {
        this.filePrivacyItems = filePrivacyItems;
    }

    public String getSelectedFilePrivacy() {
        return selectedFilePrivacy;
    }

    public void setSelectedFilePrivacy(String selectedFilePrivacy) {
        this.selectedFilePrivacy = selectedFilePrivacy;
    }

    public String getSelectedUrlPrivacy() {
        return selectedUrlPrivacy;
    }

    public void setSelectedUrlPrivacy(String selectedUrlPrivacy) {
        this.selectedUrlPrivacy = selectedUrlPrivacy;
    }

    public String getInstallName() {
        return installName;
    }

    public void setInstallName(String installName) {
        this.installName = installName;
    }

}

/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

public class JobResultsBean {
	private static Logger log = Logger.getLogger(JobResultsBean.class);

	/**
	 * Specifies job column to sort on. Possible values are jobNumber taskName
	 * dateSubmitted dateCompleted status
	 */
	private String jobSortColumn = "jobNumber";

	/**
	 * Job sort direction (true for ascending, false for descending)
	 */
	private boolean jobSortAscending = true;

	/**
	 * Specifies file column to sort on. Possible values are name size
	 * lastModified
	 */
	private String fileSortColumn = "name";

	/**
	 * File sort direction (true for ascending, false for descending)
	 */
	private boolean fileSortAscending = true;

	private List<JobResultInfo> jobResults;

	private boolean showEveryonesJobs = true;

	private boolean showExecutionLogs = true;

	List checkbox = new ArrayList();

	public JobResultsBean() {

	}

	/**
	 * Update the jobResults list
	 * 
	 */
	private void updateJobs() {
	}

	public String delete() {
		String[] selectedJobs = UIBeanHelper.getRequest().getParameterValues(
				"selectedJobs");
		deleteJobs(selectedJobs);

		String[] selectedFiles = UIBeanHelper.getRequest().getParameterValues(
				"selectedFiles");
		// TODO -- deleteFiles(selectedFiles);

		return null;

	}

	/**
	 * Delete a list of jobs.
	 * 
	 * @param jobNumbers
	 */
	private void deleteJobs(String[] jobNumbers) {
		LocalAnalysisClient analysisClient = new LocalAnalysisClient(
				UIBeanHelper.getUserId());
		List<Integer> jobErrors = new ArrayList<Integer>();

		if (jobNumbers != null) {
			for (String job : jobNumbers) {
				int jobNumber = Integer.parseInt(job);
				try {
					analysisClient.deleteJob(jobNumber);
					for (int i = 0; i < jobResults.size(); i++) {
						if (jobResults.get(i).getJobInfo().getJobNumber() == jobNumber) {
							jobResults.remove(i);
							break;
						}
					}
				} catch (NumberFormatException e) {
					log.error(e);
				} catch (WebServiceException e) {
					log.error(e);
					jobErrors.add(jobNumber);
				}
			}
		}

		// Create error messages
		StringBuffer sb = new StringBuffer();
		for (int i = 0, size = jobErrors.size(); i < size; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(jobErrors.get(i));
		}
		if (jobErrors.size() > 0) {
			String msg = "An error occurred while deleting job(s) "
					+ sb.toString() + ".";
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
		}

	}

	/**
	 * Action method. First sorts jobs, then for each job sorts files.
	 * 
	 * @return
	 */
	public void sort(ActionEvent event) {

		sortJobs();
		sortFiles();
	}

	private void sortJobs() {
		final String column = getJobSortColumn();
		Comparator comparator = new Comparator() {

			public int compare(Object o1, Object o2) {
				JobInfo c1 = ((JobResultInfo) o1).getJobInfo();
				JobInfo c2 = ((JobResultInfo) o2).getJobInfo();
				if (column == null) {
					return 0;
				} else if (column.equals("jobNumber")) {
					return jobSortAscending ? new Integer(c1.getJobNumber())
							.compareTo(c2.getJobNumber()) : new Integer(c2
							.getJobNumber()).compareTo(c1.getJobNumber());
				} else if (column.equals("taskName")) {
					return jobSortAscending ? c1.getTaskName().compareTo(
							c2.getTaskName()) : c2.getTaskName().compareTo(
							c1.getTaskName());
				} else if (column.equals("status")) {
					return jobSortAscending ? c1.getStatus().compareTo(
							c2.getStatus()) : c2.getStatus().compareTo(
							c1.getStatus());
				} else if (column.equals("dateCompleted")) {
					return jobSortAscending ? c1.getDateCompleted().compareTo(
							c2.getDateCompleted()) : c2.getDateCompleted()
							.compareTo(c1.getDateCompleted());
				} else if (column.equals("dateSubmitted")) {
					return jobSortAscending ? c1.getDateSubmitted().compareTo(
							c2.getDateSubmitted()) : c2.getDateSubmitted()
							.compareTo(c1.getDateSubmitted());
				} else {
					return 0;
				}
			}
		};
		Collections.sort(jobResults, comparator);
	}

	private void sortFiles() {
		final String column = getFileSortColumn();
		Comparator comparator = new Comparator() {

			public int compare(Object o1, Object o2) {
				FileInfo c1 = (FileInfo) o1;
				FileInfo c2 = (FileInfo) o2;
				if (column == null) {
					return 0;
				} else if (column.equals("name")) {
					return fileSortAscending ? c1.getName().compareTo(
							c2.getName()) : c2.getName()
							.compareTo(c1.getName());
				} else if (column.equals("size")) {
					return fileSortAscending ? new Long(c1.getSize())
							.compareTo(c2.getSize()) : new Long(c2.getSize())
							.compareTo(c1.getSize());
				} else if (column.equals("lastModified")) {
					return fileSortAscending ? c1.getLastModified().compareTo(
							c2.getLastModified()) : c2.getLastModified()
							.compareTo(c1.getLastModified());
				}

				else {
					return 0;
				}
			}
		};

		for (JobResultInfo jobResult : jobResults) {
			Collections.sort(jobResult.getOutputFiles(), comparator);
		}

	}

	public List<JobResultInfo> getJobResults() {
		if (jobResults == null) {
			String userId = UIBeanHelper.getUserId();
			LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
			try {
				JobInfo[] jobs = analysisClient.getJobs(
						showEveryonesJobs ? null : userId, -1,
						Integer.MAX_VALUE, false);
				jobResults = new ArrayList<JobResultInfo>(jobs.length);
				for (int i = 0; i < jobs.length; i++) {
					jobResults.add(new JobResultInfo(jobs[i]));
				}
			} catch (WebServiceException wse) {
				log.error(wse);
			}

		}
		return jobResults;
	}

	public Boolean isShowEveryonesJobs() {
		return showEveryonesJobs;
	}

	public void setShowEveryonesJobs(Boolean showAllJobs) {
		this.showEveryonesJobs = showAllJobs;
	}

	public Boolean isShowExecutionLogs() {
		return showExecutionLogs;
	}

	public void setShowExecutionLogs(Boolean showExecutionLogs) {
		this.showExecutionLogs = showExecutionLogs;
	}

	public List getCheckbox() {
		return checkbox;
	}

	public void setCheckbox(List checkboxList) {
		this.checkbox = checkboxList;
	}

	public String getJobSortColumn() {
		return jobSortColumn;
	}

	public void setJobSortColumn(String jobSortField) {
		this.jobSortColumn = jobSortField;
	}

	public String getFileSortColumn() {
		return fileSortColumn;
	}

	public void setFileSortColumn(String fileSortField) {
		this.fileSortColumn = fileSortField;
	}

	public boolean isFileSortAscending() {
		return fileSortAscending;
	}

	public void setFileSortAscending(boolean fileSortAscending) {
		this.fileSortAscending = fileSortAscending;
	}

	public boolean isJobSortAscending() {
		return jobSortAscending;
	}

	public void setJobSortAscending(boolean jobSortAscending) {
		this.jobSortAscending = jobSortAscending;
	}

	/**
	 * Represents a job result. Wraps JobInfo and adds methods for getting the
	 * output files and the expansion state of the associated UI panel
	 * 
	 */
	public static class JobResultInfo {
		JobInfo jobInfo;

		List<FileInfo> outputFiles;

		boolean expanded = true;

		public JobResultInfo(JobInfo jobInfo) {
			this.jobInfo = jobInfo;
			List<File> files = JobHelper.getOutputFiles(jobInfo);
			outputFiles = new ArrayList<FileInfo>(files.size());
			for (File file : files) {
				outputFiles.add(new FileInfo(file));
			}
		}

		public JobInfo getJobInfo() {
			return jobInfo;
		}

		public List<FileInfo> getOutputFiles() {
			return outputFiles;
		}

		public boolean isExpanded() {
			return expanded;
		}
	}

	/**
	 * Misc info on a job output file
	 */
	public static class FileInfo {
		String name;

		String absolutePath;

		long size;

		Date lastModified;

		boolean exists;

		public FileInfo(File file) {
			this.name = file.getName();
			this.size = file.length();
			this.exists = file.exists();
			this.absolutePath = file.getAbsolutePath();
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(file.lastModified());
			this.lastModified = cal.getTime();
		}

		public String getName() {
			return name;
		}

		public long getSize() {
			return size;
		}

		public String getFormattedSize() {
			NumberFormat nf = NumberFormat.getInstance();
			return nf.format(Math.max(1, size / 1000)) + "k";
		}

		public Date getLastModified() {
			return lastModified;
		}

		public String getAbsolutePath() {
			return absolutePath;
		}
	}

}

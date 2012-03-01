package com.anjlab.ping.filters;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.entities.JobResult;
import com.anjlab.ping.pages.job.EditJob;
import com.anjlab.ping.services.Application;
import com.anjlab.ping.services.JobResultCSVExporter;
import com.anjlab.ping.services.JobResultsAnalyzer;
import com.anjlab.ping.services.Mailer;
import com.anjlab.ping.services.Utils;
import com.google.appengine.api.datastore.Key;


public class BackupJobResultsFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(BackupJobResultsFilter.class);
    
    @Override
    protected void processRequest(EntityTransaction tx) 
            throws Exception
    {
        String encodedJobKey = globals.getHTTPServletRequest().getParameter(RunJobFilter.JOB_KEY_PARAMETER_NAME);

        if (Utils.isNullOrEmpty(encodedJobKey)) {
            return;
        }
        
        Key key = stringToKey(encodedJobKey);
        
        logger.debug("Running mail job: {}", key);

        Job job = jobDAO.find(key);
    
        if (job == null) {
            return;
        }

        List<JobResult> resultsBuffer = new ArrayList<JobResult>(Application.DEFAULT_NUMBER_OF_JOB_RESULTS);
        
        resultsBuffer.addAll(job.removeJobResultsExceptRecent(Application.DEFAULT_NUMBER_OF_JOB_RESULTS));

        job.setLastBackupTimestamp(new Date());
        
        if (resultsBuffer.size() > 0) {
            if (application.updateJob(job, false, true)) {

                if (job.isReceiveBackups() && !Utils.isNullOrEmpty(job.getReportEmail())) {
                    sendResultsByMail(job, resultsBuffer, job.getReportEmail());
                }

            } else {
                logger.error("Error saving job. Backup will not be sent to user this time.");
            }
        }
    }
    
    public void sendResultsByMail(Job job, List<JobResult> results, String reportRecipient) throws MessagingException, IOException, URISyntaxException {
        JobResult firstResult = (JobResult) results.get(0);
        JobResult lastResult = (JobResult) results.get(results.size() - 1);
        
        TimeZone timeZone = Application.UTC_TIME_ZONE; //  TODO Use job specific time zone
        
        JobResultsAnalyzer analyzer = new JobResultsAnalyzer(results, true);
        StringBuilder report = analyzer.buildHtmlReport(timeZone);
        
        String subject = "Data Backup for " + job.getTitleFriendly() + ": " + analyzer.getAvailabilitySummary();
        
        StringBuilder builder = new StringBuilder();
        builder.append("For period: ");
        builder.append(Application.formatDate(Application.DATETIME_FORMAT, timeZone, firstResult.getTimestamp()));
        builder.append(" (");
        builder.append(Utils.getTimeAgoUpToDays(firstResult.getTimestamp()));
        builder.append(") &ndash; ");
        builder.append(Application.formatDate(Application.DATETIME_FORMAT, timeZone, lastResult.getTimestamp()));
        builder.append(" (");
        builder.append(Utils.getTimeAgoUpToDays(lastResult.getTimestamp()));
        builder.append(") = ");
        builder.append(Utils.formatMillisecondsToWordsUpToMinutes(lastResult.getTimestamp().getTime() - firstResult.getTimestamp().getTime()));
        builder.append("<br/># of records: ");
        builder.append(results.size());
        builder.append("<br/>Time Zone: ");
        builder.append(timeZone.getDisplayName());
        builder.append(" (");
        builder.append(timeZone.getID());
        builder.append(")");
        
        builder.append(report);
        
        builder.append("<br/><br/>----"); 
        builder.append("<br/>You can disable receiving statistics backups for the job here: ");
        String editJobLink = application.getJobUrl(job, EditJob.class);
        builder.append(editJobLink);
        builder.append("<br/><br/>Note:");
        builder.append("<br/>Automatic Backups is a beta function, please use our <a href='http://ping-service.appspot.com/feedback'>feedback form</a> to provide a feedback on it.");
        builder.append("<br/>You will get approximately one email per week per job depending on job's cron string.");
        builder.append("<br/>Once you received an email with the statistics, this data will be deleted from Ping Service database.");
        builder.append("<br/>Ping Service will only store ");
        builder.append(Application.DEFAULT_NUMBER_OF_JOB_RESULTS);
        builder.append(" latest ping results per job.");
        builder.append("<br/>We're doing this to keep Ping Service free, since we're running out of free quota limit of Google App Engine infrastructure.");
        builder.append("<br/>We're sorry for any inconvenience you might get from this email.");
        builder.append("<br/>Thank you for understanding.");
        
        String message = builder.toString();

        byte[] export = JobResultCSVExporter.export(timeZone, results);

        MimeBodyPart attachment = new MimeBodyPart();
        
        attachment.setContent(new String(export), "text/csv");
        //  Set Content-Type explicitly since GAE ignores type passed to setContent(...) method
        attachment.setHeader("Content-Type", "text/csv");
        attachment.setFileName(
                "job-" 
                + job.getKey().getId() + "-results-" 
                + Application.formatDateForFileName(firstResult.getTimestamp(), timeZone) + "-" 
                + Application.formatDateForFileName(lastResult.getTimestamp(), timeZone) + ".csv");
        
        application.getMailer().sendMail2("text/html", Mailer.PING_SERVICE_NOTIFY_GMAIL_COM, reportRecipient, subject, message, attachment);
    }
    
}

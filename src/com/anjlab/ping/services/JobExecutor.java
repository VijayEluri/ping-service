package com.anjlab.ping.services;

import static com.anjlab.ping.services.Utils.getHttpCodeDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.entities.Job;
import com.anjlab.ping.entities.JobResult;
import com.anjlab.tapestry5.Utils;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.apphosting.api.ApiProxy.ApiDeadlineExceededException;


public class JobExecutor {
    protected static final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
    
    protected static void addFirefoxDefaultHeaders(HTTPRequest request) {
        request.addHeader(new HTTPHeader("User-Agent", 
                "Mozilla/5.0 (Windows; U; Windows NT 6.0; ru; rv:1.9.1.1) Gecko/20090715 Firefox/3.5.1 (.NET CLR 3.5.30729)"));
        request.addHeader(new HTTPHeader("Accept", 
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
    }

    private static final Logger logger = LoggerFactory.getLogger(JobExecutor.class);
    
    public JobResult execute(Job job) {
        JobResult jobResult = new JobResult();
        jobResult.setTimestamp(new Date());

        long startTime = jobResult.getTimestamp().getTime();

        String url = job.getPingURL();
        
        try {
            job.setLastPingTimestamp(new Date());
            
            HTTPRequest request = new HTTPRequest(new URL(url), HTTPMethod.GET);
            request.getFetchOptions().setDeadline(10d);
            request.getFetchOptions().doNotFollowRedirects();
            request.getFetchOptions().allowTruncate();

            addFirefoxDefaultHeaders(request);
            
            HTTPResponse response = urlFetchService.fetch(request);
            
            jobResult.setHTTPResponseCode(response.getResponseCode());
            
            job.setLastPingResult(0);

            StringBuffer sb = new StringBuffer();

            if (job.isUsesValidatingHttpCode()) {
                try {
                    StringBuffer hcvb = new StringBuffer();
                    checkHttpCodeValidation(job, response, hcvb);
                    sb.append(hcvb);
                } catch (Exception e) {
                    logger.warn("Error checking http code validation for url " + url, e);
                    sb.append("Error checking http code validation for url " + url + ": " + e.getMessage() + "\n");
                    job.setLastPingResult(job.getLastPingResult() | Job.PING_RESULT_HTTP_ERROR);
                }
            } else {
                sb.append("HTTP code validator wasn't configured for the job\n");
            }

            if (sb.length() > 0) {
                sb.append("\n");
            }

            if (job.isUsesValidatingRegexp()) {
                try {
                    StringBuffer rvb = new StringBuffer();
                    checkRegexpValidation(job, response, rvb);
                    sb.append(rvb);
                } catch (Exception e) {
                    logger.warn("Error checking regexp validation for url " + url, e);
                    sb.append("Error checking regexp validation for url " + url + ": " + e.getMessage());
                    job.setLastPingResult(job.getLastPingResult() | Job.PING_RESULT_REGEXP_VALIDATION_FAILED);
                }
            } else {
                sb.append("Regexp validator wasn't configured for the job");
            }

            if (job.getLastPingResult() == 0) {
                job.setLastPingResult(Job.PING_RESULT_OK);
            }
            
            job.setLastPingDetails(sb.toString());
        }
        catch (IOException e) {
            logger.debug("Error fetching url " + url, e);
            
            processException(job, url, e);
        }
        catch (ApiDeadlineExceededException e) {
            logger.debug("Error fetching url " + url, e);
            
            processException(job, url, e);
        }
        catch (Exception e) {
            logger.error("Error fetching url " + url, e);
            
            processException(job, url, e);
        }
        finally
        {
            long endTime = new Date().getTime();
            jobResult.setResponseTime((int)(endTime - startTime));
        }
        
        jobResult.setFailed(job.isLastPingFailed());
        jobResult.setPingResult(job.getLastPingResult());

        return jobResult;
    }

    private void processException(Job job, String url, Exception e) {
        job.setLastPingResult(Job.PING_RESULT_CONNECTIVITY_PROBLEM);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        e.printStackTrace(out);
        String trace = new String(baos.toByteArray());
        
        int endIndex = trace.indexOf("at " + Application.APP_PACKAGE);
        
        job.setLastPingDetails("Error fetching url " + url + ": " 
                + (endIndex < 0 ? trace : trace.substring(0, endIndex)));
    }

    private void checkHttpCodeValidation(Job job, HTTPResponse response,
            StringBuffer sb) {
        int responseCode = response.getResponseCode();
        int validatingCode = job.getValidatingHttpCode();
        
        if (validatingCode < 0) {
            validatingCode = Math.abs(validatingCode / 100);
            responseCode = Math.abs(responseCode / 100);
        }
        
        sb.append("HTTP response code validation ");
        
        if (responseCode != validatingCode) {
            job.setLastPingResult(job.getLastPingResult() | Job.PING_RESULT_HTTP_ERROR);
            
            sb.append("failed");
        } else {
            sb.append("succeeded");
        }
        
        sb.append(": expected ");
        sb.append(getHttpCodeDescription(job.getValidatingHttpCode()));
        sb.append(", was ");
        sb.append(response.getResponseCode());
        
        sb.append("\n\nHTTP headers:\n\n");
        for (int i = 0; i < response.getHeaders().size(); i++) {
            HTTPHeader httpHeader = response.getHeaders().get(i);
            sb.append(httpHeader.getName());
            sb.append('=');
            sb.append(httpHeader.getValue());
            sb.append('\n');
        }
    }

    private void checkRegexpValidation(Job job, HTTPResponse response,
            StringBuffer sb) throws UnsupportedEncodingException {
        String content = new String(response.getContent(), 
                Utils.isNullOrEmpty(job.getResponseEncoding()) 
                    ? "UTF-8" 
                    : job.getResponseEncoding());
        
        sb.append("Regexp validation ");

        Matcher m = Pattern.compile(job.getValidatingRegexp()).matcher(content);
        
        if (m.find()) {
            sb.append("succeeded");
        } else {
            job.setLastPingResult(job.getLastPingResult() | Job.PING_RESULT_REGEXP_VALIDATION_FAILED);

            sb.append("failed");
        }
        
        sb.append("\n\nResponse content is:\n\n");
        sb.append(content);
    }

}

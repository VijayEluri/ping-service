package dmitrygusev.ping.services;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.JobResult;

public class JobResultsAnalyzer {

    public static class JobResultsInterval {

        private Date startTime;
        private Date endTime;
        private int resultCode;
        private int resultsCount;
        
        public JobResultsInterval(JobResult jobResult) {
            this.resultCode = jobResult.getPingResult();
            this.startTime = jobResult.getTimestamp();
            this.endTime = startTime;
            this.resultsCount = 1;
        }

        public boolean append(JobResult jobResult) {
            this.endTime = jobResult.getTimestamp();
            boolean appended = jobResult.getPingResult() == resultCode;
            if (appended) {
                resultsCount++;
            }
            return appended;
        }

        public Date getStartTime() {
            return startTime;
        }
        
        public Date getEndTime() {
            return endTime;
        }
        
        public int getResultCode() {
            return resultCode;
        }

        public int getResultsCount() {
            return resultsCount;
        }
        
        public String getTimeDiffInWords() {
            return Utils.formatMillisecondsToWordsUpToMinutes(getMilliseconds());
        }

        public long getMilliseconds() {
            return endTime.getTime() - startTime.getTime();
        }
    }

    private List<JobResultsInterval> intervals;
    private Map<Integer, Long> resultCodeCounters;
    private Map<Integer, Long> resultsCountCounters;

    public JobResultsAnalyzer(List<JobResult> jobResults, boolean resultsSorted) {
        if (!resultsSorted) {
            sort(jobResults);
        }
        analyze(jobResults);
    }

    public List<JobResultsInterval> getIntervals() {
        return intervals;
    }

    public Map<Integer, Long> getResultCodeCounters() {
        return resultCodeCounters;
    }
    
    private void sort(List<JobResult> jobResults) {
        Collections.sort(jobResults, new Comparator<JobResult>() {
            @Override
            public int compare(JobResult o1, JobResult o2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
        });
    }

    private void analyze(List<JobResult> jobResults) {
        intervals = new ArrayList<JobResultsInterval>();
        resultCodeCounters = new HashMap<Integer, Long>();
        resultsCountCounters = new HashMap<Integer, Long>();
        
        if (jobResults.size() == 0) {
            return;
        }
        
        JobResultsInterval interval = new JobResultsInterval(jobResults.get(0));
        intervals.add(interval);
        
        for (int i = 1; i < jobResults.size(); i++) {
            JobResult jobResult = jobResults.get(i);

            if (!interval.append(jobResult)) {
                countInterval(interval);

                interval = new JobResultsInterval(jobResult);
                intervals.add(interval);
            }
        }

        countInterval(interval);
    }

    private void countInterval(JobResultsInterval interval) {
        incrementCounter(resultCodeCounters, interval.getMilliseconds(), interval);
        incrementCounter(resultsCountCounters, interval.getResultsCount(), interval);
    }

    private void incrementCounter(Map<Integer, Long> counters, long value, JobResultsInterval interval) {
        Long count = counters.get(interval.getResultCode());
        if (count == null) {
            count = value;
        } else {
            count += value;
        }
        counters.put(interval.getResultCode(), count);
    }

    private static final String spaces = buildString(50);

    private static String buildString(int n) {
        char[] a = new char[n];
        Arrays.fill(a, ' ');
        return new String(a);
    }

    public StringBuilder buildHtmlReport(TimeZone timeZone) {
        StringBuilder sb = new StringBuilder();

        sb.append("<p>Totals:</p>\n");
        
        long totalDurationMillis = 0;
        for (Long duration : resultCodeCounters.values()) {
            totalDurationMillis += duration; 
        }

        sb.append("<table>");
        
        for (Integer resultCode : resultCodeCounters.keySet()) {
            String status = Job.buildPingResultSummary(resultCode);
            Long durationMillis = resultCodeCounters.get(resultCode);
            String duration = Utils.formatMillisecondsToWordsUpToMinutes(durationMillis);
            
            sb.append("<tr><td style='text-align: right;'>");
            sb.append(status);
            sb.append(" :</td><td style='padding-left: 10px;'>");
            sb.append(duration);
            sb.append(" (");
            sb.append(String.format(Locale.ENGLISH, "%.5f", 100d * durationMillis / totalDurationMillis));
            sb.append(" %) verified by ");
            String resultsCount = resultsCountCounters.get(resultCode).toString();
            sb.append(resultsCount);
            sb.append(" ping(s)");
            sb.append("</td></tr>\n");
        }

        sb.append("</table>");
        
        sb.append("\n<p>Detailed report:</p>\n");
        
        DateFormat dateFormat = (DateFormat) Application.DATETIME_FORMAT.clone();
        if (timeZone != null) {
            dateFormat.setTimeZone(timeZone);
        }
        
        sb.append("<table>");
        
        for (JobResultsInterval interval : intervals) {
            sb.append("<tr><td>");
            sb.append(dateFormat.format(interval.getStartTime()));
            sb.append(" - ");
            sb.append(dateFormat.format(interval.getEndTime()));
            sb.append("</td><td style='padding-left: 10px;'>");
            String resultSummary = Job.buildPingResultSummary(interval.getResultCode());
            sb.append(resultSummary);
            sb.append("</td><td style='text-align: right; padding-left: 10px;'>");
            String timeDiffInWords = interval.getTimeDiffInWords();
            sb.append(timeDiffInWords);
            sb.append("</td><td style='text-align: right; padding-left: 10px;'>");
            String resultsCount = String.valueOf(interval.getResultsCount());
            sb.append(resultsCount);
            sb.append(" ping(s)");
            sb.append("</td></tr>\n");
        }
        
        sb.append("</table>");
        
        return sb;
    }
    
    public StringBuilder buildPlainTextReport(TimeZone timeZone) {
        StringBuilder sb = new StringBuilder();

        sb.append("Totals:\n");
        
        long totalDurationMillis = 0;
        for (Long duration : resultCodeCounters.values()) {
            totalDurationMillis += duration; 
        }
        
        for (Integer resultCode : resultCodeCounters.keySet()) {
            String status = Job.buildPingResultSummary(resultCode);
            Long durationMillis = resultCodeCounters.get(resultCode);
            String duration = Utils.formatMillisecondsToWordsUpToMinutes(durationMillis);
            
            sb.append(spaces.substring(0, 19 - status.length()));
            sb.append(status);
            sb.append(" : ");
            sb.append(duration);
            sb.append(" (");
            sb.append(String.format(Locale.ENGLISH, "%.5f", 100d * durationMillis / totalDurationMillis));
            sb.append(" %) verified by ");
            String resultsCount = resultsCountCounters.get(resultCode).toString();
            sb.append(resultsCount);
            sb.append(" ping(s)\n");
        }

        sb.append("\nDetailed report:\n\n");
        
        DateFormat dateFormat = (DateFormat) Application.DATETIME_FORMAT.clone();
        if (timeZone != null) {
            dateFormat.setTimeZone(timeZone);
        }
        
        for (JobResultsInterval interval : intervals) {
            sb.append(dateFormat.format(interval.getStartTime()));
            sb.append(" - ");
            sb.append(dateFormat.format(interval.getEndTime()));
            sb.append("  ");
            String resultSummary = Job.buildPingResultSummary(interval.getResultCode());
            sb.append(resultSummary);
            sb.append(spaces.substring(0, 19 - resultSummary.length()));
            String timeDiffInWords = interval.getTimeDiffInWords();
            sb.append(spaces.substring(0, 37 - timeDiffInWords.length()));
            sb.append(timeDiffInWords);
            sb.append(" ");
            String resultsCount = String.valueOf(interval.getResultsCount());
            sb.append(spaces.substring(0, 5 - resultsCount.length()));
            sb.append(resultsCount);
            sb.append(" ping(s)\n");
        }
        
        return sb;
    }

}
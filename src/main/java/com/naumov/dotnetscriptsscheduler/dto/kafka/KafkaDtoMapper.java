package com.naumov.dotnetscriptsscheduler.dto.kafka;

import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobCompletionStatus;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobFinishedMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobStatus;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.ScriptResults;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class KafkaDtoMapper {

    public Job fromJobFinishedMessage(JobFinishedMessage jobFinishedMessage) {
        Job job = new Job();
        job.setId(jobFinishedMessage.getJobId());

        JobStatus jobMessageStatus = jobFinishedMessage.getStatus();
        if (jobMessageStatus == JobStatus.ACCEPTED) {
            job.setStatus(Job.JobStatus.FINISHED);
            job.setResult(fromJobFinishedMessageScriptResults(jobFinishedMessage.getScriptResults()));
        } else if (jobMessageStatus == JobStatus.REJECTED) {
            job.setStatus(Job.JobStatus.REJECTED);
        } else {
            throw new IllegalStateException("Unable to map " + JobFinishedMessage.class.getSimpleName() + " to " +
                    Job.class.getSimpleName() + " with status " + jobMessageStatus);
        }

        return job;
    }

    private JobResult fromJobFinishedMessageScriptResults(ScriptResults scriptResults) {
        Objects.requireNonNull(scriptResults, "Parameter scriptResults must not be null");

        JobResult jobResult = new JobResult();
        jobResult.setFinishedWith(fromJobFinishedMessageScriptResultsFinishedWith(scriptResults.getFinishedWith()));
        jobResult.setStdout(scriptResults.getStdout());
        jobResult.setStderr(scriptResults.getStderr());

        return jobResult;
    }

    private JobResult.JobCompletionStatus fromJobFinishedMessageScriptResultsFinishedWith(JobCompletionStatus finishedWith) {
        Objects.requireNonNull(finishedWith, "Parameter finishedWith must not be null");

        return JobResult.JobCompletionStatus.valueOf(finishedWith.name());
    }
}

package com.naumov.dotnetscriptsscheduler.dto.kafka;

import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobCompletionStatus;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobFinishedMessage;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.JobStatus;
import com.naumov.dotnetscriptsscheduler.dto.kafka.cons.ScriptResults;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobConfig;
import com.naumov.dotnetscriptsscheduler.dto.kafka.prod.JobTaskMessage;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobPayloadConfig;
import com.naumov.dotnetscriptsscheduler.model.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class KafkaDtoMapper {

    // -------------------------------------------- "To" mappings --------------------------------------------------- //
    public JobTaskMessage toJobTaskMessage(Job job) {
        if (job == null) return null;

        JobTaskMessage.JobTaskMessageBuilder builder = JobTaskMessage.builder().jobId(job.getId());
        if (job.getRequest() != null && job.getRequest().getPayload() != null) {
            JobRequestPayload payload = job.getRequest().getPayload();
            builder.script(payload.getScript())
                    .jobConfig(toJobConfig(payload.getJobPayloadConfig()));
        }

        return builder.build();
    }

    private JobConfig toJobConfig(JobPayloadConfig jobPayloadConfigJson) {
        if (jobPayloadConfigJson == null) return null;

        return JobConfig.builder()
                .nugetConfigXml(jobPayloadConfigJson.getNugetConfigXml())
                .build();
    }

    // -------------------------------------------- "From" mappings ------------------------------------------------- //
    public Job fromJobFinishedMessage(JobFinishedMessage jobFinishedMessage) {
        Objects.requireNonNull(jobFinishedMessage, "Parameter jobFinishedMessage must not be null");

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

package com.naumov.dotnetscriptsscheduler.dto.rest;

import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayloadConfig;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.JobCreateResponse;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.JobGetResponse;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobPayloadConfig;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobRequestPayload;
import org.springframework.stereotype.Component;

@Component
public class RestDtoMapper {

    // -------------------------------------------- "From" mappings ------------------------------------------------- //
    public JobRequest fromJobCreateRequest(JobCreateRequest jobCreateRequest) {
        if (jobCreateRequest == null) return null;

        return JobRequest.builder()
                .messageId(jobCreateRequest.getRequestId())
                .senderId(jobCreateRequest.getSenderId())
                .payload(fromJobRequestPayload(jobCreateRequest.getPayload()))
                .build();
    }

    private JobRequestPayload fromJobRequestPayload(com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayload payload) {
        if (payload == null) return null;

        return JobRequestPayload.builder()
                .script(payload.getScript())
                .jobPayloadConfig(fromJobRequestPayloadConfig(payload.getJobConfig()))
                .agentType(payload.getAgentType())
                .build();
    }

    private JobPayloadConfig fromJobRequestPayloadConfig(JobRequestPayloadConfig jobConfig) {
        if (jobConfig == null) return null;

        return JobPayloadConfig.builder()
                .nugetConfigXml(jobConfig.getNugetConfigXml())
                .build();
    }

    // -------------------------------------------- "To" mappings --------------------------------------------------- //
    public JobCreateResponse toJobCreateResponse(Job job) {
        if (job == null) return null;
        return JobCreateResponse.builder()
                .jobId(job.getId())
                .build();
    }

    public JobGetResponse toJobGetResponse(Job job) {
        return null;  // TODO
    }
}
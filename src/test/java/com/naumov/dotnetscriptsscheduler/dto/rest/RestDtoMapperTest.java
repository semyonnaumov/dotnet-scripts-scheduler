package com.naumov.dotnetscriptsscheduler.dto.rest;

import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayloadConfig;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.JobCreateResponse;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobPayloadConfig;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RestDtoMapperTest {
    private final RestDtoMapper dtoMapper = new RestDtoMapper();

    @Test
    void testFromJobCreateRequestRegular() {
        JobRequestPayloadConfig jobRequestPayloadConfig = new JobRequestPayloadConfig();
        jobRequestPayloadConfig.setNugetConfigXml("<config/>");

        JobRequestPayload jobRequestPayload = new JobRequestPayload();
        jobRequestPayload.setScript("script");
        jobRequestPayload.setJobConfig(jobRequestPayloadConfig);
        jobRequestPayload.setAgentType("some-agent");

        JobCreateRequest jobCreateRequest = new JobCreateRequest();
        jobCreateRequest.setRequestId("111");
        jobCreateRequest.setSenderId("222");
        jobCreateRequest.setPayload(jobRequestPayload);

        JobRequest jobRequest = dtoMapper.fromJobCreateRequest(jobCreateRequest);
        assertNotNull(jobRequest);
        assertNull(jobRequest.getId());
        assertEquals("111", jobRequest.getMessageId());
        assertEquals("222", jobRequest.getSenderId());

        com.naumov.dotnetscriptsscheduler.model.JobRequestPayload payload = jobRequest.getPayload();
        assertNotNull(payload);
        assertNull(payload.getId());
        assertEquals("script", payload.getScript());
        assertEquals("some-agent", payload.getAgentType());

        JobPayloadConfig jobPayloadConfig = payload.getJobPayloadConfig();
        assertNotNull(jobPayloadConfig);
        assertEquals("<config/>", jobPayloadConfig.getNugetConfigXml());
    }

    @Test
    void testFromJobCreateRequestNull() {
        JobRequest jobRequest = dtoMapper.fromJobCreateRequest(null);
        assertNull(jobRequest);
    }

    @Test
    void testFromJobCreateRequestIncomplete() {
        JobCreateRequest jobCreateRequest = new JobCreateRequest();
        jobCreateRequest.setRequestId("111");
        jobCreateRequest.setSenderId("222");

        JobRequest jobRequest = dtoMapper.fromJobCreateRequest(jobCreateRequest);
        assertNotNull(jobRequest);
        assertNull(jobRequest.getId());
        assertEquals("111", jobRequest.getMessageId());
        assertEquals("222", jobRequest.getSenderId());
        assertNull(jobRequest.getPayload());
    }

    @Test
    void testToJobCreateResponseRegular() {
        UUID uuid = UUID.fromString("7f000001-8637-1fc4-8186-374017c10000");
        Job job = Job.builder().id(uuid).build();

        JobCreateResponse jobCreateResponse = dtoMapper.toJobCreateResponse(job);
        assertNotNull(jobCreateResponse);
        assertEquals(uuid, jobCreateResponse.getJobId());
    }

    @Test
    void testToJobCreateResponseNull() {
        JobCreateResponse jobCreateResponse = dtoMapper.toJobCreateResponse(null);
        assertNull(jobCreateResponse);
    }
}
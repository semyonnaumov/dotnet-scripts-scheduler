package com.naumov.dotnetscriptsscheduler.controller;

import com.naumov.dotnetscriptsscheduler.dto.rest.RestDtoMapper;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.*;
import com.naumov.dotnetscriptsscheduler.model.*;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import com.naumov.dotnetscriptsscheduler.model.JobStatus;
import com.naumov.dotnetscriptsscheduler.service.JobsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobsControllerTest {
    private final RestDtoMapper dtoMapper = new RestDtoMapper();
    private JobsService jobsServiceMock;
    private JobsController jobsController;

    @BeforeEach
    void setup() {
        jobsServiceMock = mock(JobsService.class);
        jobsController = new JobsController(jobsServiceMock, dtoMapper);
    }

    @Test
    void createJobJobExists() {
        UUID jobId = UUID.randomUUID();
        Job existingJob = Job.builder().id(jobId).build();
        when(jobsServiceMock.createOrGetJob(any())).thenReturn(JobCreationResult.ofExistingJob(existingJob));
        JobCreateRequest jobCreateRequest = JobCreateRequest.builder().requestId("request-id").build();

        ResponseEntity<JobCreateResponse> responseEntity = jobsController.createJob(jobCreateRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(jobId, responseEntity.getBody().getJobId());
    }

    @Test
    void createJobJobNotExists() {
        UUID jobId = UUID.randomUUID();
        Job newJob = Job.builder().id(jobId).build();
        when(jobsServiceMock.createOrGetJob(any())).thenReturn(JobCreationResult.ofNewJob(newJob));
        JobCreateRequest jobCreateRequest = JobCreateRequest.builder().requestId("request-id").build();

        ResponseEntity<JobCreateResponse> responseEntity = jobsController.createJob(jobCreateRequest);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(jobId, responseEntity.getBody().getJobId());
    }

    @Test
    void getJobJobExist() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.findJob(any())).thenReturn(Optional.of(Job.builder().id(jobId).build()));

        ResponseEntity<JobGetResponse> responseEntity = jobsController.getJob(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(jobId, responseEntity.getBody().getJobId());
    }

    @Test
    void getJobJobNotExist() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.findJob(any())).thenReturn(Optional.empty());

        ResponseEntity<JobGetResponse> responseEntity = jobsController.getJob(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void getJobRequestJobExists() {
        UUID jobId = UUID.randomUUID();
        String messageId = "message-id";
        when(jobsServiceMock.findJobRequestByJobId(any())).thenReturn(Optional.of(JobRequest.builder().messageId(messageId).build()));

        ResponseEntity<JobGetRequestResponse> responseEntity = jobsController.getJobRequest(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(jobId, responseEntity.getBody().getJobId());
        assertNotNull(responseEntity.getBody().getRequest());
        assertNotNull(messageId, responseEntity.getBody().getRequest().getRequestId());
    }

    @Test
    void getJobRequestJobNotExists() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.findJobRequestByJobId(any())).thenReturn(Optional.empty());

        ResponseEntity<JobGetRequestResponse> responseEntity = jobsController.getJobRequest(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void getJobStatusJobExists() {
        UUID jobId = UUID.randomUUID();
        JobStatus jobStatus = JobStatus.RUNNING;
        when(jobsServiceMock.findJobStatusByJobId(any())).thenReturn(Optional.of(jobStatus));

        ResponseEntity<JobGetStatusResponse> responseEntity = jobsController.getJobStatus(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(jobId, responseEntity.getBody().getJobId());
        assertNotNull(responseEntity.getBody().getStatus());
        assertEquals(jobStatus.toString(), responseEntity.getBody().getStatus().toString());
    }

    @Test
    void getJobStatusJobNotExists() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.findJobStatusByJobId(any())).thenReturn(Optional.empty());

        ResponseEntity<JobGetStatusResponse> responseEntity = jobsController.getJobStatus(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void getJobResultJobExists() {
        UUID jobId = UUID.randomUUID();
        String stderr = "some-stdout";
        when(jobsServiceMock.findJobResultByJobId(any())).thenReturn(Optional.of(JobResult.builder().stderr(stderr).build()));

        ResponseEntity<JobGetResultResponse> responseEntity = jobsController.getJobResult(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(jobId, responseEntity.getBody().getJobId());
        assertNotNull(responseEntity.getBody().getResult());
        assertEquals(stderr, responseEntity.getBody().getResult().getStderr());
    }

    @Test
    void getJobResultJobNotExists() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.findJobResultByJobId(any())).thenReturn(Optional.empty());

        ResponseEntity<JobGetResultResponse> responseEntity = jobsController.getJobResult(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void deleteJobJobExists() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.deleteJob(any())).thenReturn(true);

        ResponseEntity<?> responseEntity = jobsController.deleteJob(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    void deleteJobJobNotExists() {
        UUID jobId = UUID.randomUUID();
        when(jobsServiceMock.deleteJob(any())).thenReturn(false);

        ResponseEntity<?> responseEntity = jobsController.deleteJob(jobId);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }


    @Test
    void handleBadRequestExceptions() {
        ResponseEntity<DefaultErrorResponse> responseEntity = jobsController.handleBadRequestExceptions(new RuntimeException());

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Test
    void handleAllOtherExceptions() {
        ResponseEntity<DefaultErrorResponse> responseEntity = jobsController.handleAllOtherExceptions(new RuntimeException());

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }
}
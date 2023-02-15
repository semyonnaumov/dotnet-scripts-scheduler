package com.naumov.dotnetscriptsscheduler.scheduled;

import com.naumov.dotnetscriptsscheduler.AbstractIntegrationTest;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.model.JobStatus;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import com.naumov.dotnetscriptsscheduler.service.WorkerTypesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DirtiesContext
class PeriodicLongJobsCleanupServiceIntegrationTest extends AbstractIntegrationTest {
    @Value("${scheduler.jobs.rejected-timeout-ms}")
    private int jobTimeoutMs;
    @Value("${scheduler.jobs.rejected-update-period-ms}")
    private int cleanupPeriodMs;
    @Autowired
    private JobsRepository jobsRepository;
    @Autowired
    private WorkerTypesService workerTypesService;
    @SpyBean
    private PeriodicLongJobsCleanupService periodicLongJobsCleanupService;

    @BeforeEach
    void setup() {
        assertEquals(0, jobsRepository.count());
    }

    @AfterEach
    void teardown() {
        jobsRepository.deleteAll();
    }

    @Test
    void scheduleFixedRateWithInitialDelayTask() {
        Job savedJob = prepareAndSaveJob();
        UUID jobId = savedJob.getId();
        assertTrue(jobsRepository.existsById(jobId));

        int cleanupCyclesBeforeCleanup = 1 + jobTimeoutMs / cleanupPeriodMs;
        verify(periodicLongJobsCleanupService,
                timeout(jobTimeoutMs + 2L * cleanupPeriodMs)
                        .atLeast(cleanupCyclesBeforeCleanup)).rejectLongLastingJobs();

        Optional<Job> jobOptional = jobsRepository.findById(jobId);
        assertTrue(jobOptional.isPresent());
        assertEquals(JobStatus.REJECTED, jobOptional.get().getStatus());
    }

    private Job prepareAndSaveJob() {
        JobRequestPayload payload = JobRequestPayload.builder()
                .script("script")
                .agentType(workerTypesService.getDefaultWorkerType())
                .build();

        JobRequest jobRequest = JobRequest.builder()
                .messageId("message-id")
                .senderId("sender-id")
                .payload(payload)
                .build();

        Job job = Job.builder()
                .status(JobStatus.PENDING)
                .request(jobRequest)
                .build();

        return jobsRepository.saveAndFlush(job);
    }
}
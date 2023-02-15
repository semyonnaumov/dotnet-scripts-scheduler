package com.naumov.dotnetscriptsscheduler.scheduled;

import com.naumov.dotnetscriptsscheduler.model.JobStatus;
import com.naumov.dotnetscriptsscheduler.service.JobsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PeriodicLongJobsCleanupService {
    private static final Logger LOGGER = LogManager.getLogger(PeriodicLongJobsCleanupService.class);
    private final JobsService jobsService;
    private final int jobTimeoutMs;

    @Autowired
    public PeriodicLongJobsCleanupService(JobsService jobsService,
                                          @Value("${scheduler.jobs.rejected-timeout-ms}") int jobTimeoutMs) {
        this.jobsService = jobsService;
        this.jobTimeoutMs = jobTimeoutMs;
    }

    @Scheduled(
            initialDelayString = "${scheduler.jobs.rejected-update-period-ms}",
            fixedDelayString = "${scheduler.jobs.rejected-update-period-ms}"
    )
    public void rejectLongLastingJobs() {
        try {
            int updatedJobsNumber = jobsService.rejectLongLastingJobs(jobTimeoutMs);
            if (updatedJobsNumber > 0) {
                LOGGER.info("Periodic long jobs cleanup: set {} number of jobs to {} status",
                        updatedJobsNumber, JobStatus.REJECTED);
            } else {
                LOGGER.info("Periodic long jobs cleanup: no long lasting jobs found");
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to perform periodic long jobs cleanup", e);
            throw e;
        }
    }
}

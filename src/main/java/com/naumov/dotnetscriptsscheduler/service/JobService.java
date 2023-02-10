package com.naumov.dotnetscriptsscheduler.service;

import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobCreationResult;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobResult;

import java.util.Optional;
import java.util.UUID;

public interface JobService {

    // must be idempotent
    JobCreationResult createOrGetJob(JobRequest job);

    Optional<Job> findJob(UUID id);

    Optional<JobRequest> findJobRequestByJobId(UUID id);

    Optional<Job.JobStatus> findJobStatusByJobId(UUID id);

    Optional<JobResult> findJobResultByJobId(UUID id);

    boolean deleteJob(UUID id);

    // must be idempotent
    void updateStartedJob(UUID id);

    // must be idempotent
    void updateFinishedJob(Job job);
}

package com.naumov.dotnetscriptsscheduler.service;

import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobResult;

import java.util.Optional;

public interface JobService {

    Job createJob(Job job);

    Optional<Job> findJob(String id);

    Optional<Job> findJobByRequestId(String requestId);

    Optional<JobRequest> findJobRequestByJobId(String jobId);

    Optional<JobResult> findJobResultByJobId(String jobId);

    boolean deleteJob(String id);
}

package com.naumov.dotnetscriptsscheduler.service.impl;

import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import com.naumov.dotnetscriptsscheduler.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {
    private final JobsRepository jobsRepository;

    @Autowired
    public JobServiceImpl(JobsRepository jobsRepository) {
        this.jobsRepository = jobsRepository;
    }

    @Override
    public Job createJob(Job job) {
        job.setId(job.getRequest().getMessageId());
        return job;
    }

    @Override
    public Optional<Job> findJob(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<Job> findJobByRequestId(String requestId) {
        return Optional.empty();
    }

    @Override
    public Optional<JobRequest> findJobRequestByJobId(String jobId) {
        return Optional.empty();
    }

    @Override
    public Optional<JobResult> findJobResultByJobId(String jobId) {
        return Optional.empty();
    }

    @Override
    public boolean deleteJob(String id) {
        return true;
    }
}

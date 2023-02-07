package com.naumov.dotnetscriptsscheduler.service.impl;

import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.service.JobService;
import org.springframework.stereotype.Service;

import java.util.Optional;

// TODO
@Service
public class JobServiceImpl implements JobService {

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
    public boolean deleteJob(String id) {
        return true;
    }
}

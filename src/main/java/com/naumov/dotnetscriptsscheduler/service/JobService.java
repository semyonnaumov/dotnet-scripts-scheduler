package com.naumov.dotnetscriptsscheduler.service;

import com.naumov.dotnetscriptsscheduler.model.Job;

import java.util.Optional;

public interface JobService {

    Job createJob(Job job);

    Optional<Job> findJob(String id);

    Optional<Job> findJobByRequestId(String requestId);

    boolean deleteJob(String id);
}

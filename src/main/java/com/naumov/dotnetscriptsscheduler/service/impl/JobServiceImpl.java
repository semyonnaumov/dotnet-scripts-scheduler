package com.naumov.dotnetscriptsscheduler.service.impl;

import com.naumov.dotnetscriptsscheduler.kafka.JobMessagesProducer;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobCreationResult;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import com.naumov.dotnetscriptsscheduler.repository.JobsRepository;
import com.naumov.dotnetscriptsscheduler.service.JobService;
import com.naumov.dotnetscriptsscheduler.service.exception.JobServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class JobServiceImpl implements JobService {
    private static final Logger LOGGER = LogManager.getLogger(JobServiceImpl.class);
    private final JobsRepository jobsRepository;
    private final JobMessagesProducer jobMessagesProducer;

    @Autowired
    public JobServiceImpl(JobsRepository jobsRepository, JobMessagesProducer jobMessagesProducer) {
        this.jobsRepository = jobsRepository;
        this.jobMessagesProducer = jobMessagesProducer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    @Override
    public JobCreationResult createOrGetJob(JobRequest jobRequest) {
        LOGGER.debug("Job creation requested, job request {}", jobRequest);

        validateJobRequest(jobRequest);

        Optional<Job> foundJobOptional = jobsRepository.findByRequestMessageId(jobRequest.getMessageId());
        if (foundJobOptional.isPresent()) {
            LOGGER.info("Skip job creation, found existing job with message id {}", jobRequest.getMessageId());
            return JobCreationResult.ofExistingJob(foundJobOptional.get());
        }

        try {
            Job newJob = prepareJob(jobRequest);
            Job savedJob = jobsRepository.saveAndFlush(newJob); // all associations saved here using cascade
            jobMessagesProducer.sendJobTaskMessageAsync(savedJob);

            return JobCreationResult.ofNewJob(savedJob);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to create new job from job request {}", jobRequest, e);
            throw e;
        }
    }

    private void validateJobRequest(JobRequest jobRequest) {
        if (jobRequest == null) throw new JobServiceException("Cannot create job from null request");
        if (jobRequest.getMessageId() == null) throw new JobServiceException("No message id in job request");
        if (jobRequest.getId() != null) throw new JobServiceException("Job creation request cannot have an ID");
    }

    private Job prepareJob(JobRequest jobRequest) {
        return Job.builder()
                .status(Job.JobStatus.PENDING)
                .request(jobRequest)
                .build();
    }

    @Transactional
    @Override
    public Optional<Job> findJob(UUID id) {
        return jobsRepository.findById(id);
    }

    @Override
    public Optional<JobRequest> findJobRequestByJobId(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<JobResult> findJobResultByJobId(UUID id) {
        return Optional.empty();
    }

    @Override
    public boolean deleteJob(UUID id) {
        return true;
    }

    @Transactional
    @Override
    public void onJobStarted(UUID id) {

    }

    @Transactional
    @Override
    public void onJobFinished(Job job) {

    }
}

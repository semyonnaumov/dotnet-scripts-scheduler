package com.naumov.dotnetscriptsscheduler.repository;

import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface JobsRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByRequestMessageId(String messageId);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"request", "request.payload", "result"})
    Optional<Job> findById(@NonNull UUID id);

    @Query("SELECT j.request FROM Job j LEFT JOIN j.request WHERE j.id = :id")
    Optional<JobRequest> findJobRequestByJobId(UUID id);

    @Query("SELECT j.status FROM Job j WHERE j.id=:id")
    Optional<Job.JobStatus> findJobStatusByJobId(UUID id);

    @Query("SELECT j.result FROM Job j LEFT JOIN j.result WHERE j.id = :id")
    Optional<JobResult> findJobResultByJobId(UUID id);

    @Modifying
    @Query("UPDATE Job j SET j.status = :status WHERE j.id = :id")
    void updateJobSetStatusTo(UUID id, Job.JobStatus status);

    // TODO verify
//    @Query("UPDATE Job j SET j.status = :outdatedJobStatus WHERE CURRENT_TIMESTAMP > j.created")
//    @Modifying
//    void updateUnfinishedJobs(Job.JobStatus unfinishedJobNewStatus);
}

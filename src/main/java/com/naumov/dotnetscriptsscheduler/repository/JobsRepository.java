package com.naumov.dotnetscriptsscheduler.repository;

import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.model.JobRequest;
import com.naumov.dotnetscriptsscheduler.model.JobResult;
import com.naumov.dotnetscriptsscheduler.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface JobsRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByRequestMessageId(String messageId);

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    @Query("FROM Job j " +
            "LEFT JOIN FETCH j.request " +
            "LEFT JOIN FETCH j.request.payload " +
            "LEFT JOIN FETCH j.result " +
            "WHERE j.id = :id")
    Optional<Job> findByIdFetchAll(UUID id);

    @Query("SELECT j.request FROM Job j LEFT JOIN j.request WHERE j.id = :id")
    Optional<JobRequest> findJobRequestByJobId(UUID id);

    @Query("SELECT j.status FROM Job j WHERE j.id=:id")
    Optional<JobStatus> findJobStatusByJobId(UUID id);

    @Query("SELECT j.result FROM Job j LEFT JOIN j.result WHERE j.id = :id")
    Optional<JobResult> findJobResultByJobId(UUID id);

    @Query("UPDATE Job j SET j.status = :status WHERE j.id = :id")
    @Modifying
    void updateJobSetStatusTo(UUID id, JobStatus status);

    @Query("UPDATE Job j SET j.status = com.naumov.dotnetscriptsscheduler.model.JobStatus.REJECTED, j.result = null " +
            "WHERE j.status IN " +
            "(com.naumov.dotnetscriptsscheduler.model.JobStatus.PENDING, " +
            "com.naumov.dotnetscriptsscheduler.model.JobStatus.RUNNING) AND " +
            "CURRENT_TIMESTAMP - j.creationOffsetDateTime > :jobTimeout")
    @Modifying
    int rejectJobsRunningLongerThanJobTimeout(Duration jobTimeout);
}

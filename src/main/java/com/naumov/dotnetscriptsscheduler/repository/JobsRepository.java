package com.naumov.dotnetscriptsscheduler.repository;

import com.naumov.dotnetscriptsscheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface JobsRepository extends JpaRepository<Job, String> {

    Optional<Job> findByRequestMessageId(String messageId);

    @Query("FROM Job j " +
            "LEFT JOIN FETCH j.request")
    Optional<Job> findByIdFetchRequest(UUID id);

    @Query("FROM Job j " +
            "LEFT JOIN FETCH j.result")
    Optional<Job> findByIdFetchResult(UUID id);

    @Query("FROM Job j " +
            "LEFT JOIN FETCH j.request " +
            "LEFT JOIN FETCH j.result")
    Optional<Job> findByIdFetchAll(UUID id);
}

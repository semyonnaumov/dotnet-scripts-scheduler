package com.naumov.dotnetscriptsscheduler.repository;

import com.naumov.dotnetscriptsscheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface JobsRepository extends JpaRepository<Job, String> {

    Optional<Job> findByRequestMessageId(String messageId);
}

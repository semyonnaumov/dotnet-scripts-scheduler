package com.naumov.dotnetscriptsscheduler.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "uuid")
    private String id;
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private JobRequest request;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "result_id")
    private JobResult result;

    public enum JobStatus {
        PENDING,
        RUNNING,
        FINISHED,
        REJECTED
    }
}
package com.naumov.dotnetscriptsscheduler.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "uuid")
    private UUID id;
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private JobRequest request;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "result_id")
    private JobResult result;

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", request=" + request +
                ", result=" + result +
                '}';
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        FINISHED,
        REJECTED
    }
}
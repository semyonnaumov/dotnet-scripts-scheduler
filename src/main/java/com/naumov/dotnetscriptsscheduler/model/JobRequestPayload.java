package com.naumov.dotnetscriptsscheduler.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "job_request_payloads")
public class JobRequestPayload {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "job_request_payloads_gen")
    @SequenceGenerator(name = "job_request_payloads_gen", sequenceName = "job_request_payloads_seq", allocationSize = 10)
    private Long id;
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "script", columnDefinition = "text", nullable = false, updatable = false)
    private String script;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", updatable = false)
    private JobPayloadConfig jobPayloadConfigJson;
    @Column(name = "agent_type", nullable = false, updatable = false)
    private String agentType;
}

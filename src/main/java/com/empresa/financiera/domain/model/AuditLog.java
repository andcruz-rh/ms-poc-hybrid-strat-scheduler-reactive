package com.empresa.financiera.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un registro de auditoría de ejecución de jobs.
 * 
 * @param id Identificador único del registro
 * @param jobName Nombre del job que se ejecutó
 * @param executedAt Fecha y hora de ejecución del job
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 255)
    private String jobName;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public AuditLog() {
        // Constructor por defecto requerido por JPA
    }

    /**
     * Constructor con parámetros.
     * 
     * @param jobName Nombre del job que se ejecutó
     * @param executedAt Fecha y hora de ejecución del job
     */
    public AuditLog(String jobName, LocalDateTime executedAt) {
        this.jobName = jobName;
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}


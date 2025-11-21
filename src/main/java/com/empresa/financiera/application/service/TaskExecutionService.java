package com.empresa.financiera.application.service;

import com.empresa.financiera.domain.model.AuditLog;
import com.empresa.financiera.infrastructure.repository.AuditRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Servicio que encapsula la lógica de negocio para la ejecución de tareas.
 * Realiza operaciones de persistencia reactiva y transaccional.
 */
@Slf4j
@ApplicationScoped
public class TaskExecutionService {

    private final AuditRepository auditRepository;

    /**
     * Constructor con inyección de dependencias.
     * 
     * @param auditRepository Repositorio para operaciones de persistencia de AuditLog
     */
    public TaskExecutionService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Ejecuta una tarea de negocio y registra su ejecución en la base de datos.
     * La anotación @WithTransaction asegura que la operación se ejecute dentro de una transacción reactiva.
     * 
     * @param sourceName Nombre del origen/job que ejecuta la tarea
     * @return Uni<Void> que completa cuando la tarea se ha ejecutado y persistido
     */
    @WithTransaction
    public Uni<Void> executeBusinessTask(String sourceName) {
        log.info("Ejecutando tarea de negocio para: {}", sourceName);
        
        AuditLog auditLog = new AuditLog(sourceName, LocalDateTime.now());
        
        return auditRepository.persist(auditLog)
            .onItem().invoke(() -> log.debug("Registro de auditoría persistido exitosamente para: {}", sourceName))
            .onFailure().invoke(failure -> log.error("Error al persistir registro de auditoría para: {}", sourceName, failure))
            .replaceWithVoid();
    }
}


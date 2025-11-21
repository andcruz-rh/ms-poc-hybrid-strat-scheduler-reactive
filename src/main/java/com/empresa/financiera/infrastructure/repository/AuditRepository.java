package com.empresa.financiera.infrastructure.repository;

import com.empresa.financiera.domain.model.AuditLog;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repositorio reactivo para la entidad AuditLog.
 * Utiliza Hibernate Reactive Panache para operaciones de persistencia reactivas.
 */
@ApplicationScoped
public class AuditRepository implements PanacheRepository<AuditLog> {
    // Panache proporciona automáticamente métodos como persist(), findById(), findAll(), etc.
}


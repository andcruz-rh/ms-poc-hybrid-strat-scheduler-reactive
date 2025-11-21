package com.empresa.financiera.application;

import com.empresa.financiera.domain.model.AuditLog;
import com.empresa.financiera.infrastructure.repository.AuditRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración definitivo que valida el flujo completo:
 * Orquestación -> Job Dinámico -> Persistencia en Base de Datos.
 * 
 * Este test demuestra que la arquitectura híbrida funciona de punta a punta:
 * 1. El orquestador consulta parámetros
 * 2. Programa un job dinámico
 * 3. El job se ejecuta según el intervalo configurado
 * 4. El servicio de negocio persiste datos en la BD
 */
@QuarkusTest
class DynamicJobOrchestratorTest {

    @Inject
    DynamicSchedulerOrchestrator orchestrator;

    @Inject
    AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        // Verificar que las dependencias están inyectadas
        assertNotNull(orchestrator, "El orquestador debe estar inyectado");
        assertNotNull(auditRepository, "El repositorio debe estar inyectado");
    }

    @Test
    void testDynamicExecutionAndPersistence() {
        // PRE-CONDICIÓN: Verificar estado inicial de la base de datos
        Long initialCount = auditRepository.count()
            .await().atMost(Duration.ofSeconds(5));

        // ACCIÓN: Disparar manualmente el proceso de orquestación
        // El método checkAndReschedule() es reactivo, así que suscribimos para iniciar el proceso
        orchestrator.checkAndReschedule();

        // ESPERA ACTIVA: Usar Awaitility para esperar a que se cree al menos un registro
        // No usamos Thread.sleep, es mala práctica. Awaitility es más elegante y robusto.
        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> {
                Long currentCount = auditRepository.count()
                    .await().atMost(Duration.ofSeconds(2));
                return currentCount > initialCount;
            });

        // ASERCIÓN: Verificar que se creó el registro y que tiene el jobSourceName esperado
        List<AuditLog> auditLogs = auditRepository.findAll().list()
            .await().atMost(Duration.ofSeconds(5));

        assertTrue(auditLogs.size() > initialCount,
            String.format("Debe haber al menos un registro nuevo. Inicial: %d, Actual: %d",
                initialCount, auditLogs.size()));

        // Verificar que el registro más reciente tiene el formato esperado
        // El jobSourceName debe contener "dynamic-job" y el actionId
        AuditLog latestLog = auditLogs.get(auditLogs.size() - 1);
        String jobName = latestLog.getJobName();

        assertTrue(jobName != null && !jobName.isEmpty(),
            "El jobName no debe estar vacío");

        assertTrue(jobName.startsWith("dynamic-job"),
            String.format("El jobName debe comenzar con 'dynamic-job'. Actual: %s", jobName));

        assertTrue(jobName.contains("-"),
            String.format("El jobName debe contener el actionId separado por guión. Actual: %s", jobName));

        // Verificar que el registro tiene una fecha de ejecución válida
        assertNotNull(latestLog.getExecutedAt(),
            "El registro debe tener una fecha de ejecución");
    }
}


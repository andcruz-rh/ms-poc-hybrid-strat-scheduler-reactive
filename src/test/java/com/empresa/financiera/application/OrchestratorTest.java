package com.empresa.financiera.application;

import com.empresa.financiera.infrastructure.repository.AuditRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración para verificar que el DynamicSchedulerOrchestrator
 * se levanta correctamente, programa jobs dinámicos y ejecuta tareas de negocio
 * que persisten datos en la base de datos.
 */
@QuarkusTest
class OrchestratorTest {

    @Inject
    DynamicSchedulerOrchestrator orchestrator;

    @Inject
    AuditRepository auditRepository;

    @Test
    void whenOrchestratorBeanIsInjected_thenBeanIsNotNull() {
        assertNotNull(orchestrator, "El orquestador debe estar inyectado correctamente");
    }

    @Test
    void whenApplicationStarts_thenOrchestratorInitializesWithoutErrors() {
        // Este test verifica que el bean se inicializa correctamente
        // Si hay errores en la inicialización, el test fallará
        assertNotNull(orchestrator, "El orquestador debe estar disponible después de la inicialización");
    }

    @Test
    void whenJobIsScheduled_thenJobExecutesAndPersistsData() {
        // Verificar que el repositorio está inyectado
        assertNotNull(auditRepository, "El repositorio debe estar inyectado correctamente");

        // Obtener el conteo inicial de registros
        Long initialCount = auditRepository.count()
            .await().atMost(Duration.ofSeconds(5));

        // Invocar manualmente el método de programación para disparar la creación del job
        orchestrator.checkAndReschedule();

        // Dar tiempo para que el proceso reactivo se complete y el job se programe
        try {
            Thread.sleep(2000); // 2 segundos para permitir que el job se programe
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Esperar a que el job programado se ejecute al menos una vez
        // El job dinámico se programa con un intervalo (ej: PT10S), así que esperamos un poco más
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                Long currentCount = auditRepository.count()
                    .await().atMost(Duration.ofSeconds(5));
                return currentCount > initialCount;
            });

        // Verificar que se crearon registros en la base de datos
        Long finalCount = auditRepository.count()
            .await().atMost(Duration.ofSeconds(5));

        assertTrue(finalCount > initialCount, 
            String.format("Debe haber al menos un registro de auditoría. Inicial: %d, Final: %d", 
                initialCount, finalCount));
    }
}

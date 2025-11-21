package com.empresa.financiera.infrastructure.service;

import com.empresa.financiera.domain.model.JobParameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Random;

/**
 * Servicio mock que simula la obtención de parámetros desde una fuente externa
 * (Base de datos o API). Simula latencia de red/base de datos mediante delays reactivos.
 */
@Slf4j
@ApplicationScoped
public class MockParameterService {

    private static final Random random = new Random();

    /**
     * Constructor explícito para mantener el patrón de inyección por constructor.
     * Aunque no tenga dependencias actualmente, define el patrón para futuras extensiones.
     */
    public MockParameterService() {
        // Constructor explícito para mantener el patrón de inyección por constructor
    }

    /**
     * Simula la obtención de parámetros de configuración desde una fuente externa.
     * Incluye simulación de latencia (100ms) usando operadores reactivos de Mutiny.
     * 
     * @return Uni que emite un JobParameters con los parámetros simulados
     */
    public Uni<JobParameters> fetchParameters() {
        log.info("Consultando fuente externa de parámetros...");
        
        return Uni.createFrom().item(() -> {
            // Valores simulados
            // Simula un intervalo entre 10 y 30 segundos aleatorio
            int seconds = 10 + random.nextInt(21); // random.nextInt(21) devuelve 0-20, sumando 10 da 10-30
            String interval = "PT" + seconds + "S"; // Periodo aleatorio entre 10 y 30 segundos en formato ISO-8601
            String actionId = "ACTION_" + random.nextInt(1000);
            
            return new JobParameters(interval, actionId);
        })
        .onItem().delayIt().by(Duration.ofMillis(100))
        .onFailure().invoke(throwable -> 
            log.error("Error al consultar fuente externa de parámetros", throwable)
        );
    }
}


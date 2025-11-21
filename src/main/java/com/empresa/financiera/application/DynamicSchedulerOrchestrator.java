package com.empresa.financiera.application;

import com.empresa.financiera.application.service.TaskExecutionService;
import com.empresa.financiera.domain.model.JobParameters;
import com.empresa.financiera.infrastructure.service.MockParameterService;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestador que implementa la estrategia híbrida:
 * - Un job fijo (@Scheduled) verifica periódicamente los parámetros
 * - Crea dinámicamente un nuevo job basado en los parámetros obtenidos
 */
@Slf4j
@ApplicationScoped
public class DynamicSchedulerOrchestrator {

    private static final String DYNAMIC_JOB_IDENTITY = "dynamic-job";
    
    private final MockParameterService parameterService;
    private final Scheduler scheduler;
    private final TaskExecutionService taskExecutionService;
    
    private String lastInterval;

    /**
     * Constructor con inyección de dependencias.
     * 
     * @param parameterService Servicio para obtener parámetros de configuración
     * @param scheduler Scheduler de Quarkus para crear jobs programáticos
     * @param taskExecutionService Servicio para ejecutar tareas de negocio
     */
    public DynamicSchedulerOrchestrator(
            MockParameterService parameterService, 
            Scheduler scheduler,
            TaskExecutionService taskExecutionService) {
        this.parameterService = parameterService;
        this.scheduler = scheduler;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * Método orquestador que se ejecuta cada 30 segundos.
     * Consulta los parámetros y programa/reprograma el job dinámico.
     * Package-private para permitir testing.
     */
    @Scheduled(every = "30s")
    void checkAndReschedule() {
        log.info("Orquestador: Verificando parámetros para reprogramar job dinámico...");
        
        parameterService.fetchParameters()
            .subscribe().with(
                jobParameters -> {
                    log.info("Orquestador: Parámetros obtenidos - Intervalo: {}, ActionId: {}", 
                        jobParameters.interval(), jobParameters.actionId());
                    
                    scheduleDynamicJob(jobParameters);
                },
                failure -> log.error("Orquestador: Error al obtener parámetros", failure)
            );
    }

    /**
     * Programa o reprograma el job dinámico basado en los parámetros obtenidos.
     * Si el intervalo cambia, se reprograma el job.
     * 
     * @param jobParameters Parámetros de configuración del job
     */
    private void scheduleDynamicJob(JobParameters jobParameters) {
        String currentInterval = jobParameters.interval();
        
        // Si el intervalo no ha cambiado, no es necesario reprogramar
        if (currentInterval.equals(lastInterval)) {
            log.debug("Orquestador: El intervalo no ha cambiado ({}), manteniendo job actual", currentInterval);
            return;
        }
        
        log.info("Orquestador: Programando job dinámico con intervalo: {}", currentInterval);
        
        // Eliminar el job anterior si existe (necesario antes de crear uno nuevo con la misma identidad)
        try {
            scheduler.unscheduleJob(DYNAMIC_JOB_IDENTITY);
            log.debug("Orquestador: Job anterior eliminado exitosamente");
        } catch (Exception e) {
            // Si el job no existe, es la primera vez que se programa, no es un error
            log.debug("Orquestador: No se encontró job anterior para eliminar (primera ejecución o ya eliminado)");
        }
        
        // Crear el nuevo job dinámico
        String actionId = jobParameters.actionId();
        String jobSourceName = String.format("%s-%s", DYNAMIC_JOB_IDENTITY, actionId);
        
        
        scheduler.newJob(DYNAMIC_JOB_IDENTITY)
            .setInterval(currentInterval)
            .setAsyncTask(executionContext -> {
                // Ejecutar tarea de negocio de forma reactiva y transaccional
                // setAsyncTask espera un Uni<Void>, así que retornamos el Uni directamente
                return taskExecutionService.executeBusinessTask(jobSourceName)
                    .onItem().invoke(() -> log.debug("Tarea de negocio ejecutada exitosamente para: {}", jobSourceName))
                    .onFailure().invoke(failure -> log.error("Error al ejecutar tarea de negocio para: {}", jobSourceName, failure));
            })
            .schedule();
        
        lastInterval = currentInterval;
        log.info("Orquestador: Job dinámico programado exitosamente con identidad: {}", DYNAMIC_JOB_IDENTITY);
    }
}


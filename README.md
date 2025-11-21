# PoC: Estrategia Híbrida de Scheduler Reactivo

## Descripción

Este proyecto es una Prueba de Concepto (PoC) que implementa una estrategia híbrida de programación de tareas usando Quarkus. La arquitectura combina un job fijo que actúa como orquestador con jobs dinámicos que se crean y programan en tiempo de ejecución basándose en parámetros obtenidos de una fuente externa.

## Arquitectura

### Estrategia Híbrida

La solución implementa un patrón de dos niveles:

1. **Job Orquestador Fijo**: Se ejecuta cada 30 segundos usando `@Scheduled` y consulta parámetros de configuración desde una fuente externa (simulada mediante `MockParameterService`).

2. **Job Dinámico**: Se crea programáticamente usando la API de Quarkus Scheduler basándose en los parámetros obtenidos. Este job ejecuta tareas de negocio que persisten datos en la base de datos.

### Flujo de Ejecución

```
Orquestador (@Scheduled cada 30s)
    ↓
Consulta MockParameterService (reactivo)
    ↓
Obtiene JobParameters (intervalo, actionId)
    ↓
Programa/Reprograma Job Dinámico
    ↓
Job Dinámico se ejecuta según intervalo configurado
    ↓
TaskExecutionService ejecuta tarea de negocio
    ↓
Persiste AuditLog en base de datos (Hibernate Reactive)
```

## Tecnologías Utilizadas

- **Quarkus 3.11.1**: Framework Java reactivo
- **Java 21**: Lenguaje de programación
- **Hibernate Reactive Panache**: Persistencia reactiva
- **PostgreSQL**: Base de datos (configurada para uso con Hibernate Reactive)
- **Mutiny**: Programación reactiva
- **Quarkus Scheduler**: Programación de tareas
- **JUnit 5**: Framework de testing
- **Awaitility**: Esperas asíncronas en tests

## Estructura del Proyecto

```
src/main/java/com/empresa/financiera/
├── application/
│   ├── DynamicSchedulerOrchestrator.java    # Orquestador principal
│   └── service/
│       └── TaskExecutionService.java        # Servicio de ejecución de tareas
├── domain/
│   └── model/
│       ├── AuditLog.java                    # Entidad JPA
│       └── JobParameters.java              # DTO de parámetros
└── infrastructure/
    ├── repository/
    │   └── AuditRepository.java             # Repositorio Panache
    └── service/
        └── MockParameterService.java        # Servicio mock de parámetros
```

## Requisitos Previos

- Java 21 o superior
- Maven 3.8 o superior
- PostgreSQL (o usar Dev Services de Quarkus)

## Configuración

### Base de Datos

El proyecto está configurado para usar PostgreSQL con Hibernate Reactive. La configuración se encuentra en `src/main/resources/application.properties`:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.reactive.max-size=20
quarkus.datasource.reactive.idle-timeout=30M
quarkus.hibernate-orm.database.generation=drop-and-create
```

### Scheduler

El scheduler está habilitado por defecto:

```properties
quarkus.scheduler.enabled=true
```

## Compilación y Ejecución

### Compilar el Proyecto

```bash
mvn clean compile
```

### Ejecutar en Modo Desarrollo

```bash
mvn quarkus:dev
```

La aplicación estará disponible en `http://localhost:8080`.

### Ejecutar Tests

```bash
mvn test
```

### Compilar para Producción

```bash
mvn clean package
```

### Ejecutar JAR

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Componentes Principales

### DynamicSchedulerOrchestrator

Clase principal que implementa la estrategia híbrida:

- **Método `checkAndReschedule()`**: Anotado con `@Scheduled(every = "30s")`, consulta parámetros y programa el job dinámico.
- **Método `scheduleDynamicJob()`**: Crea o reprograma el job dinámico usando la API programática de Quarkus Scheduler.

### TaskExecutionService

Servicio que encapsula la lógica de negocio:

- **Método `executeBusinessTask()`**: Ejecuta una tarea de negocio y persiste un registro de auditoría en la base de datos usando transacciones reactivas (`@WithTransaction`).

### MockParameterService

Servicio que simula la obtención de parámetros desde una fuente externa:

- Retorna un `Uni<JobParameters>` con un intervalo aleatorio (entre 10 y 30 segundos) y un actionId aleatorio.
- Simula latencia de red/base de datos mediante delays reactivos.

### AuditLog

Entidad JPA que representa un registro de auditoría:

- Campos: `id`, `jobName`, `executedAt`
- Tabla: `audit_log`

## Testing

El proyecto incluye tests de integración que validan el flujo completo:

### DynamicJobOrchestratorTest

Test principal que valida:

1. La inyección correcta de dependencias
2. La ejecución del orquestador
3. La programación del job dinámico
4. La ejecución del job y persistencia en base de datos
5. El formato correcto del `jobName` en los registros de auditoría

El test utiliza Awaitility para esperas asíncronas sin usar `Thread.sleep`, siguiendo mejores prácticas.

## Características de la Implementación

### Programación Reactiva

- Todas las operaciones de base de datos son reactivas usando `Uni` de Mutiny
- Las transacciones se manejan con `@WithTransaction` para Hibernate Reactive
- El job dinámico usa `setAsyncTask()` para ejecutar tareas asíncronas

### Manejo de Errores

- Los errores en la obtención de parámetros se registran pero no detienen el orquestador
- Los errores en la ejecución de tareas se registran en los logs
- Las transacciones reactivas manejan errores automáticamente

### Optimización

- El orquestador solo reprograma el job si el intervalo ha cambiado
- Se elimina el job anterior antes de crear uno nuevo con la misma identidad

## Logging

El proyecto utiliza SLF4J con Lombok para logging:

- Nivel general: INFO
- Nivel específico del paquete `com.empresa.financiera`: DEBUG
- Formato estructurado con timestamp, nivel, clase y mensaje

## Desarrollo

### Agregar Nuevas Funcionalidades

1. **Nuevos Jobs Dinámicos**: Modificar `DynamicSchedulerOrchestrator.scheduleDynamicJob()` para crear jobs adicionales.

2. **Nuevas Tareas de Negocio**: Extender `TaskExecutionService` con nuevos métodos que retornen `Uni<Void>`.

3. **Nuevas Entidades**: Crear entidades JPA en `domain.model` y repositorios en `infrastructure.repository`.

### Mejores Prácticas Aplicadas

- Inyección por constructor (no field injection)
- Separación de responsabilidades (Domain, Application, Infrastructure)
- Programación reactiva end-to-end
- Tests de integración que validan el flujo completo
- Uso de constantes para strings repetidos
- Logging estructurado

## Troubleshooting

### El job dinámico no se ejecuta

- Verificar que el scheduler esté habilitado en `application.properties`
- Revisar los logs para ver si hay errores en la obtención de parámetros
- Verificar que el intervalo configurado sea válido (formato ISO-8601, ej: "PT10S")

### Errores de conexión a base de datos

- Verificar que PostgreSQL esté corriendo
- Revisar la configuración del datasource en `application.properties`
- Usar Dev Services de Quarkus para desarrollo (se inicia automáticamente)

### Tests fallan

- Asegurarse de que la base de datos esté disponible
- Verificar que Awaitility esté en el classpath
- Revisar los timeouts en los tests (pueden necesitar ajuste según el entorno)

## Licencia

Este proyecto es una Prueba de Concepto para fines de demostración y aprendizaje.

## Autor

Desarrollado como PoC para validar estrategias híbridas de scheduling con Quarkus.


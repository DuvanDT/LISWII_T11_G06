# GymClass - Domain-Driven Design (DDD) y Arquitectura Hexagonal

Sistema de gestión de reservas de clases grupales para gimnasios desarrollado en **Java 17** y **Spring Boot 3.4.5**, diseñado bajo los principios tácticos de **Domain-Driven Design (DDD)** y **Arquitectura Hexagonal (Ports & Adapters)**.

> **Proyecto Académico – Laboratorio de Ingeniería de Software II**  
> **Institución:** Universidad del Cauca – Facultad de Ingeniería Electrónica y Telecomunicaciones  
> **Periodo:** 2026-2 | **Grupo:** 06  
> **Autores:** Duvan Alexis Hoyos, Juan José Hurtado  
> **Docente:** Pablo Magé  

---

## 📋 Tabla de Contenidos

- [Descripción y Reglas del Negocio](#-descripción-y-reglas-del-negocio)
- [Arquitectura del Proyecto](#-arquitectura-del-proyecto)
- [Lenguaje Ubicuo y Modelo de Dominio](#-lenguaje-ubicuo-y-modelo-de-dominio)
- [Tecnologías Utilizadas](#-tecnologías-utilizadas)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación y Ejecución](#-instalación-y-ejecución)
- [Endpoints de la API REST](#-endpoints-de-la-api-rest)
- [Colección de Postman](#-colección-de-postman)
- [Pruebas Automatizadas](#-pruebas-automatizadas)
- [Diagramas de Arquitectura](#-diagramas-de-arquitectura)

---

## 🎯 Descripción y Reglas del Negocio

**GymClass** administra la programación de sesiones grupales (**Yoga**, **Pilates** y **Spinning**) y las reservas de los socios garantizando que todas las reglas e invariantes residan en el núcleo del dominio, completamente aisladas de los frameworks de infraestructura:

1. **Control de Membresías:** Solo los socios con estado `ACTIVE` pueden realizar reservas.
2. **Restricción por Plan:**
   - `BASIC`: Acceso exclusivo a **Yoga**.
   - `STANDARD`: Acceso a **Yoga** y **Pilates**.
   - `PREMIUM`: Acceso ilimitado (**Yoga**, **Pilates** y **Spinning**).
3. **Capacidad Limitada:** Cada clase tiene un cupo máximo (`capacity`) que no puede ser excedido.
4. **Unicidad de Reserva:** Un socio no puede tener más de una reserva confirmada (`CONFIRMED`) para la misma sesión.
5. **Ventana Temporal de Reserva:** No se permiten reservas una vez que la sesión ha iniciado.
6. **Política de Cancelación:** Una reserva solo puede cancelarse antes de la hora de inicio de la clase, liberando inmediatamente el cupo disponible.

---

## 🏛️ Arquitectura del Proyecto

El código está organizado en cuatro zonas conceptuales siguiendo el patrón **Ports & Adapters**:

```text
src/main/java/com/example/gymclass/
├── domain/                    # Núcleo del Negocio (Cero dependencias de frameworks)
│   ├── model/                 # Entidades y Raíces de Agregado (Member, ClassSession, Booking)
│   ├── valueobject/           # Objetos de Valor inmutables y Enums (Plan, TimeSlot, IDs...)
│   └── service/               # Servicios de Dominio (BookingPolicy)
├── application/               # Casos de Uso y Orquestación
│   ├── port/in/               # Puertos de Entrada / Driving (GymClassInputPort)
│   ├── port/out/              # Puertos de Salida / Driven (MemberRepositoryPort, ClassSessionRepositoryPort)
│   ├── service/               # Orquestador de Casos de Uso (GymClassApplicationService)
│   └── dto/                   # DTOs agnósticos al protocolo de transporte
├── adapter/                   # Adaptadores Tecnológicos (Infraestructura)
│   ├── in/web/                # Adaptador REST (GymClassController, ApiExceptionHandler)
│   └── out/persistence/       # Adaptador JPA y mapeadores (MemberRepositoryAdapter, Entidades JPA)
└── config/                    # Composition Root (HexagonalConfiguration)
```

---

## 📖 Lenguaje Ubicuo y Modelo de Dominio

| Concepto | Tipo DDD | Descripción |
| :--- | :--- | :--- |
| **`Member`** | Aggregate Root | Socio del gimnasio titular de una membresía (`Plan` y `MemberStatus`). |
| **`ClassSession`** | Aggregate Root | Clase grupal programada (`ActivityType`, `TimeSlot`, `capacity`) que custodia la colección de reservas. |
| **`Booking`** | Entity | Reserva individual de un cupo dentro de una `ClassSession` (`CONFIRMED` / `CANCELLED`). |
| **`BookingPolicy`** | Domain Service | Evalúa las reglas de negocio cruzadas entre `Member` y `ClassSession` antes de autorizar una reserva. |
| **`MemberId`, `SessionId`, `BookingId`** | Value Object | Identificadores únicos e inmutables basados en UUID. |
| **`TimeSlot`** | Value Object | Intervalo temporal validado (`startsAt` - `endsAt`). |

---

## 🛠️ Tecnologías Utilizadas

- **Java 17**
- **Spring Boot 3.4.5** (Spring Web, Spring Data JPA, Bean Validation)
- **Base de Datos H2** (en memoria)
- **Maven 3.x**
- **JUnit 5 & Mockito** (Pruebas unitarias y de integración con `MockMvc`)
- **PlantUML & Postman** (Documentación arquitectónica y pruebas de API)

---

## ⚙️ Requisitos Previos

- **JDK 17** o superior instalado y configurado en `JAVA_HOME`.
- **Apache Maven 3.8+** (o uso del wrapper `./mvnw` incluido en el proyecto).

---

## 🚀 Instalación y Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/duvanhoyos/gymclass-ddd-hexagonal.git
   cd gymclass-ddd-hexagonal
   ```

2. **Compilar y ejecutar la aplicación:**
   ```bash
   mvn spring-boot:run
   ```

3. **Accesos locales disponibles:**
   - **API REST Base URL:** `http://localhost:8080/api`
   - **Consola Web H2:** `http://localhost:8080/h2-console`
     - **JDBC URL:** `jdbc:h2:mem:gymclassdb`
     - **User:** `sa`
     - **Password:** *(vacío)*

---

## 🌐 Endpoints de la API REST

| Método | Endpoint | Descripción | Código Éxito |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/members` | Registrar un nuevo socio (`BASIC`, `STANDARD`, `PREMIUM`). | `201 Created` |
| `POST` | `/api/sessions` | Programar una nueva sesión grupal (`YOGA`, `PILATES`, `SPINNING`). | `201 Created` |
| `GET` | `/api/sessions` | Listar todas las sesiones programadas y sus cupos disponibles. | `200 OK` |
| `POST` | `/api/sessions/{sessionId}/bookings` | Reservar un cupo para un socio en una sesión. | `201 Created` |
| `POST` | `/api/sessions/{sessionId}/bookings/{bookingId}/cancel` | Cancelar una reserva existente y liberar el cupo. | `200 OK` |

### Ejemplo de Solicitudes (`curl`)

**1. Registrar socio PREMIUM:**
```bash
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Duván Hoyos", "plan": "PREMIUM"}'
```

**2. Programar sesión de Spinning:**
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "activityType": "SPINNING",
    "startsAt": "2026-12-01T10:00:00Z",
    "endsAt": "2026-12-01T11:00:00Z",
    "capacity": 2
  }'
```

---

## 🧪 Colección de Postman

Se incluye una colección automatizada con pruebas de flujos exitosos y validaciones de reglas de negocio (doble reserva, restricción de plan, cancelación):

- **Ruta:** `lisw2_T11_G06/postman/GymClass-Grupo6.postman_collection.json`

Importa el archivo en Postman y ejecuta la carpeta completa con el *Collection Runner*; las variables `memberId`, `sessionId` y `bookingId` se capturan automáticamente entre peticiones.

---

## ✅ Pruebas Automatizadas

Para ejecutar la suite completa de pruebas unitarias (dominio y aplicación) y pruebas de integración:

```bash
mvn test
```

### Cobertura de Pruebas:
- **`ClassSessionTest` & `DomainRulesTest`:** Pruebas unitarias puras sin contexto de Spring que verifican invariantes de capacidad, unicidad, ventanas de tiempo y políticas de planes.
- **`GymClassApplicationServiceTest`:** Pruebas de orquestación de casos de uso mediante Mocks sobre los puertos de salida.
- **`GymClassApiIntegrationTest`:** Pruebas extremo a extremo levantando Spring Boot, `MockMvc` y la base de datos H2.

---

## 📐 Diagramas de Arquitectura

Los diagramas fuente en PlantUML (`.puml`) y sus exportaciones en PNG se encuentran en `lisw2_T11_G06/diagramas/`:

1. `01-arquitectura-capas.png` – Estructura de paquetes y separación de responsabilidades.
2. `02-modelo-ddd.png` – Diagrama de clases del modelo de dominio (Agregados, Entidades, VOs y Servicios).
3. `03-hexagonal-puertos-adaptadores.png` – Puertos y adaptadores primarios (*driving*) y secundarios (*driven*).
4. `04-secuencia-reservar-clase.png` – Trazabilidad extremo a extremo del caso de uso `bookClass`.
5. `05-persistencia.png` – Separación entre el modelo de dominio y las entidades JPA.
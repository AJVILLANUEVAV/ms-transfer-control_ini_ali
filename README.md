# ms-transfer-control

Microservicio de ejemplo del Banco ABC para controlar solicitudes de transferencia. Expone `POST /api/transfers/control` y valida que el importe sea positivo, que la cuenta de origen exista y esté activa, y que el total diario de transferencias no supere S/ 17,000. Si las validaciones se cumplen, registra la solicitud y devuelve una respuesta `APPROVED`.

> Este proyecto registra y autoriza la solicitud; no realiza débitos ni abonos en cuentas.

## Tecnologías

- Java 17
- Spring Boot 3.2.11 y Spring WebFlux
- Spring Data R2DBC con H2 en memoria
- Maven
- JUnit 5, Mockito y Reactor Test para pruebas

La base de datos se inicializa al arrancar con `src/main/resources/schema.sql` y `data.sql`. Al ser H2 en memoria, los datos se reinician al detener la aplicación.

## Requisitos

- JDK 17 instalado y configurado en `JAVA_HOME`
- Apache Maven 3.6.3 o posterior, disponible en `PATH`

Comprueba las instalaciones con:

```shell
java -version
mvn -version
```

## Compilar y probar

Desde la carpeta raíz del proyecto, ejecuta:

```shell
mvn clean verify
```

Este comando compila el código y ejecuta las pruebas automatizadas. Para compilar sin ejecutar las pruebas:

```shell
mvn clean package -DskipTests
```

El JAR ejecutable se genera en `target/ms-transfer-control-0.0.1-SNAPSHOT.jar`.

## Ejecutar

Con Maven:

```shell
mvn spring-boot:run
```

La aplicación inicia en `http://localhost:8085`.

## Endpoint

`POST /api/transfers/control` — `Content-Type: application/json`

Ejemplo de solicitud:

```json
{
  "customerId": "100",
  "sourceAccount": "001-100001",
  "targetAccount": "001-200001",
  "amount": 250.00
}
```

El proyecto incluye los scripts `mvnw` y `mvnw.cmd`, pero no la configuración del wrapper (`.mvn/wrapper/maven-wrapper.properties`). Por ello, hasta completar esa configuración, utiliza una instalación local de Maven con los comandos anteriores.

# Laboratory work 4

## Architecture

Camunda 7 is not embedded in `delivery.war`. The Camunda WildFly distribution
starts the `default` process engine and the Camunda web applications as
container-managed services. The delivery WAR is a Spring process application
that deploys BPMN and Camunda Forms to that shared engine through
`META-INF/processes.xml`.

The application therefore has three independently deployable runtime parts:

1. Camunda 7.24 and the delivery WAR in WildFly.
2. Apache ActiveMQ with an AMQP 1.0 acceptor on port 5672.
3. An optional delivery worker JAR for JMS consumers and the clustered Quartz
   retry job.

The Bitrix24 JCA resource adapter is deployed to the same WildFly instance as
the delivery WAR.

## Prerequisites

- Java 17 and Maven.
- Camunda BPM WildFly 7.24.0 distribution.
- PostgreSQL databases for application and Camunda engine data.
- A standalone Apache ActiveMQ broker accepting AMQP 1.0 connections.
- PostgreSQL XA datasources `java:/jboss/datasources/DeliveryDS` and
  `java:jboss/datasources/ProcessEngine` in WildFly. Replace the demonstration
  H2 `ProcessEngine` datasource from the Camunda distribution with PostgreSQL;
  XA datasources let Narayana coordinate engine and delivery-data updates.
- The Bitrix24 JCA connection factory `java:/eis/Bitrix24ConnectionFactory`.

## Build

Build the resource adapter first and then the process application:

```shell
mvn -f bitrix24-jca/pom.xml clean package
mvn clean package
```

The relevant artifacts are:

- `bitrix24-jca/target/bitrix24-jca-1.0.0.rar`
- `target/delivery-0.0.1-SNAPSHOT.war`
- `target/delivery-0.0.1-SNAPSHOT-worker.jar`

## WildFly deployment

Set the runtime secrets before starting WildFly:

```shell
export BITRIX24_WEBHOOK_URL='https://your-portal/rest/user/token/'
export DELIVERY_JWT_SECRET='replace-with-a-long-random-secret'
export DELIVERY_AMQP_URL='amqp://active-mq-host:5672'
export DELIVERY_AMQP_USERNAME='admin'
export DELIVERY_AMQP_PASSWORD='replace-me'
export CAMUNDA_WEBAPP_URL='https://your-helios-address/camunda'
```

Configure the RAR with `wildfly/configure-bitrix24-jca.cli`, deploy the delivery
WAR to `standalone/deployments`, and start the Camunda WildFly server with both
application profiles:

```shell
$WILDFLY_HOME/bin/jboss-cli.sh --connect --file=wildfly/configure-bitrix24-jca.cli
$WILDFLY_HOME/bin/standalone.sh -Dspring.profiles.active=helios,api
```

The process application automatically deploys `delivery-order.bpmn`,
`courier-availability.bpmn`, and all referenced `.form` files to the standalone
`default` engine.

## Distributed worker

Run one or more worker nodes separately when distributed execution is needed:

```shell
java -jar target/delivery-0.0.1-SNAPSHOT-worker.jar \
  --spring.profiles.active=worker \
  --spring.datasource.url=jdbc:postgresql://db-host:5432/delivery_db \
  --app.messaging.amqp.url=amqp://active-mq-host:5672
```

The worker consumes courier-assignment and notification queues. When it assigns
a courier, it sends `process.courier-assigned`; the WildFly API node consumes
that message and correlates `courierAssigned` with Camunda. Quartz uses its JDBC
job store, so clustered workers execute each retry trigger once.

## User interface and access

Open the standalone Tasklist at `${CAMUNDA_WEBAPP_URL}/app/tasklist/default/`.
Accounts, passwords and delivery roles are sourced from `users.xml` and
synchronized to Camunda on delivery application startup. Camunda authorizations
and BPMN assignees restrict customer, shop and courier tasks. The REST API keeps
the original Spring Security, JAAS and JWT protection.

The root URL of the delivery WAR redirects to the standalone Tasklist. On a
default local WildFly deployment it is
`http://localhost:8080/delivery-0.0.1-SNAPSHOT/`.

## Requirements mapping

- BPMN 2.0 orchestration: `delivery-order.bpmn` and
  `courier-availability.bpmn`, exported by Camunda Modeler 5.50.1.
- Dynamic process: gateways, user tasks, message events, parallel branches and
  both ten-minute timers are controlled by Camunda.
- Generated UI: every start/user task references a deployment-bound Camunda
  Form (`.form`).
- Transactions: Java delegates call declarative `@Transactional` services;
  WildFly supplies Narayana JTA in the `helios` profile.
- Asynchronous processing: delegates publish through Qpid JMS over AMQP 1.0 to
  Apache ActiveMQ; Spring `@JmsListener` workers consume the queues.
- Periodic processing: clustered JDBC Quartz periodically retries orders that
  are still waiting for a courier. Business deadlines are BPMN timer events.
- EIS integration: order creation and status transitions are sent through the
  Bitrix24 JCA RAR.
- Deployment: the process application is packaged as a WAR for the Camunda
  WildFly distribution on Helios; the engine lifecycle is independent from the
  application WAR.

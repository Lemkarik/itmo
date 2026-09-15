# Laboratory work 4

## Architecture

Camunda 7 runs as a standalone service. It is not embedded in `delivery.war`,
and the delivery application does not use the Camunda process-engine API or the
Camunda database. The runtime consists of separate nodes:

1. Camunda WildFly: process engine, REST API, Tasklist/Cockpit and the delivery
   JAAS identity-provider plugin.
2. Application WildFly: `delivery.war`, PostgreSQL/JTA, the Bitrix24 JCA RAR,
   REST API and Camunda External Task handlers.
3. Apache ActiveMQ: the AMQP 1.0 message broker.
4. One or more optional worker JAR nodes: JMS consumers and clustered Quartz.

The BPMN engine owns gateways, forms, user tasks, both ten-minute deadlines and
the process state. Automatic BPMN service tasks are External Tasks fetched by
the application through Camunda REST. This keeps the process dynamic without
putting the engine inside the business application.

## Build

```shell
mvn -f camunda-jaas-identity/pom.xml clean package
mvn -f bitrix24-jca/pom.xml clean package
mvn clean package
```

Artifacts:

- `camunda-jaas-identity/target/camunda-jaas-identity-1.0.0.jar`
- `bitrix24-jca/target/bitrix24-jca-1.0.0.rar`
- `target/delivery-0.0.1-SNAPSHOT.war`
- `target/delivery-0.0.1-SNAPSHOT-worker.jar`

## Camunda server

Use one Camunda BPM WildFly 7.24 installation only as the process server. Do
not deploy `delivery.war` to it.

Install the JAAS identity-provider plugin while this WildFly is stopped. The
recommended manual procedure uses the WildFly management CLI rather than
modifying `standalone.xml` or the Camunda REST WAR.

First, create the custom module using the CLI's `module add` command:

```powershell
& C:\servers\camunda-wildfly\bin\jboss-cli.bat --commands `
  "module add --name=com.example.delivery.camunda-jaas --resources=$PWD\camunda-jaas-identity\target\camunda-jaas-identity-1.0.0.jar --dependencies=java.base,java.logging,java.xml,org.camunda.bpm.camunda-engine"
```

The Camunda engine plugin module is a static WildFly module rather than a
management-model resource. Add this one dependency to
`modules/org/camunda/bpm/camunda-engine-plugins/main/module.xml` manually,
inside `<dependencies>`:

```xml
<module name="com.example.delivery.camunda-jaas" export="true" />
```

Start Camunda WildFly. Configure the plugin in the running server through the
management CLI (run once; first inspect the existing value so that a duplicate
plugin is not added):

```powershell
& C:\servers\camunda-wildfly\bin\jboss-cli.bat --connect --commands `
  '/subsystem=camunda-bpm-platform/process-engines=default:read-attribute(name=plugins)'

& C:\servers\camunda-wildfly\bin\jboss-cli.bat --connect --commands `
  '/subsystem=camunda-bpm-platform/process-engines=default:list-add(name=plugins,value={"class" => "com.example.delivery.camunda.identity.DeliveryJaasIdentityProviderPlugin"})'
```

Enable HTTP Basic authentication on the bundled Camunda REST application with
a deployment overlay; this leaves the original WAR unchanged:

```powershell
& C:\servers\camunda-wildfly\bin\jboss-cli.bat --connect --commands `
  'deployment-overlay add --name=camunda-rest-auth --content="/WEB-INF/web.xml=C:\Users\me\delivery\wildfly\camunda-rest-web.xml" --deployments=camunda-engine-rest-jakarta-7.24.0.war --redeploy-affected'
```

Replace `C:\Users\me\delivery` with the absolute project path. Finally run
`reload` in `jboss-cli` or restart WildFly. The repository also contains
`wildfly/install-camunda-jaas.ps1` and `.sh` for the automated equivalent.

The plugin creates Camunda users and memberships but delegates password checks
to JAAS and reads from `users.xml`. The REST authentication filter makes the
application service account use the same provider. To use one external XML
file on both servers, set `DELIVERY_USERS_XML` to its absolute path before
starting each server.

Configure Camunda's `ProcessEngine` datasource to use PostgreSQL, then start
Camunda WildFly. Deploy the BPMN diagrams and generated forms over REST:

```powershell
.\wildfly\deploy-camunda-process.ps1 `
  -RestUrl http://localhost:8080/engine-rest `
  -Username admin -Password admin123
```

```shell
./wildfly/deploy-camunda-process.sh \
  http://localhost:8080/engine-rest admin admin123
```

The standard Tasklist login now accepts the accounts from `users.xml`, for
example `admin/admin123`, `alice/password`, `shop/shop123` and
`courier1/courier123`.

## Application server

Use a second, independent WildFly installation for the application. Configure
the PostgreSQL XA datasource `java:/jboss/datasources/DeliveryDS`, deploy and
configure `bitrix24-jca-1.0.0.rar` with
`wildfly/configure-bitrix24-jca.cli`, and deploy only
`delivery-0.0.1-SNAPSHOT.war`.

The datasource is a server resource and is not supplied by the application
WAR. First deploy a PostgreSQL JDBC driver in the application WildFly (replace
the JAR path with the version available on the host):

```text
deploy C:/path/to/postgresql-42.7.7.jar --name=postgresql-42.7.7.jar --force
```

Then add the XA datasource. Replace the host, database name and credentials
with the PostgreSQL values for the environment:

```text
/subsystem=datasources/xa-data-source=DeliveryDS:add(jndi-name="java:/jboss/datasources/DeliveryDS",driver-name=postgresql-42.7.7.jar,xa-datasource-class=org.postgresql.xa.PGXADataSource,user-name=postgres,password=postgres,enabled=true,use-java-context=true)
/subsystem=datasources/xa-data-source=DeliveryDS/xa-datasource-properties=ServerName:add(value=localhost)
/subsystem=datasources/xa-data-source=DeliveryDS/xa-datasource-properties=PortNumber:add(value=5432)
/subsystem=datasources/xa-data-source=DeliveryDS/xa-datasource-properties=DatabaseName:add(value=delivery_db)
/subsystem=datasources/xa-data-source=DeliveryDS:test-connection-in-pool
```

The last command must return `outcome => success` before deploying or
redeploying the application WAR.

Set at least these environment variables:

```shell
export DELIVERY_USERS_XML=/absolute/path/to/users.xml
export DELIVERY_JWT_SECRET=replace-with-a-long-random-secret
export DELIVERY_AMQP_URL=amqp://active-mq-host:5672
export DELIVERY_AMQP_USERNAME=admin
export DELIVERY_AMQP_PASSWORD=replace-me
export BITRIX24_WEBHOOK_URL=https://your-portal/rest/user/token/
export CAMUNDA_WEBAPP_URL=http://camunda-host:8080/camunda
export CAMUNDA_REST_URL=http://camunda-host:8080/engine-rest
export CAMUNDA_SERVICE_USERNAME=admin
export CAMUNDA_SERVICE_PASSWORD=admin123
```

Start the application WildFly with:

```shell
$WILDFLY_HOME/bin/standalone.sh -Dspring.profiles.active=helios,api
```

The application WAR can be replaced or stopped without stopping Camunda; the
process engine and active process instances remain available. Restarted API
nodes resume fetching unlocked External Tasks.

## Distributed worker

```shell
java -jar target/delivery-0.0.1-SNAPSHOT-worker.jar \
  --spring.profiles.active=worker \
  --spring.datasource.url=jdbc:postgresql://db-host:5432/delivery_db \
  --app.messaging.amqp.url=amqp://active-mq-host:5672
```

The worker consumes courier-assignment and notification queues. Assignment
produces `process.courier-assigned`; the API node consumes that queue and
correlates the BPMN message through Camunda REST. Quartz uses the shared JDBC
job store, so clustered workers execute one trigger only once.

## Requirements mapping

- BPMN 2.0 and Modeler: `delivery-order.bpmn` and
  `courier-availability.bpmn` were exported by Camunda Modeler 5.50.1.
- Dynamic BPMS: Camunda owns all routing, state, user tasks, event waits and
  deadlines; application code handles only External Task adapters.
- Standalone engine: the Camunda server and application server communicate over
  REST and have independent processes, deployments and lifecycles.
- Generated UI: every start/user task references a deployment-bound Camunda
  Form (`.form`).
- Access control: Tasklist authenticates through the JAAS/XML engine plugin;
  Camunda groups and BPMN assignments enforce `CUSTOMER`, `SHOP`, `COURIER`
  and `ADMIN`. Application REST keeps Spring Security, JAAS and JWT.
- Transactions: business services retain declarative `@Transactional` JTA;
  WildFly supplies Narayana. Distributed processing/transactions remain on the
  delivery and JMS nodes as allowed by the assignment.
- Asynchronous processing: Qpid JMS sends AMQP 1.0 messages to Apache ActiveMQ;
  Spring `@JmsListener` consumers process queues.
- Periodic processing: clustered JDBC Quartz retries courier searches; BPMN
  timers enforce business deadlines.
- EIS integration: order creation and state changes use the Bitrix24 JCA RAR.
- Helios: both server components are deployable under WildFly; the application
  artifact remains a standard WAR.

# Multi App Example

This example shows two dapr-enabled applications that can be started locally and connect using shared infrastructure. 

Application A uses Dapr workflow to orchestrate application logic, including producing Kafka Messages that will be consumed by Application B. 

Application B consumes Kafka Messages produced by Application A and raise events for workflow instances.

## Running the apps

```
cd producer-app/
../../mvnw -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

```
cd consumer-app/
../../mvnw -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```
# Workflow Patterns with Spring Boot

This repository includes a set of workflow patterns and examples that shows how to use Dapr Workflow with Spring Boot applications. 

The `workflows` application can be started by running the following commands for local development: 

```
cd workflows/
mvn spring-boot:test-run
```

## Patterns

The `workflows` Maven project contains different workflow patterns showing also some integration patterns and Dapr workflow features. 

- Simple HTTP endpoint with Activity Retry Policies (`simplehttp`)
- Async Kafka producer and consumer 


### Simple HTTP with retry policies

This example shows a workflow with a single activity that performs a remote HTTP endpoint call. This activity is configured to retry if the HTTP endpoint call fails. 

Once the application is running, you can invoke the endpoint using `cURL` or `HTTPie`.

```bash
http :8080/simplehttp/start id="123" customer="salaboy" amount=10
```

As soon as the workflow is started, you get a response back containing the `workflowInstanceId` set: 

```
{
    "amount": 10,
    "customer": "salaboy",
    "id": "123",
    "processedByExternalAsyncSystem": false,
    "processedByRemoteHttpService": false,
    "workflowInstanceId": "2ea091b4-5f26-45eb-b414-7a62f6d9816e"
}
```

In the application logs you can see how the activity retries the HTTP call until the remote services returns a `200` response:

```
io.dapr.durabletask                      : Performing retires based on policy
io.dapr.durabletask                      : 1 retries out of total 2 performed
i.d.s.w.s.MakePaymentRequestActivity     : Payment Result: AuditPaymentPayload{paymentRequestId='123', customer='salaboy', amount=10, message='Salaboy's payment'}

...
io.dapr.workflows.WorkflowContext        : Payment request: PaymentRequest{id='123', customer='salaboy', amount=10, processedByRemoteHttpService=true, processedByExternalAsyncSystem=false, workflowInstanceId='null'} sent to payment service.
```
The important bit of information here is the `processedByRemoteHttpService=true` property that is set by the activity that calls the remote HTTP endpoint after obtaining a result from the endpoint.


### Async Kafka producer and consumer

This workflow consist on an activity that produce a Kafka message into a Kafka topic and a WaitForExternal event activity. 

The workflow is complemented by a KafkaListener that consume messages from a topic and then raise a workflow event using the workflowInstanceId to target the right instance. 

Once the application is running, you can invoke the endpoint using `cURL` or `HTTPie`.

```bash
http :8080/asynckafka/start id="123" customer="salaboy" amount=10
```

The applications logs should show something similar to: 

```
io.dapr.workflows.WorkflowContext        : Let's send the payment request to an async external system: 123
i.d.s.w.a.SendPaymentAsyncSystemActivity : Placing a kafka message from Activity: io.dapr.springboot.workflows.asynckafka.SendPaymentAsyncSystemActivity
i.d.s.w.a.AsyncKafkaRestController       : Workflow instance a8eae19e-6bf5-4468-81d3-315b5bb2565e started

...
i.d.s.w.a.ExternalKafkaMessageListener   : Payment request approval requested: 123
io.dapr.workflows.WorkflowContext        : Let's wait for external (async) system to get back to us: 123
...
io.dapr.workflows.WorkflowContext        : Payment was processed and event arrived: 123
```


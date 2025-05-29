# Workflow Patterns with Spring Boot

This repository includes a set of workflow patterns and examples that shows how to use Dapr Workflow with Spring Boot applications. 

The `workflows` application can be started by running the following commands for local development: 

```
cd workflows/
mvn spring-boot:test-run
```

**Note:** If you want to run this examples against Diagrid's Catalyst, you need to comment out the following line in the `application.properties` file located in `src/test/resources/`:

#comment out to run tests against Catalyst
tests.dapr.local=true


## Patterns

The `workflows` Maven project contains different workflow patterns showing also some integration patterns and Dapr workflow features. 

- Simple HTTP endpoint with Activity Retry Policies (`simplehttp`)
- Async Kafka producer and consumer  (`asynckafka`)
- Async PubSub producer and consumer (`asyncpubsub`)
- Simple Timer (`simpletimer`)


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

The workflow is complemented by a KafkaListener that consume messages from a topic and then raise a workflow event using the workflowInstanceId to target the right workflow instance. 

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

### Async PubSub producer and consumer

This workflow consist on an activity that produce a message using the Dapr PubSub API and a WaitForExternal event activity. 

The workflow is complemented by a RestEndpoint that  is subscribed (with a Dapr Subscription) to consume messages from a topic and then raise a workflow event using the workflowInstanceId to continue the right workflow instance. 

Once the application is running, you can invoke the endpoint using `cURL` or `HTTPie`.

```bash
http :8080/asyncpubsub/start id="123" customer="salaboy" amount=10
```
You should see the response back from the server similar to: 

```bash
HTTP/1.1 200 
Connection: keep-alive
Content-Type: application/json
Date: Thu, 29 May 2025 09:11:37 GMT
Keep-Alive: timeout=60
Transfer-Encoding: chunked

{
    "amount": 10,
    "customer": "salaboy",
    "id": "123",
    "processedByExternalAsyncSystem": false,
    "processedByRemoteHttpService": false,
    "updatedAt": [],
    "workflowInstanceId": "31b4a139-a9c3-420d-bb45-6282d1bd1053"
}
```

The applications logs should show something similar to: 

```bash
io.dapr.workflows.WorkflowContext        : Let's send the payment request to an async external system: 123
i.d.s.w.a.AsyncPubSubRestController      : Workflow instance 31b4a139-a9c3-420d-bb45-6282d1bd1053 started
i.d.s.w.a.SendPaymentViaPubSubActivity   : Placing a PubSub message from Activity: io.dapr.springboot.workflows.asyncpubsub.SendPaymentViaPubSubActivity
io.dapr.workflows.WorkflowContext        : Let's wait for external (async) system to get back to us: 123

...


i.d.s.w.a.AsyncPubSubRestController      : Payment request approval requested: 123
io.dapr.workflows.WorkflowContext        : Payment was processed and event arrived: 123

```

### Simple Timer

This workflow shows how a timer can be used to wait for a consistent amount of time before moving to the next workflow activity. 
The workflow consist of three steps: 
- ModifyPaymentRequestActivity
- Timer wait for 10 seconds
- ModifyPaymentRequestActivity

The PaymentRequest payload stores the timestamps when the activities where executed, allowing us to validate that the timer was triggered at the right time.

Once the application is running, you can invoke the endpoint using `cURL` or `HTTPie`.

```bash
http :8080/simpletimer/start id="123" customer="salaboy" amount=10
```

You should see a response back from the server similar to: 
```bash
HTTP/1.1 200 
Connection: keep-alive
Content-Type: application/json
Date: Thu, 29 May 2025 09:14:46 GMT
Keep-Alive: timeout=60
Transfer-Encoding: chunked

{
    "amount": 10,
    "customer": "salaboy",
    "id": "123",
    "processedByExternalAsyncSystem": false,
    "processedByRemoteHttpService": false,
    "updatedAt": [],
    "workflowInstanceId": "eb3c82ec-0ffb-421f-9a01-0297bd562f30"
}
```

```bash
io.dapr.workflows.WorkflowContext        : Let's Update the Payment Request: 123
i.d.s.w.s.TimersRestController           : Workflow instance eb3c82ec-0ffb-421f-9a01-0297bd562f30 started
i.d.s.w.s.UpdatePaymentRequestActivity   : PaymentRequest updated at: [Thu May 29 10:14:46 WEST 2025]
io.dapr.workflows.WorkflowContext        : Payment request: PaymentRequest [id=123, customer=salaboy, amount=10, processedByRemoteHttpService=false, processedByExternalAsyncSystem=false, workflowInstanceId=null, updatedAt=[Thu May 29 10:14:46 WEST 2025]] updated.

...

io.dapr.workflows.WorkflowContext        : Timer completed
io.dapr.workflows.WorkflowContext        : Let's Update the Payment Request: 123
i.d.s.w.s.UpdatePaymentRequestActivity   : PaymentRequest updated at: [Thu May 29 10:14:46 WEST 2025, Thu May 29 10:14:56 WEST 2025]
io.dapr.workflows.WorkflowContext        : Payment request: PaymentRequest [id=123, customer=salaboy, amount=10, processedByRemoteHttpService=false, processedByExternalAsyncSystem=false, workflowInstanceId=null, updatedAt=[Thu May 29 10:14:46 WEST 2025, Thu May 29 10:14:56 WEST 2025]] updated.

```

If you inspect the output, you will see that the second time that the activity is executed happens 10 seconds after the first execution. Check the timestamps: `updatedAt=[Thu May 29 10:14:46 WEST 2025, Thu May 29 10:14:56 WEST 2025]]`
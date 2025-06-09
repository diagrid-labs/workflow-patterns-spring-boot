/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.springboot.workflows.suspendresumetimer;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.workflows.DaprTestContainersConfig;
import io.dapr.springboot.workflows.TestWorkflowsApplication;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/*
 * This test highlights the use of suspend and resume workflow instances.
 * The workflow consist of a wait for an external event and a Log activity. We use the wait for the event to
 * execute the suspend and resume command and assert the workflow instance status, then we emit the event so the
 * workflow instance can finish.
 */
@SpringBootTest(classes = { TestWorkflowsApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "tests.dapr.local=true" }) // this property is used to start dapr for this test
class ResumeSuspendTimerWorkflowTests {

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    paymentRequestsStore.getPaymentRequests().clear();
    paymentRequestsStore.clearPaymentTimes();
  }

  @Test
  void testSuspendResumeTimerWorkflows() throws InterruptedException, IOException {

    PaymentRequest paymentRequest = new PaymentRequest("123", "salaboy", 10);

    PaymentRequest paymentRequestResult = given().contentType(ContentType.JSON)
        .body(paymentRequest)
        .when()
        .post("/suspendresume/timer/start")
        .then()
        .statusCode(200).extract().as(PaymentRequest.class);

    // Check that I have an instance id
    assertFalse(paymentRequestResult.getWorkflowInstanceId().isEmpty());

    // One activity was executed, hence the time of the payment must be logged
    await().atMost(Duration.ofSeconds(3))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> paymentRequestsStore.getPaymentTimes(paymentRequest.getId()).size() == 1);


    //Let's suspend the running instance
    given().contentType(ContentType.JSON)
            .param("instanceId", paymentRequestResult.getWorkflowInstanceId())
            .when()
            .patch("/suspendresume/suspend")
            .then()
            .statusCode(200);


    // wait for the workflow to be SUSPENDED
    await().atMost(Duration.ofSeconds(3))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              WorkflowInstanceStatus instanceState = daprWorkflowClient
                      .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
              if(instanceState == null){
                return false;
              }
              return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.SUSPENDED);

            });

    System.out.println("Let's wait for 20 seconds to validate the timer is not firing. Starting at: " + new Date());
    // Let's wait for 20 seconds, to make sure that the timer is not active
    for(int i = 0 ; i < 20; i++){
      System.out.println("Waiting...");
      Thread.sleep(1000);
    }

    System.out.println("Finishing wait at: " + new Date());

    //Let's resume the running instance
    given().contentType(ContentType.JSON)
            .param("instanceId", paymentRequestResult.getWorkflowInstanceId())
            .when()
            .patch("/suspendresume/resume")
            .then()
            .statusCode(200);


    // Two activity were executed, hence the time of the payment must be logged twice
    await().atMost(Duration.ofSeconds(3))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              for(Date d : paymentRequestsStore.getPaymentTimes(paymentRequest.getId())){
                System.out.println("Date: " +  d);
              }
              return paymentRequestsStore.getPaymentTimes(paymentRequest.getId()).size() == 2;
            });


    // wait for the workflow to be running
    await().atMost(Duration.ofSeconds(3))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              WorkflowInstanceStatus instanceState = daprWorkflowClient
                      .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
              if(instanceState == null){
                return false;
              }
              return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.RUNNING);

            });

    // The time between the first activity and the second (right after the timer due) should be the waiting time
    // plus 1 or 2 seconds

    List<Date> paymentTimes = paymentRequestsStore.getPaymentTimes(paymentRequest.getId());
    assertEquals(2, paymentTimes.size());

    long diffInMillies = Math.abs(paymentTimes.get(1).getTime() - paymentTimes.get(0).getTime());
    long diff = TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    System.out.println("Diff between first and second activity: " + diff);
    assertTrue(diff > 20 && diff <= 22);

    //Let's continue the workflow by sending the event that is waiting for
    given().contentType(ContentType.JSON)
            .body(paymentRequest)
            .when()
            .post("/suspendresume/continue")
            .then()
            .statusCode(200);


    // Three activities were executed, hence the time of the payment must be logged thrice
    await().atMost(Duration.ofSeconds(3))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              for(Date d : paymentRequestsStore.getPaymentTimes(paymentRequest.getId())){
                System.out.println("Date: " +  d);
              }
              return paymentRequestsStore.getPaymentTimes(paymentRequest.getId()).size() == 3;
            });

    // wait for the workflow to be running
    await().atMost(Duration.ofSeconds(10))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              WorkflowInstanceStatus instanceState = daprWorkflowClient
                      .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
              if(instanceState == null){
                return false;
              }
              return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED);

            });

  }


}

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
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    System.out.println("Let's wait for 20 seconds to validate the timer is not firing.");
    // Let's wait for 20 seconds, to make sure that the timer is not active
    for(int i = 0 ; i < 20; i++){
      System.out.println("Waiting...");
      Thread.sleep(1000);
    }

    //Let's resume the running instance
    given().contentType(ContentType.JSON)
            .param("instanceId", paymentRequestResult.getWorkflowInstanceId())
            .when()
            .patch("/suspendresume/resume")
            .then()
            .statusCode(200);


//
//    // wait for the workflow to be running
//    await().atMost(Duration.ofSeconds(3))
//            .pollDelay(500, TimeUnit.MILLISECONDS)
//            .pollInterval(500, TimeUnit.MILLISECONDS)
//            .until(() -> {
//              WorkflowInstanceStatus instanceState = daprWorkflowClient
//                      .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
//              if(instanceState == null){
//                return false;
//              }
//              return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.RUNNING);
//
//            });
//
//    //Let's continue the workflow by sending the event that is waiting for
//    given().contentType(ContentType.JSON)
//            .body(paymentRequest)
//            .when()
//            .post("/suspendresume/continue")
//            .then()
//            .statusCode(200);
//
//
//    // Check that we have received a payment request
//    await().atMost(Duration.ofSeconds(5))
//        .pollDelay(500, TimeUnit.MILLISECONDS)
//        .pollInterval(500, TimeUnit.MILLISECONDS)
//        .until(() -> paymentRequestsStore.getPaymentRequests().size() == 1);

    // wait for the workflow to be running
    await().atMost(Duration.ofSeconds(30))
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

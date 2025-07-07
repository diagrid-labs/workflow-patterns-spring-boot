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

package io.dapr.springboot.workflows.raisemultievent;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.workflows.DaprTestContainersConfig;
import io.dapr.springboot.workflows.TestWorkflowsApplication;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.springboot.workflows.service.PaymentWorkflowsStore;
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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/*
 * This test highlights
 */
@SpringBootTest(classes = { TestWorkflowsApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "tests.dapr.local=true" }) // this property is used to start dapr for this test
class RaiseMultiEventsWorkflowTests {

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    paymentRequestsStore.getLoggedPayments().clear();
  }

  @Test
  void testRaiseMultiEventsWorkflows() throws InterruptedException, IOException {


    PaymentRequest paymentRequest = new PaymentRequest("0", "salaboy", 0);
    PaymentRequest paymentRequestResult = given().contentType(ContentType.JSON)
        .body(paymentRequest)
        .when()
        .post("/raisemultievent/start")
        .then()
        .statusCode(200).extract().as(PaymentRequest.class);

    // Check that I have an instance id
    assertFalse(paymentRequestResult.getWorkflowInstanceId().isEmpty());

    for( int i = 1; i < 101; i ++) {
      paymentRequest.setAmount(i);
      given().contentType(ContentType.JSON)
              .body(paymentRequest)
              .when()
              .post("/raisemultievent/continue-2")
              .then()
              .statusCode(200);
    }


    // Check that the workflow is still running
    await().atMost(Duration.ofSeconds(10))
        .pollDelay(500, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() -> {
          WorkflowInstanceStatus instanceState = daprWorkflowClient
              .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
          if (instanceState == null) {
            return false;
          }
          return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.RUNNING);
        });

    paymentRequest.setAmount(0);
    given().contentType(ContentType.JSON)
            .body(paymentRequest)
            .when()
            .post("/raisemultievent/continue-1")
            .then()
            .statusCode(200);

    // If an event was raised before it is consumed, it will be consumed when the workflow wait for Event2
    // Notice that it will pick up the first one that was consumed.

    await().atMost(Duration.ofSeconds(10))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              WorkflowInstanceStatus instanceState = daprWorkflowClient
                      .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
              if (instanceState == null) {
                return false;
              }
              return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED);
            });


    assertEquals(2, paymentRequestsStore.getLoggedPayments().size());
    // The first PaymentRequest has a 0 amount as is the payload sent for EVENT1
    assertEquals(0, paymentRequestsStore.getLoggedPayments().get(0).getAmount());
    // The second PaymentRequest has a 1 amount as is the payload sent for the first EVENT2
    assertEquals(1, paymentRequestsStore.getLoggedPayments().get(1).getAmount());

  }


}

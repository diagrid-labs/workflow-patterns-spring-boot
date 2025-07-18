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

package io.dapr.springboot.workflows.terminate;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.workflows.DaprTestContainersConfig;
import io.dapr.springboot.workflows.TestWorkflowsApplication;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.springboot.workflows.service.PaymentWorkflowsStore;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
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
 * This test highlights remote HTTP invocation calling with configured retries for the workflow activity.
 * It uses a Microcks Ensemble to simulate a remote HTTP endpoint that creates one failure (returning a 500)
 * with a subsequent success (200), testing the retry mechanism.
 */
@SpringBootTest(classes = { TestWorkflowsApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "tests.dapr.local=true" }) // this property is used to start dapr for this test
class TerminateWorkflowTests {

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private PaymentWorkflowsStore paymentWorkflowsStore;

  @Autowired
  private MicrocksContainersEnsemble ensemble;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    paymentRequestsStore.getPaymentRequests().clear();
  }

  @Test
  void testTerminateWorkflow() throws InterruptedException, IOException {


    PaymentRequest paymentRequest = new PaymentRequest("123", "salaboy", 10);
    PaymentRequest paymentRequestResult = given().contentType(ContentType.JSON)
        .body(paymentRequest)
        .when()
        .post("/terminate/start")
        .then()
        .statusCode(200).extract().as(PaymentRequest.class);

    // Check that I have an instance id
    assertFalse(paymentRequestResult.getWorkflowInstanceId().isEmpty());

    await().atMost(Duration.ofSeconds(3))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              WorkflowInstanceStatus instanceState = daprWorkflowClient
                      .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), false);
              if (instanceState == null) {
                return false;
              }
              return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.RUNNING);
            });

     given().contentType(ContentType.JSON)
            .body(paymentRequest)
            .when()
            .delete("/terminate/")
            .then()
            .statusCode(200);

    // Check that the workflow completed successfully
    await().atMost(Duration.ofSeconds(5))
        .pollDelay(500, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() -> {
          WorkflowInstanceStatus instanceState = daprWorkflowClient
              .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
          if (instanceState == null) {
            return false;
          }
          if(!instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.TERMINATED)){
            return false;
          };
          PaymentRequest paymentRequestOutput = instanceState.readOutputAs(PaymentRequest.class);

          return paymentRequestOutput.getTerminated();
        });
  }

}

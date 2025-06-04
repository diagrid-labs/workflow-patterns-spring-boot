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

package io.dapr.springboot.workflows.asynctasks;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.workflows.DaprTestContainersConfig;
import io.dapr.springboot.workflows.TestWorkflowsApplication;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.AccountService;
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
 * This test highlights
 */
@SpringBootTest(classes = { TestWorkflowsApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "tests.dapr.local=true" }) // this property is used to start dapr for this test
class AsyncTasksWorkflowTests {

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private PaymentWorkflowsStore paymentWorkflowsStore;

  @Autowired
  private MicrocksContainersEnsemble ensemble;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private AccountService accountService;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    paymentRequestsStore.getPaymentRequests().clear();
    accountService.resetBalances();
  }

  @Test
  void testAsyncTasksWorkflows() throws InterruptedException, IOException {
    Long callsToRemoteServiceBeforeStart = ensemble.getMicrocksContainer()
            .getServiceInvocationsCount("API Payment Validator", "1.0.0");

    accountService.credit("salaboy", 100);

    PaymentRequest paymentRequest = new PaymentRequest("123", "salaboy", 10);
    PaymentRequest paymentRequestResult = given().contentType(ContentType.JSON)
        .body(paymentRequest)
        .when()
        .post("/asynctasks/start")
        .then()
        .statusCode(200).extract().as(PaymentRequest.class);

    // Check that I have an instance id
    assertFalse(paymentRequestResult.getWorkflowInstanceId().isEmpty());

    // Check that we have received a payment request
    await().atMost(Duration.ofSeconds(10))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              return paymentRequestsStore.getPaymentRequests().size() == 2;
            });

    // Checking that retry mechanism is hitting the endpoint twice one for a 500 and
    // then a 200
    await().atMost(Duration.ofSeconds(20))
        .pollDelay(500, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() ->  {
          return (ensemble.getMicrocksContainer()
                  .getServiceInvocationsCount("API Payment Validator", "1.0.0") - callsToRemoteServiceBeforeStart) == 2;
        } );

    // Customer should have a balance of 80 after two debits
    await().atMost(Duration.ofSeconds(5))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> accountService.getCustomerBalance("salaboy") == 80);

    // Check that the workflow completed successfully
    await().atMost(Duration.ofSeconds(10))
        .pollDelay(500, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() -> {
          WorkflowInstanceStatus instanceState = daprWorkflowClient
              .getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
          if (instanceState == null) {
            return false;
          }
          if (!instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED)) {
            return false;
          }
          PaymentRequest paymentRequestResultFromWorkflow = instanceState.readOutputAs(PaymentRequest.class);
          if (paymentRequestResultFromWorkflow == null) {
            return false;
          }
          return paymentRequestResultFromWorkflow.getProcessedByRemoteHttpService();
        });
  }

}

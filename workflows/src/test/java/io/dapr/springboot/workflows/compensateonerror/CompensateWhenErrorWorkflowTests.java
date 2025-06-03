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

package io.dapr.springboot.workflows.compensateonerror;

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
import static org.junit.jupiter.api.Assertions.*;

/*
 * This test highlights
 */
@SpringBootTest(classes = { TestWorkflowsApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "tests.dapr.local=true" }) // this property is used to start dapr for this test
class CompensateWhenErrorWorkflowTests {

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
  }

  @Test
  void testCompensateOnErrorWorkflows() throws InterruptedException, IOException {

    accountService.credit("other", 100);
    accountService.credit("salaboy", 100);

    PaymentRequest paymentRequest = new PaymentRequest("123", "other", 10);
    PaymentRequest paymentRequestResult = given().contentType(ContentType.JSON)
        .body(paymentRequest)
        .when()
        .post("/compensateonerror/start")
        .then()
        .statusCode(200).extract().as(PaymentRequest.class);

    // Check that I have an instance id
    assertFalse(paymentRequestResult.getWorkflowInstanceId().isEmpty());


    // Checking that the debit and credit endpoints where it as part of the compensation
    await().atMost(Duration.ofSeconds(5))
        .pollDelay(500, TimeUnit.MILLISECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(() ->  ensemble.getMicrocksContainer()
              .getServiceInvocationsCount("API Payment Validator", "1.0.0") == 2);

    // Both customer have the same balance as they started with, after compensations
    await().atMost(Duration.ofSeconds(5))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> accountService.getCustomerBalance("salaboy") == 100 && accountService.getCustomerBalance("other") == 100);


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
          return instanceState.getRuntimeStatus().equals(WorkflowRuntimeStatus.COMPLETED);
        });

  }

}

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

package io.dapr.springboot.workflows.asyncpubsub;

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
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * This test highlights
 */
@SpringBootTest(classes = {TestWorkflowsApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"tests.dapr.local=true"}) //this property is used to start dapr for this test
class AsyncPubSubWorkflowTests {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private PaymentWorkflowsStore paymentWorkflowsStore;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }

  @Test
  void testAsyncPubSubWorkflows() throws InterruptedException, IOException {

    PaymentRequest paymentRequest = new PaymentRequest("123", "salaboy", 10);
    PaymentRequest paymentRequestResult = given().contentType(ContentType.JSON)
            .body(paymentRequest)
            .when()
            .post("/asyncpubsub/start")
            .then()
            .statusCode(200).extract().as(PaymentRequest.class);

    // Check that I have an instance id
    assertFalse(paymentRequestResult.getWorkflowInstanceId().isEmpty());

    // Check that we have received a payment request
    assertEquals(1, paymentRequestsStore.getPaymentRequests().size());


    await().atMost(Duration.ofSeconds(5))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              System.out.println(paymentRequestsStore.getPaymentRequest(paymentRequest.getId()).getProcessedByExternalAsyncSystem());
              return paymentRequestsStore.getPaymentRequest(paymentRequest.getId()).getProcessedByExternalAsyncSystem();
            });


    WorkflowInstanceStatus instanceState = daprWorkflowClient.getInstanceState(paymentRequestResult.getWorkflowInstanceId(), true);
    assertNotNull(instanceState);        
    PaymentRequest paymentRequestResultFromWorkflow = instanceState.readOutputAs(PaymentRequest.class);
    assertNotNull(paymentRequestResultFromWorkflow);
    assertEquals(WorkflowRuntimeStatus.COMPLETED, instanceState.getRuntimeStatus());

    assertTrue(paymentRequestResultFromWorkflow.getProcessedByExternalAsyncSystem());

  }

}

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

package io.dapr.springboot.workflows;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.workflows.asynckafka.ExternalKafkaMessageListener;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.springboot.workflows.workflow.PaymentProcessingWorkflow;
import io.dapr.springboot.workflows.asynckafka.SendPaymentAsyncSystemActivity;
import io.dapr.springboot.workflows.workflow.StorePaymentRequestActivity;
import io.dapr.testcontainers.DaprContainer;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TestPaymentsServiceApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class, PaymentsServiceRestController.class, PaymentProcessingWorkflow.class,
        SendPaymentAsyncSystemActivity.class, StorePaymentRequestActivity.class, ExternalKafkaMessageListener.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"REMOTE_KAFKA_TOPIC = topic", "tests.dapr.local=true"})
class PaymentServiceAppTests {


  @Autowired
  private PaymentRequestsStore paymentRequestsStore;


  @Autowired
  private DaprContainer daprContainer;

  @Autowired
  private MicrocksContainersEnsemble ensemble;


  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);

  }


  @Test
  void testPaymentProcessingWorkflows() throws InterruptedException, IOException {

    PaymentRequest paymentRequest = given().contentType(ContentType.JSON)
            .body(new PaymentRequest("123", "salaboy", 10 ))
            .when()
            .post("/pay")
            .then()
            .statusCode(200).extract().as(PaymentRequest.class);


    await().atMost(Duration.ofSeconds(5))
            .until(paymentRequestsStore.getPaymentRequests()::size, equalTo(1));


    await().atMost(Duration.ofSeconds(20))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              System.out.println(paymentRequestsStore.getPaymentRequest(paymentRequest.getId()));
              return paymentRequestsStore.getPaymentRequest(paymentRequest.getId()).getProcessedByExternalAsyncSystem();
            });

    // Checking that retry mechanism is hitting the endpoint twice one for a 500 and then a 200
    assertEquals(2, ensemble.getMicrocksContainer()
            .getServiceInvocationsCount("API Payment Validator", "1.0.0"));

  }

}

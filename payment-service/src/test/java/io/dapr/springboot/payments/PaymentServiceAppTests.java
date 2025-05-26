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

package io.dapr.springboot.payments;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.payments.model.PaymentRequest;
import io.dapr.springboot.payments.service.PaymentRequestsStore;
import io.dapr.springboot.payments.workflow.PaymentProcessingWorkflow;
import io.dapr.springboot.payments.workflow.SendPaymentAsyncSystemActivity;
import io.dapr.springboot.payments.workflow.StorePaymentRequestActivity;
import io.dapr.testcontainers.DaprContainer;
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


  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);


  }


  @Test
  void testPaymentProcessingWorkflows() throws InterruptedException, IOException {

    PaymentRequest paymentRequest = given().contentType(ContentType.JSON)
            .body(new PaymentRequest("salaboy", 10 ))
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


  }

}

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

package io.dapr.springboot.payments.workflow;


import io.dapr.springboot.payments.model.PaymentRequest;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 This activity shows how to place a Kafka Message to a Broker
 */
@Component
public class SendPaymentAsyncSystemActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(SendPaymentAsyncSystemActivity.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;


  public SendPaymentAsyncSystemActivity(KafkaTemplate<String, Object> kafkaTemplate ) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Value("${REMOTE_KAFKA_TOPIC:}")
  private String kafkaTopic;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

    kafkaTemplate.send(kafkaTopic, paymentRequest);

//    // Local registry of payment requests
//    paymentRequestsStore.savePaymentRequest(paymentRequest);
//
//    // Adding delay to simulate a remote interaction
//    try {
//      Thread.sleep(5000);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }
//
//    if(remoteServiceUrl != null && !remoteServiceUrl.isEmpty()) {
//      // Define the shape of the request for the remote service
//      HttpEntity<AuditPaymentPayload> request =
//              new HttpEntity<>(new AuditPaymentPayload(paymentRequest.getId(),
//                      paymentRequest.getCustomer(), paymentRequest.getAmount(), "Payment accepted for processing"));
//      String orderUpdateString =
//              restTemplate.postForObject(remoteServiceUrl + "/audit-payment-request", request, String.class);
//      logger.info("Update Order result: " + orderUpdateString);
//    }
//    logger.info("PaymentRequest: " + paymentRequest.getId() + " stored for auditing.");
    return paymentRequest;
  }


}

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

package io.dapr.springboot.workflows.asynckafka;


import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
 This activity shows how to place a Kafka Message to a Broker
 */
@Component
public class SendPaymentViaKafkActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(SendPaymentViaKafkActivity.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  public SendPaymentViaKafkActivity(KafkaTemplate<String, Object> kafkaTemplate ) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Value("${REMOTE_KAFKA_TOPIC:topic}")
  private String kafkaTopic;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);
    paymentRequestsStore.savePaymentRequest(paymentRequest);
    logger.info("Placing a kafka message from Activity: " + ctx.getName());
    kafkaTemplate.send(kafkaTopic, paymentRequest);

    return paymentRequest;
  }


}

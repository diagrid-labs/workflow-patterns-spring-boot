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

package io.dapr.springboot.examples.producer.workflow;



import io.dapr.springboot.examples.Payload;
import io.dapr.springboot.examples.producer.PayloadStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProduceMessageActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(ProduceMessageActivity.class);
  private final PayloadStore payloadStore;

  @Value("${REMOTE_KAFKA_TOPIC:topic}")
  private String kafkaTopic;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public ProduceMessageActivity(PayloadStore payloadStore,
                                KafkaTemplate<String, Object> kafkaTemplate) {
    this.payloadStore = payloadStore;
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    Payload payload = ctx.getInput(Payload.class);

    logger.info("Payload: {} accepted.", payload);

    Payload modifiedPayload = payloadStore.getPayloadById(payload.getId());
    modifiedPayload.setSentToKafka(true);
    payloadStore.addPayload(modifiedPayload);

    // Sent message to Kafka
    kafkaTemplate.send(kafkaTopic, payload);

    logger.info("Payload: {} sent to Kafka.", payload);

    return payload;
  }


}

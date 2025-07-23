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

package io.dapr.springboot.examples.consumer;

import io.dapr.springboot.examples.Payload;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SubscriberRestController {

  private final Logger logger = LoggerFactory.getLogger(SubscriberRestController.class);

  private List<Payload> payloads = new ArrayList<>();

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;


  /**
   *  Wait for async kafka message
   *  @param payload associated with a workflow instance
   *  @return confirmation that the follow-up was requested
   */
  @KafkaListener(topics = "${REMOTE_KAFKA_TOPIC:topic}", groupId = "workflows")
  public String paymentRequestApproval(Payload payload) {
    logger.info("Payload modification requested: " + payload.getId());
    payload.setContent("MODIFIED PAYLOAD");
    payloads.add(payload);
    daprWorkflowClient.raiseEvent(payload.getWorkflowId(), "PayloadModified", payload);
    return "Payment processed by async service";

  }

  @GetMapping("events")
  public List<Payload> getAllPayloads() {
    return payloads;
  }

}


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

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.payments.model.PaymentRequest;
import io.dapr.springboot.payments.service.PaymentRequestsStore;
import io.dapr.springboot.payments.workflow.PaymentProcessingWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableDaprWorkflows
public class PaymentsServiceRestController {

  private final Logger logger = LoggerFactory.getLogger(PaymentsServiceRestController.class);

  @Value("${PUBLIC_IP:localhost:8080}")
  private String publicIp;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  //@TODO: this is an in-memory correlation between payment requests and workflow instances
  private Map<String, String> paymentsWorkflows = new HashMap<>();

  //@TODO: as this is an example, we store incoming requests in-memory
  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  /**
   * Track customer endpoint.
   *
   * @param paymentRequest provided customer to track
   * @return confirmation that the workflow instance was created for a given customer
   */
  @PostMapping("/pay")
  public PaymentRequest placeOrder(@RequestBody PaymentRequest paymentRequest) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(PaymentProcessingWorkflow.class, paymentRequest);
    logger.info("Workflow instance " + instanceId + " started");
    paymentRequest.setWorkflowId(instanceId);
    paymentsWorkflows.put(paymentRequest.getId(), instanceId);
    return paymentRequest;
  }



  @GetMapping("/orders")
  public Collection<PaymentRequest> getPaymentRequests() {
    return paymentRequestsStore.getPaymentRequests();
  }

  public record PaymentEvent(PaymentRequest order) {
  }

  @GetMapping("/server-info")
  public Info getInfo(){
    return new Info(publicIp);
  }

  public record Info(String publicIp){}

  public Map<String, String> getPaymentsWorkflows() {
    return paymentsWorkflows;
  }
}


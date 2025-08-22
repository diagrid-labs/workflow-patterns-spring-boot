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

package io.dapr.springboot.workflows.multiretry;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.ActivityTrackerService;
import io.dapr.springboot.workflows.service.RetryLogService;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableDaprWorkflows
public class MultiRetryRestController {

  private final Logger logger = LoggerFactory.getLogger(MultiRetryRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private RetryLogService retryLogService;

  @Autowired
  private ActivityTrackerService activityTrackerService;

  private String instanceId;

  /**
   * Multi retry Payment workflow
   *
   * @param paymentRequest to be sent to a remote http service
   * @return workflow instance id created for the payment
   */
  @PostMapping("/multiretry/start")
  public PaymentRequest placePaymentRequest(@RequestBody PaymentRequest paymentRequest) {
    retryLogService.resetRetryCounter();
    activityTrackerService.clearExecutedActivities();

    instanceId = daprWorkflowClient.scheduleNewWorkflow(MultiRetryWorkflow.class, paymentRequest);
    paymentRequest.setWorkflowInstanceId(instanceId);
    return paymentRequest;
  }

  @PostMapping("/multiretry/event")
  public String event(@RequestBody String content) {
    logger.info("Event received with content {}.", content);
    daprWorkflowClient.raiseEvent(instanceId, "EVENT", content);
    return "Event processed";
  }

}


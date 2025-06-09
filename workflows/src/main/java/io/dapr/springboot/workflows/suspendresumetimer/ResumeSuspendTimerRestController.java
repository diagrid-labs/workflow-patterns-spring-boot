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

package io.dapr.springboot.workflows.suspendresumetimer;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.springboot.workflows.service.PaymentWorkflowsStore;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@EnableDaprWorkflows
public class ResumeSuspendTimerRestController {

  private final Logger logger = LoggerFactory.getLogger(ResumeSuspendTimerRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private PaymentWorkflowsStore paymentsWorkflowsStore;


  //@TODO: as this is an example, we store incoming requests in-memory
  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  /**
   * Simple Payment workflow
   *
   * @param paymentRequest to be sent to a remote http service
   * @return workflow instance id created for the payment
   */
  @PostMapping("/suspendresume/timer/start")
  public PaymentRequest placePaymentRequest(@RequestBody PaymentRequest paymentRequest) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ResumeSuspendTimerWorkflow.class, paymentRequest);
    paymentRequest.setWorkflowInstanceId(instanceId);
    paymentsWorkflowsStore.savePaymentWorkflow(paymentRequest, instanceId);
    return paymentRequest;
  }

  @PostMapping("/suspendresume/timer/continue")
  public PaymentRequest continuePayment(@RequestBody PaymentRequest paymentRequest) {
    logger.info("Payment request continue requested: {}", paymentRequest.getId());
    String workflowIdForPayment = paymentsWorkflowsStore.getPaymentWorkflowInstanceId(paymentRequest.getId());

    if (workflowIdForPayment == null || workflowIdForPayment.isEmpty()) {
      return null;
    } else {
      paymentRequestsStore.savePaymentRequest(paymentRequest);
      daprWorkflowClient.raiseEvent(workflowIdForPayment, "Continue", paymentRequest);
      return paymentRequest;
    }
  }


  @PatchMapping("/suspendresume/timer/suspend")
  public void suspendWorkflowInstance(@RequestParam("instanceId") String instanceId) {
    logger.info("Suspending Workflow Instance: {}, at {}", instanceId, new Date());
    daprWorkflowClient.suspendWorkflow(instanceId, "");
  }

  @PatchMapping("/suspendresume/timer/resume")
  public void resumeWorkflowInstance(@RequestParam("instanceId") String instanceId) {
    logger.info("Resuming Workflow Instance: {}, at {}", instanceId, new Date());
    daprWorkflowClient.resumeWorkflow(instanceId, "");
  }


}


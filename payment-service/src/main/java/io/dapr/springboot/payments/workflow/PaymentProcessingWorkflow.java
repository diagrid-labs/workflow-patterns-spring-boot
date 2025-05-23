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
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessingWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);
      paymentRequest.setWorkflowId(instanceId);
      // Audit incoming Payment Request
      ctx.getLogger().info("Let's store the payment request: " + paymentRequest.getId()
              + " for customer: " + paymentRequest.getCustomer());
      ctx.callActivity(StorePaymentRequestActivity.class.getName(), paymentRequest, PaymentRequest.class).await();

      ctx.getLogger().info("Payment request: " + paymentRequest.getId()
              + " sent to audit service.");

      ctx.complete(paymentRequest);
    };
  }
}



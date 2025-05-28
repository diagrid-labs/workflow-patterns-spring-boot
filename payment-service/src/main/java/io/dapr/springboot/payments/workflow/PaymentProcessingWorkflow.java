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
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PaymentProcessingWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);
      // Audit incoming Payment Request
      ctx.getLogger().info("Let's store the payment request: " + paymentRequest.getId()
              + " for customer: " + paymentRequest.getCustomer());
      ctx.callActivity(StorePaymentRequestActivity.class.getName(), paymentRequest, new WorkflowTaskOptions(new WorkflowTaskRetryPolicy(5,
                      Duration.ofSeconds(2), 1.0, Duration.ofSeconds(10), Duration.ofSeconds(20))),
              PaymentRequest.class).await();

      ctx.getLogger().info("Payment request: " + paymentRequest.getId()
              + " sent to audit service.");

      ctx.getLogger().info("Let's send the payment request to an async external system: " + paymentRequest.getId());
      ctx.callActivity(SendPaymentAsyncSystemActivity.class.getName(), paymentRequest, PaymentRequest.class).await();

      // Wait for the external system to get back to us
      ctx.getLogger().info("Let's wait for external system to get back to us: " + paymentRequest.getId());
      paymentRequest = ctx.waitForExternalEvent("ExternalProcessingDone", Duration.ofMinutes(5), PaymentRequest.class).await();

      ctx.getLogger().info("Payment was approved and event arrived: " + paymentRequest.getId());

      ctx.getLogger().info("Let's update the payment state: " + paymentRequest.getId());
      paymentRequest = ctx.callActivity(UpdatePaymentRequestActivity.class.getName(), paymentRequest, PaymentRequest.class).await();
      ctx.getLogger().info("Payment state updated: " + paymentRequest);

      ctx.complete(paymentRequest);
    };
  }
}



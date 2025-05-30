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

package io.dapr.springboot.workflows.simpletimer;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class TimersWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance " + instanceId + " started");
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);
      ctx.getLogger().info("Let's Update the Payment Request: " + paymentRequest.getId());
      paymentRequest = ctx.callActivity(UpdatePaymentRequestActivity.class.getName(), paymentRequest,
        PaymentRequest.class).await();
  
      ctx.getLogger().info("Payment request: " + paymentRequest
              + " updated.");

      ctx.createTimer(Duration.ofSeconds(10)).await();

      ctx.getLogger().info("Timer completed");

      ctx.getLogger().info("Let's Update the Payment Request: " + paymentRequest.getId());
      paymentRequest = ctx.callActivity(UpdatePaymentRequestActivity.class.getName(), paymentRequest,
        PaymentRequest.class).await();
  
      ctx.getLogger().info("Payment request: " + paymentRequest
              + " updated.");


      ctx.complete(paymentRequest);
    };
  }
}



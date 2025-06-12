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

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.suspendresume.LogPaymentActivity;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

@Component
public class ResumeSuspendTimerWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance {} started", instanceId);
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      ctx.getLogger().info("Let's call the Log activity for payment: {}", paymentRequest.getId());
      paymentRequest = ctx.callActivity(LogPaymentActivity.class.getName(), paymentRequest, PaymentRequest.class).await();

      ctx.getLogger().info("Starting the timer at: {}", new Date());
      ctx.createTimer(Duration.ofSeconds(20)).await();
      ctx.getLogger().info("Finishing the timer at: {}", new Date());

      ctx.getLogger().info("Let's call the Log activity for payment: {}", paymentRequest.getId());
      paymentRequest = ctx.callActivity(LogPaymentActivity.class.getName(), paymentRequest, PaymentRequest.class).await();

      ctx.getLogger().info("Let's wait for external (async) system to get back to us: {}", paymentRequest.getId());
      ctx.waitForExternalEvent("Continue", PaymentRequest.class).await();

      ctx.getLogger().info("Let's call the Log activity for payment: {}", paymentRequest.getId());
      paymentRequest = ctx.callActivity(LogPaymentActivity.class.getName(), paymentRequest, PaymentRequest.class).await();

      ctx.getLogger().info("Workflow completed for: {}", paymentRequest.getId());
      ctx.complete(paymentRequest);
    };
  }
}



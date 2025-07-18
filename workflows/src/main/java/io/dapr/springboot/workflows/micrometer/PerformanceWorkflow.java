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

package io.dapr.springboot.workflows.micrometer;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PerformanceWorkflow implements Workflow {


  private final MeterRegistry registry;
  private Timer.Sample workflowTimerSample = null;
  private Timer.Sample storeActivityTimerSample = null;
  private Timer.Sample slowActivityTimerSample = null;

  public PerformanceWorkflow(MeterRegistry registry) {
    this.registry = registry;

  }

  @Override
  public WorkflowStub create() {
    return ctx -> {

      if (!ctx.isReplaying()) {
        workflowTimerSample = Timer.start(registry);
      }

      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance {} started", instanceId);
      final PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      PaymentRequest paymentRequestResult = null;

      if (!ctx.isReplaying()) {
        storeActivityTimerSample = Timer.start(registry);
      }
      paymentRequestResult =
              ctx.callActivity(StorePaymentRequestActivity.class.getName(),
                              paymentRequest, PaymentRequest.class).await();

      if (!ctx.isReplaying()) {
        storeActivityTimerSample.stop(registry.timer("callActivity1.workflow", "workflow", "callActivity"));
      }
      ctx.getLogger().info("Payment request stored.");

      ctx.getLogger().info("Calling Slow Activity.");


      if (!ctx.isReplaying()) {
        slowActivityTimerSample = Timer.start(registry);
      }
      paymentRequestResult =
              ctx.callActivity(SlowActivity.class.getName(), paymentRequest, PaymentRequest.class)
                      .await();
      if (!ctx.isReplaying()) {
        slowActivityTimerSample.stop(registry.timer("callActivity2.workflow", "workflow", "callActivity"));
      }
      ctx.getLogger().info("Slow Activity done.");

      ctx.complete(paymentRequest);
      ctx.getLogger().info("Workflow Completed for Payment: {}.", paymentRequestResult);
      if (!ctx.isReplaying()) {
        workflowTimerSample.stop(registry.timer("total.workflow", "workflow", "end-to-end"));
      }
    };
  }
}



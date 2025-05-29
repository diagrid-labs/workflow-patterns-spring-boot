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

package io.dapr.springboot.workflows.asyncpubsub;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AsyncPusSubWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      ctx.getLogger().info("Let's send the payment request to an async external system: " + paymentRequest.getId());
      ctx.callActivity(SendPaymentViaPubSubActivity.class.getName(), paymentRequest, PaymentRequest.class).await();

      // Wait for the external system to get back to us
      ctx.getLogger().info("Let's wait for external (async) system to get back to us: " + paymentRequest.getId());
      paymentRequest = ctx.waitForExternalEvent("ExternalPubSubProcessingDone", Duration.ofMinutes(5), PaymentRequest.class).await();

      ctx.getLogger().info("Payment was processed and event arrived: " + paymentRequest.getId());

      ctx.complete(paymentRequest);
    };
  }
}



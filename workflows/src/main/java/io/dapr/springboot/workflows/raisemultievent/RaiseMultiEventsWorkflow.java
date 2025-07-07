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

package io.dapr.springboot.workflows.raisemultievent;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RaiseMultiEventsWorkflow implements Workflow {

  @Autowired
  private PaymentRequestsStore store;

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance {} started", instanceId);

      // Wait for the external system to get back to us
      ctx.getLogger().info("Let's wait for external event: EVENT1");
      PaymentRequest pr =  ctx.waitForExternalEvent("EVENT1", Duration.ofMinutes(5), PaymentRequest.class).await();
      store.logPayment(pr);
      ctx.getLogger().info("EVENT1 received!{}", pr);
      ctx.getLogger().info("Let's wait for external event: EVENT2 ");
      PaymentRequest pr2 = ctx.waitForExternalEvent("EVENT2", Duration.ofMinutes(5), PaymentRequest.class).await();
      store.logPayment(pr2);
      ctx.getLogger().info("EVENT2 received!{}", pr2);
      ctx.complete(null);
    };
  }
}



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

package io.dapr.springboot.examples.producer.workflow;


import io.dapr.springboot.examples.Payload;
import io.dapr.springboot.examples.producer.PayloadStore;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PayloadProcessingWorkflow implements Workflow {


  private PayloadStore payloadStore;

  public PayloadProcessingWorkflow(PayloadStore payloadStore) {
    this.payloadStore = payloadStore;
  }

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      Payload payload = ctx.getInput(Payload.class);
      payload.setWorkflowId(instanceId);

      if (!ctx.isReplaying()) {
        payloadStore.addPayload(payload);
      }

      ctx.getLogger().info("Let's produce a message: {}", payload);
      ctx.callActivity(ProduceMessageActivity.class.getName(), payload, Payload.class).await();
      ctx.getLogger().info("Let's wait for the payload: {} to be modified by app-b.", payload);
      Payload modifiedPayload = ctx.waitForExternalEvent("PayloadModified", Duration.ofMinutes(5), Payload.class).await();
      ctx.getLogger().info("Congratulations the payload was modified: {}.", payload);
      ctx.complete(payload);
    };
  }
}

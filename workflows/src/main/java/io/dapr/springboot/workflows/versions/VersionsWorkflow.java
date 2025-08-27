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

package io.dapr.springboot.workflows.versions;

import io.dapr.durabletask.TaskCanceledException;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.multiretry.CompensationActivity;
import io.dapr.springboot.workflows.multiretry.FirstActivity;
import io.dapr.springboot.workflows.multiretry.NextActivity;
import io.dapr.springboot.workflows.multiretry.RetryActivity;
import io.dapr.springboot.workflows.service.RetryLogService;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class VersionsWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance {} started", instanceId);
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);


      ctx.getLogger().info("Let's call the first activity for payment {}.", paymentRequest.getId());
      paymentRequest = ctx.callActivity(FirstActivity.class.getName(), paymentRequest,
              PaymentRequest.class).await();

      ctx.getLogger().info("First Activity for payment: {} completed.", paymentRequest.getId());

      String eventContent = "";
      try {
        ctx.getLogger().info("Wait for event, for 60 seconds.");
        eventContent = ctx.waitForExternalEvent("EVENT", Duration.ofSeconds(60), String.class).await();
        ctx.getLogger().info("Event arrived with content: {}", eventContent);
      } catch (TaskCanceledException tce) {
        ctx.getLogger().info("Wait for event timed out. ");
      }

      if (!eventContent.isEmpty()) {
        ctx.getLogger().info("We got the event. Let's execute the Next Activity.");
        paymentRequest = ctx.callActivity(NextActivity.class.getName(), paymentRequest,
                PaymentRequest.class).await();
        ctx.getLogger().info("Next activity executed successfully. ");
      } else{
        ctx.getLogger().info("We didn't got the event, hence we are not executing anything else.");
      }

      ctx.complete(paymentRequest);
      ctx.getLogger().info("Workflow {} Completed. ", paymentRequest.getId());
    };
  }
}



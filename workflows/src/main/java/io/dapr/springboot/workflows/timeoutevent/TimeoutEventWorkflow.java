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

package io.dapr.springboot.workflows.timeoutevent;

import io.dapr.durabletask.Task;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class TimeoutEventWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance " + instanceId + " started");
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      //Wait for an event that will never arrive, but we will handle the timeout
      try{  
        ctx.getLogger().info("Let's wait for external (async) system to get back to us: " + paymentRequest.getId());
        ctx.waitForExternalEvent("Continue", 
                                  Duration.ofSeconds(3), 
                                  PaymentRequest.class).await();
      } catch (io.dapr.durabletask.TaskCanceledException e) {
        ctx.getLogger().info("Timeout occurred for payment: " + paymentRequest.getId() + " let's handle it!");
        paymentRequest = ctx.callActivity(HandleTimeoutActivity.class.getName(), paymentRequest, PaymentRequest.class).await();
      }
       
      ctx.getLogger().info("Workflow completed for: " + paymentRequest.getId());
      ctx.complete(paymentRequest);
    };
  }
}



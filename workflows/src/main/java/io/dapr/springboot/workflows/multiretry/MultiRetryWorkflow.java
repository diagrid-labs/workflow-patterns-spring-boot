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

package io.dapr.springboot.workflows.multiretry;

import io.dapr.durabletask.TaskCanceledException;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.RetryLogService;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MultiRetryWorkflow implements Workflow {

  @Autowired
  private RetryLogService retryLogService;



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

      for(int i=0; i < 10; i++) {
        try {
          ctx.getLogger().info("Wait for event, for 2 seconds, iteration: {}.", i);
          eventContent = ctx.waitForExternalEvent("EVENT", Duration.ofSeconds(2), String.class).await();
          ctx.getLogger().info("Event arrived with content: {}", eventContent);
          //We got the event, so we can break the for loop.
          break;
        } catch (TaskCanceledException tce) {
          if(!ctx.isReplaying()) {
            retryLogService.incrementRetryCounter();
          }
          ctx.getLogger().info("Wait for event timed out. ");
          ctx.getLogger().info("Let's execute the Retry Activity. Retry: {}" , retryLogService.getRetryCounter());

          paymentRequest = ctx.callActivity(RetryActivity.class.getName(), paymentRequest,
                  PaymentRequest.class).await();
          ctx.getLogger().info("Retry Activity executed successfully. ");
        }
      }

      if(eventContent.isEmpty()){
        ctx.getLogger().info("Retries exhausted after {} retries. ", retryLogService.getRetryCounter());
        ctx.getLogger().info("Let's execute the Compensation Activity. ");
        paymentRequest = ctx.callActivity(CompensationActivity.class.getName(), paymentRequest,
                PaymentRequest.class).await();
        ctx.getLogger().info("Compensation Activity executed successfully. ");
      }else{
        ctx.getLogger().info("We got the event after {} retries, let's execute the Next Activity. ", retryLogService.getRetryCounter());
        paymentRequest = ctx.callActivity(NextActivity.class.getName(), paymentRequest,
                PaymentRequest.class).await();
        ctx.getLogger().info("Next activity executed successfully. ");
      }

      ctx.complete(paymentRequest);
      ctx.getLogger().info("Workflow {} Completed. ", paymentRequest.getId());
    };
  }
}



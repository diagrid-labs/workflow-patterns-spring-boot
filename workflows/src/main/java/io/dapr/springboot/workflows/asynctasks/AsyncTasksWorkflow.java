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

package io.dapr.springboot.workflows.asynctasks;

import io.dapr.durabletask.Task;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/*
 * This workflow schedule all async tasks (there is no await()).
 * All the tasks create promises that are chained together until the process ends.
 * All the tasks are processed in separate threads managed by the [Reactor Framework](https://projectreactor.io/).
 */

@Component
public class AsyncTasksWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance " + instanceId + " started");
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      ctx.getLogger().info("Let's make a payment: " + paymentRequest.getId()
              + " for customer: " + paymentRequest.getCustomer());
      Task<PaymentRequest> task1 = ctx.callActivity(TaskWithAsyncLogicActivity.class.getName(), paymentRequest,
              PaymentRequest.class);

      task1.thenApply((p) -> {
          // I need to generate a new payment request with a different Id for the second invocation
          PaymentRequest secondPaymentRequest = new PaymentRequest("456", paymentRequest.getCustomer(), paymentRequest.getAmount());
          ctx.getLogger().info("Let's make a payment: " + secondPaymentRequest.getId()
                + " for customer: " + secondPaymentRequest.getCustomer());
          return ctx.callActivity(TaskWithAsyncLogicActivity.class.getName(), secondPaymentRequest, PaymentRequest.class);
      }).thenAccept((t) -> {
        ctx.complete(t.await());
        ctx.getLogger().info("Completing the workflow async, when Activity 2 has finished ");
      });

    };
  }
}



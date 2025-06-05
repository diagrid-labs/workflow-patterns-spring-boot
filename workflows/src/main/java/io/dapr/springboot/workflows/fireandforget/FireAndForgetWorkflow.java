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

package io.dapr.springboot.workflows.fireandforget;

import io.dapr.durabletask.Task;
import io.dapr.springboot.workflows.model.Notification;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

/*
 * This workflow schedule all async tasks (there is no await()).
 * All the tasks create promises that are chained together until the process ends.
 * All the tasks are processed in separate threads managed by the [Reactor Framework](https://projectreactor.io/).
 */

@Component
public class FireAndForgetWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance " + instanceId + " started");
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      ctx.getLogger().info("Let's make a payment: " + paymentRequest.getId()
              + " for customer: " + paymentRequest.getCustomer());
      PaymentRequest resultFromExternalSystem = ctx.callActivity(TaskWithSyncLogicActivity.class.getName(), paymentRequest,
              PaymentRequest.class).await();

      ctx.getLogger().info("Now that the payment was processed, let's notify the Customer Success Team");
      Notification notificationToCustomerSuccess = new Notification(resultFromExternalSystem, "Customer Payment completed successfully.");
      ctx.callActivity(TaskWithLongDelayActivity.class.getName(), notificationToCustomerSuccess);

      ctx.getLogger().info("Now that the payment was processed, let's notify the audit team");
      Notification notificationToAudit = new Notification(resultFromExternalSystem, "Customer Payment completed and ready for audit.");
      ctx.callActivity(TaskWithLongDelayActivity.class.getName(), notificationToAudit);

      ctx.complete(resultFromExternalSystem);
      ctx.getLogger().info("Completing the workflow for payment request: " + resultFromExternalSystem);
    };
  }
}



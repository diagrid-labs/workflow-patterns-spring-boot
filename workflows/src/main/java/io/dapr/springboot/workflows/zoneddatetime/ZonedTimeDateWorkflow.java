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

package io.dapr.springboot.workflows.zoneddatetime;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.simpletimer.UpdatePaymentRequestActivity;
import io.dapr.springboot.workflows.suspendresume.LogPaymentActivity;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Date;

@Component
public class ZonedTimeDateWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      ctx.getLogger().info("Workflow instance " + instanceId + " started");
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      ctx.getLogger().info("Let's call the Log activity for payment: {}", paymentRequest.getId());
      paymentRequest = ctx.callActivity(LogPaymentActivity.class.getName(), paymentRequest,
        PaymentRequest.class).await();

      //ZonedDateTime
      //Creating fixed dates
      //Example: ZonedDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.of(3, 0, 0), ZoneId.of("PST"))
      ZonedDateTime now = ZonedDateTime.now();
      //Let's create a ZonedDateTime 10 seconds in the future
      ZonedDateTime inTheFuture = now.plusSeconds(10);
      ctx.getLogger().info("Creating a timer that due at: {}", inTheFuture);
      ctx.createTimer(inTheFuture).await();
      ctx.getLogger().info("The timer fired at: {}", ZonedDateTime.now());

      ctx.getLogger().info("Let's call the Log activity for payment: {}", paymentRequest.getId());
      paymentRequest = ctx.callActivity(LogPaymentActivity.class.getName(), paymentRequest,
        PaymentRequest.class).await();

      ctx.complete(paymentRequest);
      ctx.getLogger().info("Completing the workflow for: {}", paymentRequest.getId());
    };
  }
}



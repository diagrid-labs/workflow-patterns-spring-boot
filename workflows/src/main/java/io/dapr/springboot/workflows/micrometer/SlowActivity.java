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
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 This activity shows how to interact with an external HTTP endpoint from an activity.
 */
@Component
public class SlowActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(SlowActivity.class);

  private final Timer activityTimer;

  public SlowActivity( MeterRegistry registry) {

    this.activityTimer = Timer.builder("activity.slow-activity")
            .description("Time for SlowActivity activity execution")
            .tags("workflow", "SlowActivity")
            .register(registry);
  }


  @Override
  public Object run(WorkflowActivityContext ctx) {
    return activityTimer.record(() -> {
      PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

      // Adding intentional 100 ms
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      return paymentRequest;
    });

  }


}

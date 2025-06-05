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


import io.dapr.springboot.workflows.model.Notification;
import io.dapr.springboot.workflows.model.PaymentPayload;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.AccountService;
import io.dapr.springboot.workflows.service.NotificationService;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/*
 This activity introduces a 5 seconds delay before sending a notification.
 */
@Component
public class TaskWithLongDelayActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(TaskWithLongDelayActivity.class);

  @Autowired
  private NotificationService notificationService;

  public TaskWithLongDelayActivity() {

  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    Notification notification = ctx.getInput(Notification.class);

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    notificationService.sendNotification(notification);

    return null;
  }


}

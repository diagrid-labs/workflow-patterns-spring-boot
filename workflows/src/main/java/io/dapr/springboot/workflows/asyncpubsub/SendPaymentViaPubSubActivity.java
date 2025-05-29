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

package io.dapr.springboot.workflows.asyncpubsub;


import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
 This activity shows how to place a message via the Dapr PubSub API to a Broker
 */
@Component
public class SendPaymentViaPubSubActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(SendPaymentViaPubSubActivity.class);

  private final DaprMessagingTemplate<PaymentRequest> pubsubTemplate;

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  public SendPaymentViaPubSubActivity(DaprMessagingTemplate<PaymentRequest> pubsubTemplate) {
    this.pubsubTemplate = pubsubTemplate;
  }


  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

    paymentRequestsStore.savePaymentRequest(paymentRequest);
    logger.info("Placing a PubSub message from Activity: " + ctx.getName());
    pubsubTemplate.send("pubsubTopic", paymentRequest);

    return paymentRequest;
  }


}

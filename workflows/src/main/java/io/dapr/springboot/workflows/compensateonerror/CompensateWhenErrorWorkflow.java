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

package io.dapr.springboot.workflows.compensateonerror;

import io.dapr.durabletask.TaskFailedException;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CompensateWhenErrorWorkflow implements Workflow {

        @Autowired
        private CompensationHelper workflowCompensationService;

        @Override
        public WorkflowStub create() {
                return ctx -> {
                        String instanceId = ctx.getInstanceId();
                        ctx.getLogger().info("Workflow instance " + instanceId + " started");
                        PaymentRequest debitRequest = ctx.getInput(PaymentRequest.class);

                        try {
                                ctx.getLogger().info("Let's debit a payment: " + debitRequest.getId()
                                        + " for customer: " + debitRequest.getCustomer());
                                workflowCompensationService.addCompensation( "debit1", () ->
                                        ctx.callActivity(CompensationCreditPaymentRequestActivity.class.getName(), debitRequest,
                                                        PaymentRequest.class));
                                ctx.getLogger().info("Debit request: " + debitRequest
                                        + " sent to payment service.");
                                ctx.callActivity(DebitPaymentRequestActivity.class.getName(), debitRequest,
                                                                PaymentRequest.class).await();

                                ctx.getLogger().info("Debit request: " + debitRequest
                                        + " sent to payment service.");


                                final PaymentRequest debitRequest2 = new PaymentRequest("456", "salaboy", 20);
                                ctx.getLogger().info("Let's debit a payment: " + debitRequest.getId()
                                        + " for customer: " + debitRequest.getCustomer());

                                workflowCompensationService.addCompensation("debit2", () ->
                                        ctx.callActivity(CompensationCreditPaymentRequestActivity.class.getName(), debitRequest2,
                                                                PaymentRequest.class));
                                ctx.callActivity(DebitPaymentRequestActivity.class.getName(),
                                                                debitRequest2,
                                                                PaymentRequest.class).await();

                        } catch (TaskFailedException e) {
                                ctx.getLogger().info("Task failed: " + e.getMessage() + " - compensating.");
                                workflowCompensationService.compensate();
                                ctx.complete("Some workflow activities were compensated.");
                        }


                };
        }
}

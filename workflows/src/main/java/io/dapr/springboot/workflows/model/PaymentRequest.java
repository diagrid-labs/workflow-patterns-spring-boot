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

package io.dapr.springboot.workflows.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PaymentRequest {

  private String id;
  private String customer;
  private Integer amount;
  private Boolean processedByRemoteHttpService = false;
  private Boolean processedByExternalAsyncSystem = false;
  private String workflowInstanceId;
  private List<Date> updatedAt  = new ArrayList<>();

  public PaymentRequest() {

  }

  public PaymentRequest(String id, String customer, Integer amount) {
    this.id = id;
    this.customer = customer;
    this.amount = amount;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCustomer() {
    return customer;
  }

  public void setCustomer(String customer) {
    this.customer = customer;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public Boolean getProcessedByExternalAsyncSystem() {
    return processedByExternalAsyncSystem;
  }

  public void setProcessedByExternalAsyncSystem(Boolean processedByExternalAsyncSystem) {
    this.processedByExternalAsyncSystem = processedByExternalAsyncSystem;
  }

  public String getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public void setWorkflowInstanceId(String workflowInstanceId) {
    this.workflowInstanceId = workflowInstanceId;
  }

  public Boolean getProcessedByRemoteHttpService() {
    return processedByRemoteHttpService;
  }

  public void setProcessedByRemoteHttpService(Boolean processedByRemoteHttpService) {
    this.processedByRemoteHttpService = processedByRemoteHttpService;
  }

  public List<Date> getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(List<Date> updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void addUpdatedAt(Date updatedAt) {
    this.updatedAt.add(updatedAt);
  }

  @Override
  public String toString() {
    return "PaymentRequest [id=" + id + ", customer=" + customer + ", amount=" + amount
        + ", processedByRemoteHttpService=" + processedByRemoteHttpService + ", processedByExternalAsyncSystem="
        + processedByExternalAsyncSystem + ", workflowInstanceId=" + workflowInstanceId + ", updatedAt=" + updatedAt
        + "]";
  }

  

  

  
}

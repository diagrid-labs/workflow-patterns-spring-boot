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

package io.dapr.springboot.examples.producer;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.examples.Payload;
import io.dapr.springboot.examples.producer.workflow.PayloadProcessingWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableDaprWorkflows
public class WorkflowRestController {


  private final Logger logger = LoggerFactory.getLogger(WorkflowRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;


  @GetMapping("/")
  public String root() {
    return "OK";
  }


  /**
   * Start payload processing workflow endpoint.
   */
  @PostMapping("/start")
  public String startWorkflow(@RequestBody Payload payload) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(PayloadProcessingWorkflow.class, payload);
    logger.info("Workflow instance {} started", instanceId);
    return "New Workflow Instance created for Payload: " + payload;
  }

  /**
   * Payload modified request.
   */
  @PostMapping("/payload/modified")
  public String payloadModifiedRequest(@RequestBody Payload payload) {
    logger.info("Payload modified requested: {}", payload);
    daprWorkflowClient.raiseEvent(payload.getWorkflowId(), "PayloadModified", payload);
    return "Payload modified accepted";
  }


}


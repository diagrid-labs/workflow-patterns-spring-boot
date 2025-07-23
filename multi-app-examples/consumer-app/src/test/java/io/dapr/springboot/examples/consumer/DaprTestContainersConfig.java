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

package io.dapr.springboot.examples.consumer;

import com.redis.testcontainers.RedisContainer;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

  @Bean
  public Network getDaprNetwork(Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    System.out.println("REUSE NETWORK: " + reuse);
    if (reuse) {
      Network defaultDaprNetwork = new Network() {
        @Override
        public String getId() {
          return "dapr-network";
        }

        @Override
        public void close() {

        }

        @Override
        public Statement apply(Statement base, Description description) {
          return null;
        }
      };

      List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
          .withNameFilter("dapr-network").exec();
      if (networks.isEmpty()) {
        Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
        return defaultDaprNetwork;
      } else {
        return defaultDaprNetwork;
      }
    } else {
      return Network.newNetwork();
    }
  }

  @Bean
  @ServiceConnection
  org.testcontainers.kafka.KafkaContainer kafkaContainer(Environment env, Network network) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    System.out.println("REUSE KAFKA: " + reuse);
    return new KafkaContainer(DockerImageName.parse("apache/kafka-native"))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withReuse(reuse);
    //.withListener(() -> "kafka:19092");
  }

  @Bean
  @ServiceConnection
  RedisContainer redisContainer(Environment env, Network network) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    System.out.println("REUSE REDIS: " + reuse);
    RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:latest"))
            .withNetwork(network)
            .withExposedPorts(6379)
            .withNetworkAliases("redis")
            .withReuse(reuse);
    return redisContainer;
  }

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork, RedisContainer redisContainer, Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    System.out.println("REUSE DAPR CP: " + reuse);
    Map<String, String> redisProperties = new HashMap<>();
    redisProperties.put("redisHost", "redis:6379");
    redisProperties.put("redisPassword", "");
    redisProperties.put("actorStateStore", "true");

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("consumer-app")
        .withNetwork(daprNetwork)
            .withComponent(new Component("kvstore", "state.redis", "v1",
                    redisProperties))
        .withDaprLogLevel(DaprLogLevel.INFO)
        .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
        .withAppPort(8081)
            .withAppChannelAddress("host.testcontainers.internal")
        .withReusablePlacement(reuse)
            .withReuseScheduler(reuse)
        .withAppHealthCheckPath("/actuator/health")
            .dependsOn(redisContainer);
  }

}

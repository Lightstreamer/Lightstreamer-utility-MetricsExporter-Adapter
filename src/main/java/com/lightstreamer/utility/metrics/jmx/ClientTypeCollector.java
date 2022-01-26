/*
 *  Copyright (c) Lightstreamer Srl
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.lightstreamer.utility.metrics.jmx;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;

public class ClientTypeCollector extends Collector {

  final Gauge counter;

  private final Resource resource;

  public ClientTypeCollector(MBeanConnection serverConnection) {
    this.counter = Gauge.build()
      .namespace(JMXMetricsCollector.NAME_SPACE)
      .name("Resource_ClientType")
      .help("Number of different client types current connected")
      .labelNames("type", "version")
      .create();

    resource = serverConnection.getProxy("com.lightstreamer:type=Resource", Resource.class);
  }

  @Override
  public List<MetricFamilySamples> collect() {
    Map<String, Long> clientVersions = resource.getCurrClientVersions(null);
    clientVersions.entrySet()
      .stream()
      .forEach(this::updateCounter);

    return counter.collect();
  }

  private void updateCounter(Entry<String, Long> entry) {
    String[] tokens = entry.getKey()
      .split(" ");

    Long amount = entry.getValue();
    if (amount == null) {
      return;
    }

    if (tokens.length < 2) {
      counter.labels("Other", "N/A")
        .set(amount);
      return;
    }

    String clientType = tokens[0];
    String version = tokens[1];

    counter.labels(clientType, version)
      .set(amount);
  }

}

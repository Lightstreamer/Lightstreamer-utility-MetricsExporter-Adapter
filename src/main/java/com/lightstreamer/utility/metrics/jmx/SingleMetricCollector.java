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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.utility.metrics.jmx.JMXMetricsCollector.Gauges;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Gauge;

class SingleMetricCollector {

  private final static Logger log = LogManager.getLogger("export_jmx_metrics");

  /**
   * 
   */
  private final MBeanConnection serverConnection;

  private final ObjectName objectName;

  private final List<MBeanAttributeInfo> attributesList;

  private final String[] labelNames;

  private final String[] labelValues;

  private final Gauges gauges;

  SingleMetricCollector(MBeanConnection serverConnection, ObjectName objectName, Gauges gauges) {
    log.debug("New Collector for {}", objectName);
    this.serverConnection = serverConnection;
    this.objectName = objectName;
    this.gauges = gauges;
    this.attributesList = serverConnection.getAttributeInfos(objectName)
      .collect(Collectors.toList());

    Supplier<Stream<Entry<String, String>>> propertiesFilter = buildFilter(objectName);
    this.labelNames = getLabelNames(propertiesFilter);
    this.labelValues = getLabelValues(propertiesFilter);

    createGauges();
  }

  private Supplier<Stream<Entry<String, String>>> buildFilter(ObjectName object) {
    return () -> object.getKeyPropertyList()
      .entrySet()
      .stream()
      .sorted((a, b) -> a.getKey()
        .compareTo(b.getKey()))
      .filter(e -> !e.getKey()
        .equals("type"));
  }

  private String[] getLabelNames(Supplier<Stream<Entry<String, String>>> filter) {
    return getProperties(filter, Map.Entry::getKey);
  }

  private String[] getLabelValues(Supplier<Stream<Entry<String, String>>> filter) {
    return getProperties(filter, Map.Entry::getValue);
  }

  private String[] getProperties(Supplier<Stream<Entry<String, String>>> filter,
      Function<? super Entry<String, String>, ? extends String> mapper) {

    return filter.get()
      .map(mapper)
      .collect(Collectors.toList())
      .toArray(new String[0]);
  }

  private void createGauges() {
    for (MBeanAttributeInfo attributeInfo : attributesList) {
      String name = attributeInfo.getName();
      String type = objectName.getKeyProperty("type");

      gauges.addGauge(objectName, name, key -> {
        log.debug("Created gauge for attribute [labelNames:{}, name:{}, type:{}], total <{}>",
            labelNames, name, type, gauges.size());

        return Gauge.build()
          .namespace(JMXMetricsCollector.NAME_SPACE)
          .name(type + "_" + name)
          .labelNames(labelNames)
          .help(attributeInfo.getDescription())
          .create();
      });

    }
  }

  public Stream<MetricFamilySamples> collect() {
    return attributesList.stream()
      .map(MBeanAttributeInfo::getName)
      .flatMap(this::streamFromAttribute);
  }

  private Stream<MetricFamilySamples> streamFromAttribute(String attributeName) {
    Object attributeValue = serverConnection.getAttributeValue(objectName, attributeName);
    return Optional.ofNullable(attributeValue)
      .stream()
      .flatMap(value -> collect(value, attributeName));
  }

  private Stream<? extends MetricFamilySamples> collect(Object value, String attributeName) {
    Gauge gauge = gauges.getGauge(objectName, attributeName);
    gauge.labels(labelValues)
      .set(Double.valueOf(value.toString()));
    return gauge.collect()
      .stream();
  }

  public double get(String attributeName) {
    Gauge gauge = gauges.getGauge(objectName, attributeName);
    if (gauge != null) {
      return gauge.labels(labelValues)
        .get();
    }
    return -100;
  }

  protected ObjectName getObjectName() {
    return objectName;
  }
}

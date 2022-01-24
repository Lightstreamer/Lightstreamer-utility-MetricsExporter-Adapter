package com.lightstreamer.utility.metrics;

import com.lightstreamer.utility.metrics.jmx.JMXMetricsCollector;

public class Metrics {

  private static class InstanceHolder {

    private static final JMXMetricsCollector jmxExporter = new JMXMetricsCollector();

  }

  private Metrics() {}

  public static JMXMetricsCollector getJMXExporter() {
    return InstanceHolder.jmxExporter;
  }

}

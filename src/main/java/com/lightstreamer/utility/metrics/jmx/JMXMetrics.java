package com.lightstreamer.utility.metrics.jmx;

import javax.management.ObjectName;

public interface JMXMetrics {
  
  double getJMXValue(ObjectName objectName, String attributeName);

}

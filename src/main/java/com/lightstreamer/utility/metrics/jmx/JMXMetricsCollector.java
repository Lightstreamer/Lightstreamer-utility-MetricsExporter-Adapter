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

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeNotFoundException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.hotspot.DefaultExports;

class SimpleThreadFactory implements ThreadFactory {
  public Thread newThread(Runnable r) {
    return new Thread(r);
  }
}

enum MBeanFilter {

  STANDARD_FITLER {
    public Predicate<MBeanAttributeInfo> propertyFilter() {
  //@formatter:off
      return isNumber()
          .and(
              not(isAnyOf("Max","New","Avg")).
              or(is("NewTerminatedSessions")));
    }

    public Predicate<ObjectInstance> mbeanFilter() {
      final List<String> types = Arrays.asList(
          "Resource",
          "Load",
          "Server",
          "Stream",
          "AdapterSet",
          "DataAdapter",
          "ThreadPool",
          "Timer"
       );
      //@formatter:on

      final Predicate<Hashtable<String, String>> isMonitor = ht -> {
        return "MONITOR".equals(ht.get("AdapterSetName"));
      };

      final Predicate<Hashtable<String, String>> isOfSelectedType = ht -> {
        return types.contains(ht.get("type"));
      };

      return oi -> {
        Hashtable<String, String> ht = oi.getObjectName()
          .getKeyPropertyList();
        return not(isMonitor).and(isOfSelectedType)
          .test(ht);
      };
    }
  };

  public abstract Predicate<MBeanAttributeInfo> propertyFilter();

  public abstract Predicate<ObjectInstance> mbeanFilter();

  private static Predicate<MBeanAttributeInfo> isNumber() {
    //@formatter:off
    final List<String> numberTypes =
        Stream.of(Integer.class, Long.class, Double.class)
          .map(Class::getName)
          .collect(Collectors.toUnmodifiableList());
    //@formatter:on
    return c -> numberTypes.contains(c.getType());
  }

  private static Predicate<MBeanAttributeInfo> is(String prefix) {
    return c -> c.getName()
      .startsWith(prefix);
  }

  @SafeVarargs
  public static Predicate<MBeanAttributeInfo> isAnyOf(String... prefix) {
    return Stream.of(prefix)
      .map(MBeanFilter::is)
      .reduce(m -> false, (i, p1) -> i.or(p1));
  }
}


class MBeanInterfaceAdapter implements MBeanConnection {

  private final MBeanServerConnection connection;

  Logger log = LogManager.getLogger("export_jmx_metrics");

  public MBeanInterfaceAdapter(MBeanServerConnection connection) {
    this.connection = connection;
  }

  @Override
  public Stream<ObjectInstance> queryMBeans(ObjectName objectName) {
    try {
      return connection.queryMBeans(objectName, null)
        .stream()
        .filter(MBeanFilter.STANDARD_FITLER.mbeanFilter());
    } catch (IOException e) {
      log.error("Error while querying mbeans", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<MBeanAttributeInfo> getAttributeInfos(ObjectName objectName) {
    try {
      log.trace("Getting attribute list for bean <{}>", objectName);
      MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
      return Arrays.asList(mBeanInfo.getAttributes())
        .stream()
        .filter(MBeanFilter.STANDARD_FITLER.propertyFilter())
        .peek(m -> {
          log.debug("Collected attribute: {}.{}", objectName, m.getName());
        });
    } catch (Exception e) {
      log.warn("Returning empty list due to error while getting attribute list of bean <{}>",
          objectName);
      log.debug(e.getMessage(), e);
      return Stream.empty();
    }
  }

  @Override
  public Object getAttributeValue(ObjectName objectName, String attributeName) {
    try {
      log.trace("Getting attribute value <{}> of bean <{}>", attributeName, objectName);
      return connection.getAttribute(objectName, attributeName);
    } catch (Exception e) {
      log.warn("Returning null due to error while getting attribute <" + attributeName
          + "> of bean <" + objectName + ">", e);
      return null;
    }
  }

  @Override
  public <T> T getProxy(String obejctName, Class<T> proxy) {
    try {
      return JMX.newMBeanProxy(connection, new ObjectName(obejctName), proxy);
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    }
  }

}



public class JMXMetricsCollector extends Collector implements JMXMetrics {

  static class Gauges {

    private final Map<String, Gauge> gaugesMap = new HashMap<>();

    private String toKey(ObjectName objectName, String attributeName) {
      return objectName.getKeyProperty("type") + "_" + attributeName;
    }

    void addGauge(ObjectName objectName, String attributeName,
        Function<? super String, ? extends Gauge> gaugeFunc) {

      gaugesMap.computeIfAbsent(toKey(objectName, attributeName), gaugeFunc);
    }

    Gauge getGauge(ObjectName objectName, String attributeName) {
      return gaugesMap.get(toKey(objectName, attributeName));
    }

    int size() {
      return gaugesMap.size();
    }
  }

  public static final String NAME_SPACE = "lightstreamer";

  final Logger log = LogManager.getLogger("export_jmx_metrics");

  //@formatter:off
  private static final String[] DELAYED_THREAD_POOL_NAMES= {
    "TLS-SSL HANDSHAKE",
    "PUMP",
    "EVENTS"
  };

  private static final List<ObjectName> DELAYED_THREAD_POOLS =
    Stream.of(DELAYED_THREAD_POOL_NAMES)
      .map(s -> { 
        try {
          return new ObjectName("com.lightstreamer:name=" + s + ",type=ThreadPool");
        } catch (MalformedObjectNameException e) {
          throw new RuntimeException(e);
        }})
     .collect(Collectors.toList());
  //@formatter:on

  private static MBeanConnection getLocalConnection() {
    //@formatter:off
    MBeanServer conn = MBeanServerFactory.findMBeanServer(null)
      .stream()
      .filter(m -> Arrays.asList(m.getDomains()).contains("com.lightstreamer"))
      .findAny()
      .orElseThrow(() -> new RuntimeException("No 'com.lightstreamer' domain handled"));

    //@formatter:on
    return new MBeanInterfaceAdapter(conn);

  }

  private static MBeanInterfaceAdapter getRemoteConnection(String host, int port, String user,
      String password) {
    try {
      JMXServiceURL url =
          new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/lsjmx");
      Map<String, Object> s = new HashMap<String, Object>();
      s.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());
      s.put(JMXConnector.CREDENTIALS, new String[] {user, password});
      JMXConnector jmxc = JMXConnectorFactory.connect(url, s);

      return new MBeanInterfaceAdapter(jmxc.getMBeanServerConnection());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  final MBeanConnection serverConnection;

  final Gauges gauges = new Gauges();

  private Map<ObjectName, SingleMetricCollector> collectorsMap;

  private ClientTypeCollector clientTypeCollector;

  private ScheduledFuture<?> fut;

  public JMXMetricsCollector(MBeanConnection serverConnection) {
    this.serverConnection = serverConnection;
    inititalize();
  }

  public JMXMetricsCollector() {
    this(getLocalConnection());
  }

  private void inititalize() {

    ScheduledExecutorService checkRegisterDelayedThreadPoolIfAny =
        Executors.newScheduledThreadPool(1, new SimpleThreadFactory());

        fut = checkRegisterDelayedThreadPoolIfAny.scheduleAtFixedRate(() -> {
        try {
          log.debug("Starting metrics exporter initialization...");
          
          try {

            Server serv = serverConnection.getProxy("com.lightstreamer:type=Server", Server.class);

            log.info("Lightstreamer Server Status: {}", serv.getStatus());

            if (serv.getStatus().equalsIgnoreCase("running")) {
              fut.cancel(false);
              // Initialize single jmx metrics collectors
              collectorsMap = serverConnection.queryMBeans(null)
                .map(ObjectInstance::getObjectName)
               .map(o -> new SingleMetricCollector(serverConnection, o, gauges))
                .collect(Collectors.toMap(SingleMetricCollector::getObjectName, Function.identity()));

              // Initialize the client type metric collector
              clientTypeCollector = new ClientTypeCollector(serverConnection);
              // clientTypeCollector = new ClientTypeCollector(new MBeanConnectionMock());

              DefaultExports.initialize();

              log.debug("Starting JMX exporter regitration...");
              register();
              log.info("Registered JMX exporter");

              log.info("Initialized metrics exporter");
            }     
          } catch (Exception e) {
            if ( AttributeNotFoundException.class.isInstance(e.getCause()) ) {
              log.error("Metrics collector failed to start, your Lightstreamer license does not support the full JMX interface feature; or it is disabled through configuration.");
              log.debug(" - ", e);
            } else {
              log.warn("Error while querying mbeans", e);
            }
          }
        } catch (Exception e) {
          log.warn("Error while re-checking DelayedThreadPoolIfAny", e);
        }
      }, 0, 5, TimeUnit.SECONDS);
  }

  private void registerDelayedThreadPoolIfAny() {
    //@formatter:off
    DELAYED_THREAD_POOLS.stream()
      .filter(Predicate.not(collectorsMap::containsKey))
      .forEach(on -> {
        collectorsMap.computeIfAbsent(on, o -> {
          log.debug("Registering {} ThreadPool MBean", o.getKeyProperty("name"));
          return new SingleMetricCollector(serverConnection, o, gauges);
        });
      });
    //@formatter:on
  }

  @Override
  public final List<MetricFamilySamples> collect() {
    try {
      log.debug("Collecting metrics from JMX...");

      // CollectorRegistry.defaultRegistry.clear();

      registerDelayedThreadPoolIfAny();

      List<MetricFamilySamples> allSamples = new ArrayList<Collector.MetricFamilySamples>();

      // Stream of all collected JMX metrics
      allSamples.addAll(collectorsMap.values()
        .stream()
        .flatMap(SingleMetricCollector::collect)
        .distinct() // Here distinct is mandatory, otherwise we'll get redundant gauge values for
                    // different ThreadPool MBeans.
        .collect(Collectors.toList()));

      allSamples.addAll(clientTypeCollector.collect());

      // Iterate through the list and print each element
      for (MetricFamilySamples sample : allSamples) {
        log.trace("Output for the collect - pre - : " + sample.name + ", " + sample.type + ".");
      }

      // Create a LinkedHashMap to store unique MetricFamilySamples by name
      Map<String, MetricFamilySamples> uniqueSamplesMap = new LinkedHashMap<>();

      // Iterate through the list and add elements to the map
      for (MetricFamilySamples sample : allSamples) {
        uniqueSamplesMap.put(sample.name, sample);
      }

      // Convert the values of the map back to a list
      List<MetricFamilySamples> allSamplesFinal = uniqueSamplesMap.values().stream().collect(Collectors.toList());

      // Iterate through the list and print each element
      for (MetricFamilySamples sample : allSamplesFinal) {
        log.trace("Output for the collect - post - : " + sample.name + ", " + sample.type + ".");
      }

      return Collections.unmodifiableList(allSamplesFinal);
    } catch (Exception e) {
      log.warn("", e);
      return Collections.emptyList();
    } finally {
      log.debug("Collected metrics");
    }
  }

  public double getJMXValue(ObjectName objectName, String attributeName) {
    SingleMetricCollector collector = collectorsMap.get(objectName);
    return collector.get(attributeName);
  }

  public static void main(String[] args) throws Exception {
    String host = "localhost";
    int port = 8888;
    String user = "user_changeme";
    String pwd = "password_changeme";

    MBeanInterfaceAdapter connection = getRemoteConnection(host, port, user, pwd);

    Resource res = connection.getProxy("com.lightstreamer:type=Resource", Resource.class);
    Map<String, Long> currClientVersions = res.getCurrClientVersions(null);
    System.out.println(currClientVersions);


    // CountDownLatch l = new CountDownLatch(1);
    // new JMXMetricsCollector(connection);
    // new HTTPServer(4444);
    //
    // l.await();
  }

}

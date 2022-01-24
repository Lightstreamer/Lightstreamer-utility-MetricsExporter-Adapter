package com.lightstreamer.utility.metrics.jmx;

import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

class MBeanConnectionMock implements MBeanConnection {

  private final Map<String, Long> clientVersions;

  public MBeanConnectionMock(Map<String, Long> v) {
    this.clientVersions = v;
  }

  public MBeanConnectionMock() {
    this.clientVersions = null;
  }

  @Override
  public Stream<ObjectInstance> queryMBeans(ObjectName objectName) {
    return null;
  }

  @Override
  public Stream<MBeanAttributeInfo> getAttributeInfos(ObjectName objectName) {
    return null;
  }

  @Override
  public Object getAttributeValue(ObjectName objectName, String attributeName) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getProxy(String obejctName, Class<T> proxy) {
    return (T) new Resource() {

      @Override
      public Map<String, Long> getCurrClientVersions(String placeholder) {
        if (clientVersions != null) {
          return clientVersions;
        }

        Random r = new Random();
        long javaScriptClient1 = r.nextInt(101);
        long javaScriptClient2 = r.nextInt(101);
        long javaClients = r.nextInt(101);
        return Map.of("javascript_client 8.0.2 build 1797", javaScriptClient1,
            "javascript_client 0.1.0 build 1", javaScriptClient2, "java_client 4.3.3 build 105",
            javaClients);
      }
    };

  }

}

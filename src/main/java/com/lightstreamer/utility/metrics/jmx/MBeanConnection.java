package com.lightstreamer.utility.metrics.jmx;

import java.util.stream.Stream;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

interface MBeanConnection {

  Stream<ObjectInstance> queryMBeans(ObjectName objectName);

  Stream<MBeanAttributeInfo> getAttributeInfos(ObjectName objectName);

  Object getAttributeValue(ObjectName objectName, String attributeName);

  <T> T getProxy(String obejctName, Class<T> proxy);

}
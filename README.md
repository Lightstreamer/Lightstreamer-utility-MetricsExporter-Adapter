# Lightstreamer Metrics Exporter Adapter Set - Prometheus Integration

This project includes the source code of the Lightstreamer Metrics Exporter Adapter Set.
A ready made Adapter Set for fast deployment into Lightstreamer server an immediate export of numerous metrics for [Prometheus monitoring solution](https://prometheus.io/) out of the box.

This code is designed for Java 8 and greater.

### The Adapter Architecture

This metrics exporter is intended to be run as a Lightsteamer in-process adapter, exposing an HTTP server and serving metrics of the local JVM.

![architecture](metrics_exporter_schema.png)

The code leverages the [Prometheus Java Client](https://github.com/prometheus/client_java) and include the instrumentation logic into a Metadata Adapter class.
A custom collector is implemented to proxy metrics coming from MBeans with a one-to-one mapping between MBean attribute and Prometheus metric.
The format of a metric is something like:

	<server_name>_<bean_name>_<attribute_name>{beanpropertyName1="beanPropertyValue1", ...}: value
 
examples:

```
	lightstreamer_Stream_CumulItemUpdates 381.0
	lightstreamer_ThreadPool_Throughput{name="EVENTS",} 4.0
	lightstreamer_ThreadPool_Throughput{name="PUMP",} 5.997
	lightstreamer_ThreadPool_Throughput{name="SNAPSHOT",} 0.0
	lightstreamer_DataAdapter_InboundEventFrequency{AdapterSetName="WELCOME",DataAdapterName="CHAT",} 0.0
	lightstreamer_DataAdapter_InboundEventFrequency{AdapterSetName="DEMO",DataAdapterName="QUOTE_ADAPTER",} 1.5
```

### Configuration

Metrics will be accessible at `http://localhost:<metrics_port>/` or `http://<hostname>:<metrics_port>/` where <hostname> is the hostanme of the machine and <metrics_port> is a configuration parameter 
in the `adapters.xml` file.

Only a subset of [all attributes available from JMX](https://sdk.lightstreamer.com/ls-jmx-sdk/5.6.0/api/index.html) are considered for export.
Currently the filtering criteria are hard-coded in the [enum MBeanFilter](https://github.com/Lightstreamer/Lightstreamer-utility-MetricsExporter-Adapter/blob/master/src/main/java/com/lightstreamer/utility/metrics/jmx/JMXMetricsCollector.java#L66), but it is planned to implement the possibility of configure these criteria through the 'adapters.xml' configuration file.

## Install

To install the *Lightstreamer Metrics Exporter Adapter Set* in your local Lightstreamer Server: get the `deploy.zip` file of the [latest release](https://github.com/Lightstreamer/Lightstreamer-utility-MetricsExporter-Adapter/releases), unzip it, and copy the `metrics_exporter` folder into the `adapters` folder of your Lightstreamer Server installation.
A Lightstreamer Server reboot is needed.


## Build

To build your own version of `lightstreamer-utility-metricsexporter-adapter-0.1.0` instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Lightstreamer/Lightstreamer-utility-MetricsExporter-Adapter#install) section above, you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and building (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity only the Maven case is detailed here.

### Maven

You can easily build and run this application using Maven through the pom.xml file located in the root folder of this project. As an alternative, you can use an alternative build tool (e.g. Gradle, Ivy, etc.) by converting the provided pom.xml file.

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 mvn install dependency:copy-dependencies 
```

## Support

For questions and support please use the [Official Forum](https://forums.lightstreamer.com/).
The issue list of this page is **exclusively** for bug reports and feature requests.

## License

[Apache 2.0](https://opensource.org/licenses/Apache-2.0)

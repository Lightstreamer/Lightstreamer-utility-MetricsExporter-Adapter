package com.lightstreamer.utility;

import java.io.File;
import java.util.Map;

import com.lightstreamer.adapters.metadata.LiteralBasedProvider;
import com.lightstreamer.interfaces.metadata.MetadataProviderException;
import com.lightstreamer.utility.metrics.Metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.exporter.HTTPServer;

public class MetricsExportMetadataAdapter extends LiteralBasedProvider {
    
    @FunctionalInterface
    private static interface CheckedRunnable {
      void run() throws Throwable;
    }
    
     /**
     * Private logger; a specific "LS_demos_Logger.MetricsExporter" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;

    private static final String METRICS_PORT = "metrics_port";

    private static final String DEFAULT_METRICS_PORT = "7777";

    @SuppressWarnings("unchecked")
    private void exportMetrics(@SuppressWarnings("rawtypes") Map params)
        throws MetadataProviderException {
  
      String metricsPort = (String) params.getOrDefault(METRICS_PORT, DEFAULT_METRICS_PORT);
      tryExecute(() -> new HTTPServer(Integer.parseInt(metricsPort)));
    }
  
    private void tryExecute(CheckedRunnable run) throws MetadataProviderException {
        try {
          run.run();
        } catch (Throwable thr) {
          throw new MetadataProviderException(thr.getMessage());
        }
      }

     @Override
    public void init(Map params, File dir) throws MetadataProviderException {
        
        super.init(params, dir);

        logger = LogManager.getLogger("LS_demos_Logger.MetricsExporter");

        exportMetrics(params);
        
        Metrics.getJMXExporter();
    }
}

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

        logger = LogManager.getLogger("export_jmx_metrics");

        exportMetrics(params);
        
        Metrics.getJMXExporter();
    }
}

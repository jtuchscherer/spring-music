package org.cloudfoundry.samples.music.nmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(value = "nmt.metrics.enabled", matchIfMissing = true)
public class NMTMetric implements PublicMetrics {

    private static final String NMT_METRIC_PREFIX = "nmt.";
    private final Logger logger = LoggerFactory.getLogger(NMTMetric.class);
    @Autowired
    private NMTCommandRunner NMTCommandRunner;

    @Autowired
    private NMTFullExtractor nmtExtractor;

    @Override
    public Collection<Metric<?>> metrics() {

        String nmtOutput;
        try {
            nmtOutput = NMTCommandRunner.runNMTSummary();
        } catch (NMTNotEnabledException e) {
            logger.error("Could not add NMT metrics to /metric endpoint - NMT not enabled\n" +
                    "Please add -XX:NativeMemoryTracking=summary to your Java command line params");
            return Collections.emptyList();
        }

        Map<String, Map<String, Integer>> nmtProperties = nmtExtractor.extractProperties(nmtOutput);

        if (nmtProperties.isEmpty()) {
            logger.info("None NMT metric has been added to /metrics endpoint");
            return Collections.emptyList();
        }

        List<Metric<?>> metrics = new ArrayList<>();
        nmtProperties.forEach((category, properties) -> {
            properties.forEach((key, value) -> {
                metrics.add(new Metric<>(NMT_METRIC_PREFIX + category + "." + key, value));

            });
            logger.debug("Added metrics for category : {}", category);
        });

        return metrics;
    }
}

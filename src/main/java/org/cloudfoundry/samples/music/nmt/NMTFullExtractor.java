package org.cloudfoundry.samples.music.nmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class NMTFullExtractor {
    private final Logger logger = LoggerFactory.getLogger(NMTFullExtractor.class);
    private static final String RESERVED_PROPERTY = "reserved";
    private static final String COMMITTED_PROPERTY = "committed";
    private static final String MMAP_RESERVED_PROPERTY = "mmapreserved";
    private static final String MMAP_COMMITTED_PROPERTY = "mmapcommitted";
    private static final String STACK_RESERVED_PROPERTY = "stackreserved";
    private static final String STACK_COMMITTED_PROPERTY = "stackcommitted";
    private static final String CATEGORY_PROPERTY = "category";
    private static final String MALLOC_PROPERTY = "malloc";
    private static final String MALLOC_CALLS_PROPERTY = "malloccalls";
    private static final String ARENA_PROPERTY = "arena";
    private static final String ARENA_CALLS_PROPERTY = "arenacalls";
    private static final String CLASSIFIER_PROPERTY = "classifier";
    private static final String CLASSIFIER_CALLS_PROPERTY = "classifiercalls";
    private final Pattern totalPattern = Pattern.compile("Total: reserved=(?<" + RESERVED_PROPERTY + ">\\d*)KB, committed=(?<" + COMMITTED_PROPERTY + ">\\d*)KB");
    private final Pattern categoryPattern = Pattern.compile("-\\s*(?<" + CATEGORY_PROPERTY + ">.*) \\(reserved=(?<" + RESERVED_PROPERTY + ">\\d*)KB, committed=(?<" + COMMITTED_PROPERTY + ">\\d*)KB\\)");
    private final Pattern mallocPattern = Pattern.compile("\\s*\\(malloc=(?<" + MALLOC_PROPERTY + ">\\d*)KB( #(?<" + MALLOC_CALLS_PROPERTY + ">\\d*))?\\)");
    private final Pattern arenaPattern = Pattern.compile("\\s*\\(arena=(?<" + ARENA_PROPERTY + ">\\d*)KB( #(?<" + ARENA_CALLS_PROPERTY + ">\\d*))?\\)");
    private final Pattern mmapPattern = Pattern.compile("\\s*\\(mmap: reserved=(?<" + MMAP_RESERVED_PROPERTY + ">\\d*)KB, committed=(?<" + MMAP_COMMITTED_PROPERTY + ">\\d*)KB\\)");
    private final Pattern stackPattern = Pattern.compile("\\s*\\(stack: reserved=(?<" + STACK_RESERVED_PROPERTY + ">\\d*)KB, committed=(?<" + STACK_COMMITTED_PROPERTY + ">\\d*)KB\\)");
    private final Pattern classesAndThreadsPattern = Pattern.compile("\\s*\\((?<" + CLASSIFIER_PROPERTY + ">\\w+) #(?<" + CLASSIFIER_CALLS_PROPERTY + ">\\d*)\\)");


    public Map<String, Map<String, Integer>> extractProperties(String jcmdOutput) {
        Map<String, Map<String, Integer>> nmtProperties = new HashMap<>();

        try {
            extractTotalProperty(jcmdOutput, nmtProperties);
            extractAllCategories(jcmdOutput, nmtProperties);
            logger.debug("Extracted NMT properties : {}", nmtProperties.toString());
        } catch (Throwable t) {
            logger.error("Could not parse NMT output", t);
        }

        if (nmtProperties.isEmpty()) {
            logger.info("NMT properties are empty after extraction. Probably something wrong occurred during extraction");
        }
        return nmtProperties;
    }

    private void extractAllCategories(String jcmdOutput, Map<String, Map<String, Integer>> nmtProperties) {

        if (jcmdOutput.indexOf("-") > 0) {
            jcmdOutput = jcmdOutput.substring(jcmdOutput.indexOf("-"));
        } else {
            logger.error("Could not parse NMT output: " + jcmdOutput);
            return;
        }
        String categories[] = jcmdOutput.split("\\r?\\n\\s*\\r?\\n");
        for (String category : categories) {
            String categoryLines[] = category.split("\\r?\\n", 2);
            if (categoryLines.length > 0) {
                Matcher matcher = categoryPattern.matcher(categoryLines[0]);
                if (matcher.find()) {
                    Map<String, Integer> properties = new HashMap<>();

                    properties.put(RESERVED_PROPERTY, Integer.parseInt(matcher.group(RESERVED_PROPERTY)));
                    properties.put(COMMITTED_PROPERTY, Integer.parseInt(matcher.group(COMMITTED_PROPERTY)));
                    String categoryNane = matcher.group(CATEGORY_PROPERTY).toLowerCase().replace(" ", ".");

                    if (categoryLines.length > 1) {
                        String categoryMetrics[] = categoryLines[1].split("\\r?\\n");
                        for (String categoryMetric : categoryMetrics) {
                            if (categoryMetric.contains("(malloc=")) {
                                matcher = mallocPattern.matcher(categoryMetric);
                                if (matcher.find()) {
                                    properties.put(MALLOC_PROPERTY, Integer.parseInt(matcher.group(MALLOC_PROPERTY)));
                                    if (matcher.group(MALLOC_CALLS_PROPERTY) != null) {
                                        properties.put(MALLOC_CALLS_PROPERTY, Integer.parseInt(matcher.group(MALLOC_CALLS_PROPERTY)));
                                    }
                                }
                            } else if (categoryMetric.contains("(arena=")) {
                                matcher = arenaPattern.matcher(categoryMetric);
                                if (matcher.find()) {
                                    properties.put(ARENA_PROPERTY, Integer.parseInt(matcher.group(ARENA_PROPERTY)));
                                    if (matcher.group(ARENA_CALLS_PROPERTY) != null) {
                                        properties.put(ARENA_CALLS_PROPERTY, Integer.parseInt(matcher.group(ARENA_CALLS_PROPERTY)));
                                    }
                                }
                            } else if (categoryMetric.contains("(mmap: ")) {
                                matcher = mmapPattern.matcher(categoryMetric);
                                if (matcher.find()) {
                                    properties.put(MMAP_RESERVED_PROPERTY, Integer.parseInt(matcher.group(MMAP_RESERVED_PROPERTY)));
                                    properties.put(MMAP_COMMITTED_PROPERTY, Integer.parseInt(matcher.group(MMAP_COMMITTED_PROPERTY)));
                                }
                            } else if (categoryMetric.contains("(stack: ")) {
                                matcher = stackPattern.matcher(categoryMetric);
                                if (matcher.find()) {
                                    properties.put(STACK_RESERVED_PROPERTY, Integer.parseInt(matcher.group(STACK_RESERVED_PROPERTY)));
                                    properties.put(STACK_COMMITTED_PROPERTY, Integer.parseInt(matcher.group(STACK_COMMITTED_PROPERTY)));
                                }
                            } else if (categoryMetric.contains("(classes ") || categoryMetric.contains("(thread ")) {
                                matcher = classesAndThreadsPattern.matcher(categoryMetric);
                                if (matcher.find()) {
                                    properties.put(matcher.group(CLASSIFIER_PROPERTY), Integer.parseInt(matcher.group(CLASSIFIER_CALLS_PROPERTY)));
                                }
                            }
                        }
                    }
                    nmtProperties.put(categoryNane, properties);
                }
            }
        }

    }

    private void extractTotalProperty(String jcmdOutput, Map<String, Map<String, Integer>> nmtProperties) {
        Matcher matcher = totalPattern.matcher(jcmdOutput);
        matcher.find();
        Map<String, Integer> properties = new HashMap<>();
        properties.put(RESERVED_PROPERTY, Integer.parseInt(matcher.group(RESERVED_PROPERTY)));
        properties.put(COMMITTED_PROPERTY, Integer.parseInt(matcher.group(COMMITTED_PROPERTY)));
        nmtProperties.put("total", properties);
    }
}
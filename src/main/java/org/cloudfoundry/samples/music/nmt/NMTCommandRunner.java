package org.cloudfoundry.samples.music.nmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Scanner;

@Component
class NMTCommandRunner {

    private static String os = System.getProperty("os.name").toLowerCase();
    private static boolean isUnix = os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0;
    private static boolean isWindows = os.indexOf("win") >= 0;
    private static boolean isMacOsX = os.indexOf("mac os x") >= 0;
    private static String JCMD_CMD;
    private final Logger logger = LoggerFactory.getLogger(NMTCommandRunner.class);
    private boolean nmtEnabled = false;

    @Autowired
    private Environment environment;

    @PostConstruct
    private void init() {
        if (isUnix) {
            JCMD_CMD = "./jcmd";
        } else if (isWindows || isMacOsX) {
            JCMD_CMD = "jcmd";
        } else {
            throw new RuntimeException("OS not supported ! NMTCommandRunner only supports Windows and Unix systems." +
                    "Your system is: " + os);
        }

        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        List<String> javaOpts = mxBean.getInputArguments();
        nmtEnabled = javaOpts.contains("-XX:NativeMemoryTracking=summary");
    }

    String runNMTSummary() throws NMTNotEnabledException {
        if (!nmtEnabled) {
            throw new NMTNotEnabledException();
        }

        ProcessBuilder builder = new ProcessBuilder(JCMD_CMD, environment.getProperty("PID"), "VM.native_memory summary");
        builder.directory(new File(environment.getProperty("java.home") + File.separator + "bin"));
        String cmd = builder.command().toString();
        logger.info("Running command : {}", cmd);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = readCommandOutput(process);
            logger.debug("Output of command {} : {}", cmd, output);
            return output;
        } catch (IOException e) {
            logger.error("Error while starting command : {}", cmd, e);
        }

        return null;
    }

    private String readCommandOutput(Process process) {
        StringBuilder sb = new StringBuilder();
        Scanner scanner = new Scanner(process.getInputStream());
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine());
            sb.append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }
}

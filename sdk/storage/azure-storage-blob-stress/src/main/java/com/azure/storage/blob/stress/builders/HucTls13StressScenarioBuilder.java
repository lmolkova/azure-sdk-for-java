package com.azure.storage.blob.stress.builders;

import com.azure.core.util.logging.ClientLogger;
import com.azure.storage.blob.stress.scenarios.DownloadToFileStressScenario;
import com.azure.storage.blob.stress.scenarios.HucTls13StressScenario;
import com.azure.storage.stress.StorageStressScenario;

import java.nio.file.Path;

public class HucTls13StressScenarioBuilder extends BlobScenarioBuilder
{
    private static final ClientLogger LOGGER = new ClientLogger(HucTls13StressScenarioBuilder.class);
    private static void printAndValidateJavaVersion() {
        Runtime.Version javaVersion = Runtime.version();
        LOGGER.atInfo().addKeyValue("version", javaVersion).log("Java version");

        if (javaVersion.feature() >= 20
                || (javaVersion.feature() == 11 && javaVersion.update() >= 18)
                || (javaVersion.feature() == 17 && javaVersion.update() >= 7)) {
            throw new RuntimeException("Only Java 11 before update 18, Java 12-16, Java 17 before update 7, and "
                    + "Java 18-19 are affected by this issue.");
        }
    }

    public HucTls13StressScenarioBuilder() {
        printAndValidateJavaVersion();
    }

     @Override
    public StorageStressScenario build() {
        return new HucTls13StressScenario(this);
    }
}

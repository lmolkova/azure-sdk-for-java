// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress;

import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.eventhubs.stress.scenarios.EventHubsScenario;
import com.azure.messaging.eventhubs.stress.util.ScenarioOptions;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.Objects;

import static com.azure.messaging.eventhubs.stress.util.TestUtils.startSampledInSpan;
import static java.lang.System.exit;

/**
 * Runner for the Event Hubs stress tests.
 */
@SpringBootApplication
public class EventHubsScenarioRunner implements ApplicationRunner {

    private static final ClientLogger LOGGER = new ClientLogger(EventHubsScenarioRunner.class);
    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected ScenarioOptions options;

    public static void main(String[] args) {
        SpringApplication.run(EventHubsScenarioRunner.class, args);
    }

    /**
     * Run test scenario class.
     *
     * @param args the application arguments. it should contain "--TEST_CLASS='your test class'".
     */
    @Override
    public void run(ApplicationArguments args) {
        String scenarioName = Objects.requireNonNull(options.getTestClass(),
            "The test class should be provided, please add --TEST_CLASS=<your test class> as start argument");
        EventHubsScenario scenario = (EventHubsScenario) applicationContext.getBean(scenarioName);

        beforeRun(scenario);
        Instant startTime = Instant.now();
        try {
            scenario.run();
        } catch (Exception ex) {
            scenario.recordError("scenario error", ex, "run");
            exit(1);
        } finally {
            afterRun(scenario, startTime);
            scenario.close();
        }
    }

    @SuppressWarnings("try")
    private void beforeRun(EventHubsScenario scenario) {
        Span before = startSampledInSpan("before run");
        try (Scope s = before.makeCurrent()) {
            scenario.recordRunOptions(before);
            scenario.beforeRun();
        } finally {
            before.end();
        }
    }

    @SuppressWarnings("try")
    private void afterRun(EventHubsScenario scenario, Instant start) {
        Span after = startSampledInSpan("after run");
        after.setAttribute(AttributeKey.longKey("durationMs"), Instant.now().toEpochMilli() - start.toEpochMilli());
        try (Scope s = after.makeCurrent()) {
            scenario.afterRun();
        } catch (Exception ex) {
            LOGGER.logThrowableAsWarning(ex);
        } finally {
            after.end();
        }
    }
}

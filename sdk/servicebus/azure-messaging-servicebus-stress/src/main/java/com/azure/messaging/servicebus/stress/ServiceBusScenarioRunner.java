// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress;

import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.stress.scenarios.BinderConsumer;
import com.azure.messaging.servicebus.stress.scenarios.ServiceBusScenario;
import com.azure.messaging.servicebus.stress.util.RunResult;
import com.azure.messaging.servicebus.stress.util.ScenarioOptions;
import com.azure.spring.messaging.servicebus.core.listener.ServiceBusMessageListenerContainer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.Objects;

import static java.lang.System.exit;

/**
 * Runner for the Service Bus stress tests.
 */
@SpringBootApplication
public class ServiceBusScenarioRunner implements ApplicationRunner {
    private static final ClientLogger LOGGER = new ClientLogger(ServiceBusScenarioRunner.class);
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("ServiceBusScenario");

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected ScenarioOptions options;

    public static void main(String[] args) {
        SpringApplication.run(ServiceBusScenarioRunner.class, args);
    }

    /**
     * Run test scenario class.
     *
     * @param args the application arguments. it should contain "--TEST_CLASS='your scenarios class name'".
     */
    @Override
    public void run(ApplicationArguments args) {
        String scenarioName = Objects.requireNonNull(options.getTestClass(),
            "The test class should be provided, please add --TEST_CLASS=<your test class> as start argument");
        ServiceBusScenario scenario = (ServiceBusScenario) applicationContext.getBean(scenarioName);
        beforeRun(scenario);

        Instant startTime = Instant.now();
        RunResult result;
        try {
            result = scenario.run();
        } catch (Exception ex) {
            LOGGER.error("error running scenario", ex);
            result = RunResult.ERROR;
        }

        afterRun(scenario, startTime, result);

        scenario.close();
        exit(0);
    }

    @SuppressWarnings("try")
    private void afterRun(ServiceBusScenario scenario, Instant start, RunResult result) {
        Span after = startSpan("after run");
        after.setAttribute(AttributeKey.stringKey("result"), result.name());
        after.setAttribute(AttributeKey.longKey("durationMs"), Instant.now().toEpochMilli() - start.toEpochMilli());
        try (Scope s = after.makeCurrent()) {
            scenario.afterRun(result);
        } catch (Exception ex) {
            LOGGER.logThrowableAsWarning(ex);
        } finally {
            after.end();
        }
    }

    @SuppressWarnings("try")
    private void beforeRun(ServiceBusScenario scenario) {
        Span before = startSpan("before run");
        recordTestConfiguration(before);
        try (Scope s = before.makeCurrent()) {
            scenario.beforeRun();
        } finally {
            before.end();
        }
    }

    private Span startSpan(String name) {
        return TRACER.spanBuilder(name)
            // guarantee that we have before/after spans sampled in
            // and record duration/result of the test
            .setAttribute(AttributeKey.stringKey("sample.me.in"), "true")
            .startSpan();
    }

    private void recordTestConfiguration(Span span) {

        String serviceBusPackageVersion = "unknown";
        try {
            Class<?> serviceBusPackage = Class.forName("com.azure.messaging.servicebus.ServiceBusClientBuilder");
            serviceBusPackageVersion = serviceBusPackage.getPackage().getImplementationVersion();
            if (serviceBusPackageVersion == null) {
                serviceBusPackageVersion = "null";
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warning("could not find ServiceBusClientBuilder class", e);
        }

        span.setAttribute(AttributeKey.stringKey("duration"), options.getTestDuration().toString());
        span.setAttribute(AttributeKey.stringKey("tryTimeout"), options.getTryTimeout().toString());
        span.setAttribute(AttributeKey.stringKey("testClass"), options.getTestClass());
        span.setAttribute(AttributeKey.stringKey("entityType"), options.getServiceBusEntityType().toString());
        span.setAttribute(AttributeKey.stringKey("queueName"), options.getServiceBusQueueName());
        span.setAttribute(AttributeKey.stringKey("topicName"), options.getServiceBusTopicName());
        span.setAttribute(AttributeKey.stringKey("sessionQueueName"), options.getServiceBusSessionQueueName());
        span.setAttribute(AttributeKey.stringKey("subscriptionName"), options.getServiceBusSubscriptionName());
        span.setAttribute(AttributeKey.stringKey("serviceBusPackageVersion"), serviceBusPackageVersion);
        span.setAttribute(AttributeKey.stringKey("annotation"), options.getAnnotation());
    }
}

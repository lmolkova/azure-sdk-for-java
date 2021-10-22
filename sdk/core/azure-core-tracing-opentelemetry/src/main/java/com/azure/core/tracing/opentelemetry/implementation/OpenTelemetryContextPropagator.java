package com.azure.core.tracing.opentelemetry.implementation;

import io.opentelemetry.context.Scope;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

public class OpenTelemetryContextPropagator<T> implements CoreSubscriber<T> {
    private io.opentelemetry.context.Context traceContext;
    private final Subscriber<? super T> subscriber;
    private final reactor.util.context.Context context;

    public OpenTelemetryContextPropagator(
        Subscriber<? super T> subscriber,
        reactor.util.context.Context ctx,
        io.opentelemetry.context.Context traceContext) {
        this.subscriber = subscriber;
        this.context = ctx;
        this.traceContext = traceContext;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        withActiveSpan(() -> subscriber.onSubscribe(subscription));
    }

    @Override
    public void onNext(T o) {
        withActiveSpan(() -> subscriber.onNext(o));
    }

    @Override
    public void onError(Throwable throwable) {
        withActiveSpan(() -> subscriber.onError(throwable));
    }

    @Override
    public void onComplete() {
        withActiveSpan(subscriber::onComplete);
    }

    @Override
    public reactor.util.context.Context currentContext() {
        return context;
    }

    private void withActiveSpan(Runnable runnable) {
        try (Scope ignored = traceContext.makeCurrent()) {
            runnable.run();
        }
    }
}

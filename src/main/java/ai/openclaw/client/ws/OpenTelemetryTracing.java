package ai.openclaw.client.ws;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class OpenTelemetryTracing {

    private final Tracer tracer;
    private final boolean enabled;

    public OpenTelemetryTracing(Tracer tracer) {
        this(tracer, true);
    }

    public OpenTelemetryTracing(Tracer tracer, boolean enabled) {
        this.tracer = tracer;
        this.enabled = enabled;
    }

    public TracedResult executeWithTrace(String operationName, TraceableOperation operation) {
        if (!enabled || tracer == null) {
            try {
                Object result = operation.execute();
                return new TracedResult(result, null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Span span = tracer.spanBuilder(operationName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            Object result = operation.execute();
            span.setAttribute("operation.success", true);
            return new TracedResult(result, span, scope);
        } catch (Exception e) {
            span.setAttribute("operation.success", false);
            span.recordException(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    public Span startSpan(String name) {
        if (!enabled || tracer == null) {
            return null;
        }
        return tracer.spanBuilder(name).startSpan();
    }

    public void endSpan(Span span) {
        if (span != null) {
            span.end();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public interface TraceableOperation {
        Object execute() throws Exception;
    }

    public static class TracedResult {
        private final Object result;
        private final Span span;
        private final Scope scope;

        public TracedResult(Object result, Span span, Scope scope) {
            this.result = result;
            this.span = span;
            this.scope = scope;
        }

        public Object getResult() {
            return result;
        }

        public Span getSpan() {
            return span;
        }
    }
}

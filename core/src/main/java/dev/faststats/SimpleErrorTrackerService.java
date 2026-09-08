package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

final class SimpleErrorTrackerService extends SubmissionService implements ErrorTrackerService {
    private static Thread.@Nullable UncaughtExceptionHandler originalHandler;
    private static final Set<SimpleErrorTracker> DISPATCHER_TRACKERS = new CopyOnWriteArraySet<>();

    private final Set<SimpleErrorTracker> errorTrackers = new CopyOnWriteArraySet<>();
    private final SimpleErrorTracker globalErrorTracker;

    private final URI url = getServerUrl(
            "faststats.error-tracker-server",
            "https://metrics.faststats.dev"
    ).resolve("/v1/error");

    SimpleErrorTrackerService(final SimpleContext context, final ErrorTracker globalErrorTracker) {
        super(context);
        this.globalErrorTracker = ((SimpleErrorTracker) globalErrorTracker);
    }

    @Override
    public ErrorTracker globalErrorTracker() {
        return globalErrorTracker;
    }

    @Override
    public ErrorTrackerService registerErrorTracker(final ErrorTracker errorTracker) {
        errorTrackers.add(((SimpleErrorTracker) errorTracker));
        return this;
    }

    static synchronized void attachErrorTracker(final SimpleErrorTracker tracker) {
        if (DISPATCHER_TRACKERS.isEmpty()) {
            originalHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(SimpleErrorTrackerService::handleUncaughtException);
        }
        DISPATCHER_TRACKERS.add(tracker);
    }

    static synchronized void detachErrorTracker(final SimpleErrorTracker tracker) {
        DISPATCHER_TRACKERS.remove(tracker);
        if (DISPATCHER_TRACKERS.isEmpty()) {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
            originalHandler = null;
        }
    }

    private static void handleUncaughtException(final Thread thread, final Throwable error) {
        for (final var tracker : DISPATCHER_TRACKERS) {
            try {
                final var loader = tracker.attachedLoader();
                if (loader != null && !ErrorHelper.isSameLoader(thread, loader, error)) continue;
                tracker.trackError(error).handled(false);
                tracker.getContextErrorHandler().ifPresent(handler -> handler.accept(loader, error));
            } catch (final Throwable t) {
                // todo: replace with better solution
                System.err.println("Failed to dispatch uncaught error to tracker");
                t.printStackTrace(System.err);
            }
        }

        final var handler = originalHandler;
        if (handler != null) handler.uncaughtException(thread, error);
    }

    private void submit() {
        try {
            final var data = createData();
            if (data == null) return;

            if (submit(url, data, "errors")) clear();
        } catch (final Throwable t) {
            logger.error("Failed to submit errors", t);
        }
    }

    @VisibleForTesting
    public @Nullable JsonObject createData() {
        final var globalErrorTrackerData = globalErrorTracker.getData(false);
        if (errorTrackers.isEmpty() && globalErrorTrackerData.isEmpty()) return null;

        final var data = new JsonObject();
        context.getSdkInfo().getBuildId().ifPresent(id -> data.addProperty("buildId", id));
        data.addProperty("identifier", context.getConfig().serverId().toString());
        data.addProperty("language", "java");
        data.addProperty("project_name", context.getProjectName());
        data.addProperty("sdk_name", context.getSdkInfo().getName());
        data.addProperty("sdk_version", context.getSdkInfo().getVersion());

        final var defaultContext = new JsonObject();
        context.metrics().ifPresent(metrics -> {
            final var simpleMetrics = (SimpleMetrics) metrics;
            simpleMetrics.appendData(defaultContext);
        });
        globalErrorTracker.getAttributes().forEachPrimitive(defaultContext::add);
        data.add("context", defaultContext);

        final var errors = new JsonArray();
        errors.addAll(globalErrorTrackerData);
        errorTrackers.forEach(tracker -> errors.addAll(tracker.getFullData()));
        data.add("errors", errors);
        return data;
    }

    private void clear() {
        globalErrorTracker.clear();
        errorTrackers.forEach(SimpleErrorTracker::clear);
    }

    void startErrorSubmission() {
        logger.info("Starting errors submission task");
        final var initialDelay = TimeUnit.SECONDS.toMillis(Long.getLong("faststats.initial-delay", 30));
        final var period = TimeUnit.MINUTES.toMillis(30);
        context.scheduleAtFixedRate(this::submit, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    protected String serverType() {
        return "error";
    }

    public void shutdown() {
        submit();
        clear();
    }
}

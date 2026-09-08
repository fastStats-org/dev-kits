package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class ErrorHelper {
    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final int MAX_FRAME_SIZE = 300;
    public static final int MAX_STACK_SIZE = 30;

    private static final Set<String> allowedNames = Set.of("minecraft", "server", "root", "ubuntu");
    private static final List<Map.Entry<Pattern, String>> defaultAnonymizationEntries = defaultAnonymizationEntries();

    public static JsonObject compile(final TrackedError error, @Nullable final List<String> suppress,
                                     final List<Map.Entry<Pattern, String>> customPatterns,
                                     @Nullable final Attributes attributes) {
        final var patterns = new ArrayList<>(customPatterns);
        patterns.addAll(defaultAnonymizationEntries);
        return compileAll(error, suppress, patterns, attributes);
    }

    private static JsonObject compileAll(final TrackedError trackedError, @Nullable final List<String> suppress,
                                         final List<Map.Entry<Pattern, String>> customPatterns,
                                         @Nullable final Attributes defaultAttributes) {
        final var error = trackedError.error();
        final var report = new JsonObject();
        final var message = anonymize(error.message(), customPatterns);

        final var stacktrace = new JsonArray();
        final var header = message != null
                ? error.type().getName() + ": " + message
                : error.type().getName();
        stacktrace.add(header);

        final var elements = error.stackTraces();
        final var stack = collapseStackTrace(elements);
        final var list = new ArrayList<>(stack);
        if (suppress != null) list.removeAll(suppress);
        final var traces = Math.min(list.size(), MAX_STACK_SIZE);

        populateTraces(traces, list, elements, stacktrace);
        appendCauseChain(error.cause(), stack, suppress, stacktrace, customPatterns);

        report.addProperty("error", error.type().getName());
        final var first = anonymize(findFirstMessage(error), customPatterns);
        if (first != null) report.addProperty("message", first);

        report.add("stack", stacktrace);
        report.addProperty("handled", trackedError.handled());

        final var attributes = new JsonObject();
        if (defaultAttributes != null) defaultAttributes.forEachPrimitive(attributes::add);
        trackedError.attributes().forEachPrimitive(attributes::add);
        if (!attributes.isEmpty()) report.add("context", attributes);

        return report;
    }

    private static void appendCauseChain(TrackedError.@Nullable ThrowableSnapshot cause, final List<String> parentStack,
                                         @Nullable final List<String> suppress, final JsonArray stacktrace,
                                         final List<Map.Entry<Pattern, String>> customPatterns) {
        final var toSuppress = new ArrayList<>(parentStack);
        if (suppress != null) toSuppress.addAll(suppress);
        while (cause != null) {
            final var causeMessage = anonymize(cause.message(), customPatterns);
            final var header = causeMessage != null
                    ? "Caused by: " + cause.type().getName() + ": " + causeMessage
                    : "Caused by: " + cause.type().getName();
            stacktrace.add(header);

            final var causeElements = cause.stackTraces();
            final var causeStack = collapseStackTrace(causeElements);
            final var causeList = new ArrayList<>(causeStack);
            causeList.removeAll(toSuppress);
            final var causeTraces = Math.min(causeList.size(), MAX_STACK_SIZE);
            populateTraces(causeTraces, causeList, causeElements, stacktrace);

            cause = cause.cause();
        }
    }

    private static void populateTraces(final int traces, final List<String> list, final StackTraceElement[] elements,
                                       final JsonArray stacktrace) {
        for (var i = 0; i < traces; i++) {
            final var string = list.get(i);
            if (MAX_FRAME_SIZE < 0 || string.length() <= MAX_FRAME_SIZE) stacktrace.add("  at " + string);
            else stacktrace.add("  at " + string.substring(0, MAX_FRAME_SIZE) + "...");
        }
        if (traces > 0 && traces < list.size()) {
            stacktrace.add("  ... " + (list.size() - traces) + " more");
        } else {
            final var i = elements.length - list.size();
            if (i > 0) stacktrace.add("  ... " + i + " more");
        }
    }

    private static List<String> collapseStackTrace(final StackTraceElement[] trace) {
        final var lines = Arrays.stream(trace)
                .map(StackTraceElement::toString)
                .toList();

        return collapseRepeatingPattern(lines);
    }

    private static List<String> collapseRepeatingPattern(final List<String> lines) {
        final var deduplicated = collapseConsecutiveDuplicates(lines);

        final var n = deduplicated.size();

        for (var cycleLen = 1; cycleLen <= n / 2; cycleLen++) {
            var isPattern = true;
            var repetitions = 0;

            for (var i = 0; i < n; i++) {
                if (!deduplicated.get(i).equals(deduplicated.get(i % cycleLen))) {
                    isPattern = false;
                    break;
                }
                if (i > 0 && i % cycleLen == 0) repetitions++;
            }

            if (isPattern && repetitions >= 2) {
                return deduplicated.subList(0, cycleLen);
            }
        }

        return deduplicated;
    }

    private static List<String> collapseConsecutiveDuplicates(final List<String> lines) {
        if (lines.isEmpty()) return lines;

        final var result = new ArrayList<String>();
        String previous = null;

        for (final var line : lines) {
            if (line.equals(previous)) continue;
            result.add(line);
            previous = line;
        }

        return result;
    }

    public static boolean isSameLoader(final Thread thread, final ClassLoader loader, final Throwable error) {
        return isSameLoader(thread, loader, error, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean isSameLoader(final Thread thread, final ClassLoader loader, @Nullable final Throwable error, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return false;

        final var stackTrace = error.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0)
            return isSameLoader(thread, loader, error.getCause(), visited);

        final var firstNonLibraryIndex = findFirstNonLibraryFrameIndex(stackTrace);
        if (firstNonLibraryIndex == -1) return isSameLoader(thread, loader, error.getCause(), visited);

        final var framesToCheck = Math.min(5, stackTrace.length - firstNonLibraryIndex);

        for (var i = 0; i < framesToCheck; i++) {
            final var frame = stackTrace[firstNonLibraryIndex + i];
            if (isLibraryFrame(frame.getClassName())) continue;
            if (!isFromLoader(frame, loader)) return isSameLoader(thread, loader, error.getCause(), visited);
        }

        return classLoadersMatch(thread.getContextClassLoader(), loader);
    }

    private static boolean classLoadersMatch(@Nullable final ClassLoader first, @Nullable final ClassLoader second) {
        if (Objects.equals(first, second)) return true;
        if (first == null || second == null) return false;
        if (classLoadersMatch(first.getParent(), second)) return true;
        if (classLoadersMatch(first, second.getParent())) return true;
        return false;
    }

    private static int findFirstNonLibraryFrameIndex(final StackTraceElement[] stackTrace) {
        for (var i = 0; i < stackTrace.length; i++) {
            if (!isLibraryFrame(stackTrace[i].getClassName())) return i;
        }
        return -1;
    }

    static boolean isLibraryFrame(final String frame) {
        return frame.startsWith("java.")
                || frame.startsWith("javax.")
                || frame.startsWith("sun.")
                || frame.startsWith("com.sun.")
                || frame.startsWith("jdk.");
    }

    private static boolean isFromLoader(final StackTraceElement frame, final ClassLoader loader) {
        try {
            final var clazz = Class.forName(frame.getClassName(), false, loader);
            return isSameClassLoader(clazz.getClassLoader(), loader);
        } catch (final Throwable t) {
            return false;
        }
    }

    private static boolean isSameClassLoader(final ClassLoader classLoader, final ClassLoader loader) {
        if (classLoader == loader) return true;
        var current = classLoader;
        while (current != null && current != loader) {
            current = current.getParent();
        }
        return loader == current;
    }

    private static @Nullable String findFirstMessage(final TrackedError.@Nullable ThrowableSnapshot error) {
        if (error == null) return null;
        final var message = error.message();
        if (message != null) return message;
        return findFirstMessage(error.cause());
    }

    private static @Nullable String anonymize(@Nullable final String message, final List<Map.Entry<Pattern, String>> customPatterns) {
        if (message == null) return null;
        var truncated = message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH) + "..."
                : message;
        for (final var entry : customPatterns) {
            truncated = entry.getKey().matcher(truncated).replaceAll(entry.getValue());
        }
        return truncated;
    }

    private static List<Map.Entry<Pattern, String>> defaultAnonymizationEntries() {
        final var entries = new ArrayList<>(List.of(
                Map.entry(ipv4Pattern(), "[IP hidden]"),
                Map.entry(ipv6Pattern(), "[IP hidden]"),
                Map.entry(userHomePathPattern(), "$1$2$3[username hidden]"),
                Map.entry(discordWebhookPattern(), "$1[token hidden]"),
                Map.entry(jdbcUrlPattern(), "$1[password hidden]$2")
        ));
        usernamePattern().ifPresent(pattern -> entries.add(Map.entry(pattern, "[username hidden]")));
        return entries;
    }

    private static Pattern discordWebhookPattern() {
        return Pattern.compile("(https://discord\\.com/api/webhooks/\\d+/)[\\w-]+");
    }

    private static Pattern ipv4Pattern() {
        return Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b");
    }

    private static Pattern ipv6Pattern() {
        return Pattern.compile("(?i)\\b([0-9a-f]{1,4}:){7}[0-9a-f]{1,4}\\b|" + // Full form
                "(?i)\\b([0-9a-f]{1,4}:){1,7}:\\b|" +                          // Trailing ::
                "(?i)\\b([0-9a-f]{1,4}:){1,6}:[0-9a-f]{1,4}\\b|" +             // :: in middle (1 group after)
                "(?i)\\b([0-9a-f]{1,4}:){1,5}(:[0-9a-f]{1,4}){1,2}\\b|" +      // :: in middle (2 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,4}(:[0-9a-f]{1,4}){1,3}\\b|" +      // :: in middle (3 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,3}(:[0-9a-f]{1,4}){1,4}\\b|" +      // :: in middle (4 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,2}(:[0-9a-f]{1,4}){1,5}\\b|" +      // :: in middle (5 groups after)
                "(?i)\\b[0-9a-f]{1,4}:(:[0-9a-f]{1,4}){1,6}\\b|" +             // :: in middle (6 groups after)
                "(?i)\\b:(:[0-9a-f]{1,4}){1,7}\\b|" +                          // Leading ::
                "(?i)\\b::([0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4}\\b|" +            // :: at start
                "(?i)\\b::\\b");                                               // Just ::
    }

    private static Pattern jdbcUrlPattern() {
        return Pattern.compile("(jdbc:[^:]+://[^:]+:(?:\\d+:)?)[^@]+(@)");
    }

    private static Pattern userHomePathPattern() {
        return Pattern.compile("(/home/)[^/\\s]+" +       // Linux: /home/username
                "|(/Users/)[^/\\s]+" +                    // macOS: /Users/username
                "|((?i)[A-Z]:\\\\Users\\\\)[^\\\\\\s]+"); // Windows: A-Z:\\Users\\username
    }

    private static Optional<Pattern> usernamePattern() {
        return Optional.ofNullable(System.getProperty("user.name"))
                .filter(s -> s.trim().length() > 2)
                .filter(s -> !allowedNames.contains(s.toLowerCase(Locale.ROOT)))
                .map(Pattern::quote)
                .map(s -> Pattern.compile(s, Pattern.CASE_INSENSITIVE));
    }
}

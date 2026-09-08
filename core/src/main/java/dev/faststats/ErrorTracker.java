package dev.faststats;

import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * An error tracker.
 *
 * @since 0.24.0
 */
public sealed interface ErrorTracker permits SimpleErrorTracker {
    /**
     * Creates a context-aware error tracker policy for the current class loader.
     *
     * @return the error tracker policy
     * @see #contextAware(ClassLoader)
     * @since 0.24.0
     */
    @Contract(value = " -> new", pure = true)
    static ErrorTracker contextAware() {
        return contextAware(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a context-aware error tracker policy for the given class loader.
     * <p>
     * The returned tracker has its error context attached immediately. If the class
     * loader is {@code null}, the tracker will track all errors.
     *
     * @param classLoader the class loader whose errors should be tracked, or {@code null} to track all errors
     * @return the error tracker policy
     * @throws IllegalStateException if the error context is already attached
     * @see #attachErrorContext(ClassLoader)
     * @since 0.24.0
     */
    @Contract(value = "_ -> new", pure = true)
    static ErrorTracker contextAware(@Nullable final ClassLoader classLoader) {
        final var tracker = new SimpleErrorTracker();
        tracker.attachErrorContext(classLoader);
        return tracker;
    }

    /**
     * Creates a context-unaware error tracker policy.
     *
     * @return the error tracker policy
     * @since 0.24.0
     */
    @Contract(value = " -> new", pure = true)
    static ErrorTracker contextUnaware() {
        return new SimpleErrorTracker();
    }

    /**
     * Returns the global error context attributes configured for this tracker.
     *
     * @return the global error context attributes
     * @since 0.24.0
     */
    @Contract(pure = true)
    Attributes getAttributes();

    /**
     * Tracks a handled  error.
     *
     * @param message the error message
     * @return a new mutable tracked error
     * @see #trackError(Throwable)
     * @since 0.24.0
     */
    @Contract(mutates = "this")
    TrackedError trackError(String message);

    /**
     * Tracks a handled error.
     *
     * @param error the error
     * @return a new mutable tracked error
     * @since 0.24.0
     */
    @Contract(mutates = "this")
    TrackedError trackError(Throwable error);

    /**
     * Adds an error type that will not be reported to FastStats.
     * <p>
     * Matching is done exactly. If, for example {@link LinkageError} was ignored,
     * {@link NoClassDefFoundError} would still be reported, even though it extends {@link LinkageError}
     *
     * @param type the error type
     * @return the error tracker
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    ErrorTracker ignoreError(Class<? extends Throwable> type);

    /**
     * Adds a pattern that will be matched against all error messages.
     * <p>
     * If an error's message matches the given pattern, it will not be reported to FastStats.
     * <pre>{@code
     * // Exact match
     * tracker.ignoreError(Pattern.compile("No space left on device"));
     *
     * // Regex match
     * tracker.ignoreError(Pattern.compile("No serializer for: class .*"));
     * }</pre>
     *
     * @param pattern the regex pattern to match against error messages
     * @return the error tracker
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    ErrorTracker ignoreError(Pattern pattern);

    /**
     * Adds a pattern that will be matched against all error messages.
     * <p>
     * If an error's message matches the given pattern, it will not be reported to FastStats.
     *
     * @param pattern the regex pattern string to match against error messages
     * @return the error tracker
     * @see #ignoreError(Pattern)
     * @since 0.24.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    default ErrorTracker ignoreError(@RegExp final String pattern) {
        return ignoreError(Pattern.compile(pattern));
    }

    /**
     * Adds an error type combined with a message pattern that will not be reported to FastStats.
     * <p>
     * An error is ignored only if its class matches the given type exactly and its message matches the given pattern.
     * <pre>{@code
     * tracker.ignoreError(IOException.class, Pattern.compile("No space left on device"));
     * }</pre>
     *
     * @param type    the error type
     * @param pattern the regex pattern to match against error messages
     * @return the error tracker
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    ErrorTracker ignoreError(Class<? extends Throwable> type, Pattern pattern);

    /**
     * Adds an error type combined with a message pattern that will not be reported to FastStats.
     * <p>
     * An error is ignored only if its class matches the given type exactly and its message matches the given pattern.
     *
     * @param type    the error type
     * @param pattern the regex pattern string to match against error messages
     * @return the error tracker
     * @see #ignoreError(Class, Pattern)
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default ErrorTracker ignoreError(final Class<? extends Throwable> type, @RegExp final String pattern) {
        return ignoreError(type, Pattern.compile(pattern));
    }

    /**
     * Adds an anonymization pattern that replaces matched text in error messages.
     * <pre>{@code
     * tracker.anonymize(Pattern.compile("token=[^&]+"), "token=[redacted]");
     * }</pre>
     *
     * @param pattern     the regex pattern to match
     * @param replacement the replacement string
     * @return the error tracker
     * @see java.util.regex.Matcher#replaceAll(String)
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    ErrorTracker anonymize(Pattern pattern, String replacement);

    /**
     * Adds an anonymization pattern that replaces matched text in error messages.
     *
     * @param pattern     the regex pattern string to match
     * @param replacement the replacement string
     * @return the error tracker
     * @see #anonymize(Pattern, String)
     * @see java.util.regex.Matcher#replaceAll(String)
     * @since 0.24.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default ErrorTracker anonymize(@RegExp final String pattern, final String replacement) {
        return anonymize(Pattern.compile(pattern), replacement);
    }

    /**
     * Attaches an error context to the tracker.
     * <p>
     * If the class loader is {@code null}, the tracker will track all errors.
     *
     * @param loader the class loader
     * @throws IllegalStateException if the error context is already attached
     * @since 0.23.0
     */
    void attachErrorContext(@Nullable ClassLoader loader) throws IllegalStateException;

    /**
     * Detaches the error context from the tracker.
     * <p>
     * This restores the original uncaught exception handler that was in place before
     * {@link #attachErrorContext(ClassLoader)} was called.
     * <p>
     * This should be called during shutdown to prevent {@link BootstrapMethodError}
     * when the provider's JAR file is closed.
     *
     * @since 0.23.0
     */
    void detachErrorContext();

    /**
     * Returns whether an error context is attached.
     *
     * @return whether an error context is attached
     * @since 0.23.0
     */
    boolean isContextAttached();

    /**
     * Sets the error event handler which will be called when an error is tracked automatically.
     * <p>
     * The purpose of this handler is to allow custom error handling like logging.
     *
     * @param errorEvent the error event handler
     * @since 0.23.0
     */
    @Contract(mutates = "this")
    void setContextErrorHandler(@Nullable BiConsumer<@Nullable ClassLoader, Throwable> errorEvent);

    /**
     * Returns the error event handler which will be called when an error is tracked automatically.
     *
     * @return the error event handler
     * @since 0.23.0
     */
    @Contract(pure = true)
    Optional<BiConsumer<@Nullable ClassLoader, Throwable>> getContextErrorHandler();

    /**
     * Checks if the error occurred in the same class loader as the provided loader.
     *
     * @param loader the class loader
     * @param error  the error
     * @return whether the error occurred in the same class loader
     * @since 0.23.0
     */
    @Contract(pure = true)
    static boolean isSameLoader(final ClassLoader loader, final Throwable error) {
        return ErrorHelper.isSameLoader(Thread.currentThread(), loader, error);
    }

    /**
     * Checks if the error occurred in the same class loader as the provided loader and thread.
     *
     * @param thread the thread
     * @param loader the class loader
     * @param error  the error
     * @return whether the error occurred in the same class loader
     * @since 0.23.0
     */
    @Contract(pure = true)
    static boolean isSameLoader(final Thread thread, final ClassLoader loader, final Throwable error) {
        return ErrorHelper.isSameLoader(thread, loader, error);
    }
}    

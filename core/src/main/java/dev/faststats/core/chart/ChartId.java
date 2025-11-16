package dev.faststats.core.chart;

import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * An annotation to mark a chart id.
 *
 * @since 0.1.0
 */
@Pattern(ChartId.PATTERN)
@Retention(RetentionPolicy.CLASS)
@Target({METHOD, FIELD, PARAMETER, LOCAL_VARIABLE})
public @interface ChartId {
    String PATTERN = "[a-z_]+";
}

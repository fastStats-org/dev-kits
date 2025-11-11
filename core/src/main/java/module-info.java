import org.jspecify.annotations.NullMarked;

@NullMarked
module org.faststats.core {
    exports org.faststats.chart;
    exports org.faststats;

    requires com.google.gson;
    requires java.net.http;

    requires static org.jspecify;
    requires org.jetbrains.annotations;
}
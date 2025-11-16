import org.jspecify.annotations.NullMarked;

@NullMarked
module org.faststats.core {
    exports org.faststats.chart;
    exports org.faststats;

    requires com.github.luben.zstd_jni;
    requires com.google.gson;
    requires java.net.http;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
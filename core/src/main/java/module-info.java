import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.core {
    exports dev.faststats.chart;
    exports dev.faststats;

    requires com.github.luben.zstd_jni;
    requires com.google.gson;
    requires java.net.http;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
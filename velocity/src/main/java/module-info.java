import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.velocity {
    exports dev.faststats.velocity;

    requires com.google.gson;
    requires com.google.guice;
    requires com.velocitypowered.api;
    requires dev.faststats.core;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
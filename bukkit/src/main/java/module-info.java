import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.bukkit {
    exports dev.faststats.bukkit;

    requires com.google.gson;
    requires dev.faststats.core;
    requires java.logging;
    requires org.bukkit;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
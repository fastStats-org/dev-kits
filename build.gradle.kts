plugins {
    id("java")
    id("java-library")
}

group = "org.faststats"
version = "0.1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.compileJava {
    options.release.set(21)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi("com.google.code.gson:gson:2.11.0")
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
}

plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

group = "org.faststats.metrics"
version = "0.1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
    withJavadocJar()
}

tasks.compileJava {
    options.release.set(21)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi("com.google.code.gson:gson:2.13.1")
    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation("com.google.code.gson:gson:2.13.1")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
    repositories.maven {
        val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
        url = uri("https://repo.thenextlvl.net/$channel")
        credentials {
            username = System.getenv("REPOSITORY_USER")
            password = System.getenv("REPOSITORY_TOKEN")
        }
    }
}
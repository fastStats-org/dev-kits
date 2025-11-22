plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

group = "dev.faststats.metrics"
version = "0.1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
    withJavadocJar()
}

tasks.compileJava {
    options.release.set(21)
}

tasks.test {
    dependsOn(tasks.javadoc)
}

tasks.javadoc {
    val options = options as StandardJavadocDocletOptions
    options.tags("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:")
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.github.luben:zstd-jni:1.5.7-6")
    
    compileOnlyApi("com.google.code.gson:gson:2.13.2")
    compileOnlyApi("org.jetbrains:annotations:26.0.2-1")
    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation("com.google.code.gson:gson:2.13.2")
    
    testImplementation(platform("org.junit:junit-bom:6.1.0-M1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showCauses = true
        showExceptions = true
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "core"
        groupId = "dev.faststats.metrics"
        pom.url.set("https://faststats.dev/docs")
        pom.scm {
            val repository = "faststats-dev/dev-kits"
            url.set("https://github.com/$repository")
            connection.set("scm:git:git://github.com/$repository.git")
            developerConnection.set("scm:git:ssh://github.com/$repository.git")
        }
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
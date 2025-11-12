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

tasks.test {
    dependsOn(tasks.javadoc)
}

tasks.javadoc {
    val options = options as StandardJavadocDocletOptions
    options.tags("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "bukkit"
        groupId = "org.faststats.metrics"
        pom.url.set("https://faststats.org/docs")
        pom.scm {
            val repository = "fastStats-org/dev-kits"
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
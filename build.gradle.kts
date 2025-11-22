subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "dev.faststats.metrics"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(21)
        withSourcesJar()
        withJavadocJar()
    }

    tasks.named<JavaCompile>("compileJava") {
        options.release.set(21)
    }

    tasks.named<Test>("test") {
        dependsOn(tasks.named("javadoc"))
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showCauses = true
            showExceptions = true
        }
    }

    tasks.named<Javadoc>("javadoc") {
        val options = options as StandardJavadocDocletOptions
        options.tags("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:")
    }

    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") {
                artifactId = project.name
                groupId = "dev.faststats.metrics"

                pom {
                    url.set("https://faststats.dev/docs")
                    scm {
                        val repository = "faststats-dev/dev-kits"
                        url.set("https://github.com/$repository")
                        connection.set("scm:git:git://github.com/$repository.git")
                        developerConnection.set("scm:git:ssh://github.com/$repository.git")
                    }
                }

                from(components["java"])
            }

            repositories {
                maven {
                    val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
                    url = uri("https://repo.thenextlvl.net/$channel")
                    credentials {
                        username = System.getenv("REPOSITORY_USER")
                        password = System.getenv("REPOSITORY_TOKEN")
                    }
                }
            }
        }
    }
}

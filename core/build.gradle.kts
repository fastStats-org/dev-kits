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

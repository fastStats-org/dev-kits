repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    compileOnly("com.velocitypowered:velocity-api:4.0.0-SNAPSHOT")
}

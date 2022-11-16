
// The property assigning the environment to run
val runEnv =
    if (project.hasProperty("runEnv"))
        project.properties["runEnv"]
    else if (System.getenv().containsKey("runEnv"))
        System.getenv()["runEnv"]
    else if (System.getProperties().containsKey("runEnv"))
        System.getProperties()["runEnv"]
    else "local"

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.plugin.spring")
    application
    `maven-publish`
}

group = "com.ai4traders"
version = "0.1-DEVEL"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://mvnrepository.com")
    }
}

application {
    mainClass.set("com.ai4traders.botella.Application")
}

java {
    withSourcesJar()
}

dependencies {
    // Kotlin related dependencies
    api(libs.botellaCommon) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-slf4j")
    }
    api(libs.kotlinxCoroutines)
    api(libs.kotlinxCoroutinesJvm)
    api(libs.kotlinxCoroutinesJdk8)
    api(libs.kotlinxCoroutinesSlf4j)
}

sourceSets {
    main {
        java {
            srcDir("${buildDir.absolutePath}/generated/ksp/main/java")
        }
        kotlin {
            srcDir("${buildDir.absolutePath}/generated/ksp/main/kotlin")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("Botella") {
            artifactId = "Botella"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.bootRun.configure {
    systemProperty("spring.profiles.active", "${runEnv}")
}

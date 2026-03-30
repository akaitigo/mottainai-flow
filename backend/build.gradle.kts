plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.allopen") version "2.0.21"
    id("io.quarkus") version "3.17.5"
    id("com.google.protobuf") version "0.9.4"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.jmailen.kotlinter") version "4.4.1"
}

repositories {
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-arc")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("io.grpc:grpc-protobuf:1.64.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.5")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.13")
}

group = "com.mottainai"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.64.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
            // Use protobuf-java 3.25.5 compatible output
            it.builtins.named("java")
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("../proto")
        }
    }
}

detekt {
    config.setFrom(files("detekt.yml"))
    buildUponDefaultConfig = true
}

// Skip Quarkus application packaging in dev (requires running DB).
// Full Quarkus build is done via Docker Compose (Issue #5).
tasks.named("quarkusBuild") { enabled = false }
tasks.named("quarkusAppPartsBuild") { enabled = false }
tasks.named("quarkusDependenciesBuild") { enabled = false }

// Exclude generated proto sources from kotlinter lint/format
tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask> {
    source = source.filter {
        !it.path.contains("/generated/") && !it.path.contains("/build/")
    }.asFileTree
}
tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask> {
    source = source.filter {
        !it.path.contains("/generated/") && !it.path.contains("/build/")
    }.asFileTree
}

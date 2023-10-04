import java.net.URI

plugins {
    java
    id("com.google.protobuf") version "0.9.3"
    id("com.jfrog.artifactory") version "5.1.0"
    id("maven-publish")
}

val grpcVersion = "1.55.1"
val protobufVersion = "3.23.2"

val buffGenerateDir = "generated"
val currentVersion: String by project
val artifactoryContext = findProperty("artifactoryContext").toString()


allprojects {
    apply(plugin = "com.jfrog.artifactory")


    repositories {
        apply(plugin = "com.jfrog.artifactory")
        group = findProperty("group").toString()
        version = findProperty("version").toString()
        status = "Integration"
        maven {
            url = URI("http://127.0.0.1:8081/artifactory/libs-release")
            credentials {
                username = findProperty("artifactory_user").toString()
                password = findProperty("artifactory_password").toString()
            }
        }

    }
}

buildscript {
    repositories {
        mavenCentral()

    }
    dependencies {
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4+")
    }
}

fun javaProjects() = subprojects.filter {
    File(it.projectDir, "src").isDirectory
}

configure(javaProjects()) {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("mavenJava") {
                from(components.getByName("java"))
                artifact("$buildDir/libs") {
                    extension = "jar"
                }
                group = "br.com.shire42"
                artifactId="proto-service"
            }
        }
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}

artifactory {
    setContextUrl(artifactoryContext)


    publish {

        repository {
            repoKey = "gradle-dev"
            username = findProperty("artifactory_user").toString()
            password = findProperty("artifactory_password").toString()
        }
        defaults {


            publications("mavenJava")
            setPublishArtifacts(true)
            setProperty("publishArtifacts", true)
            setPublishPom(true)
        }
    }
}

dependencies {
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}


sourceSets["main"].java { srcDir("$buildDir/$buffGenerateDir/java") }



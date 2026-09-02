plugins {
    `java-library`
    `maven-publish`
}

providers.gradleProperty("publicationGroup").orNull?.let { group = it }
version = providers.gradleProperty("publicationVersion").getOrElse("0.1.0-SNAPSHOT")

description = "Reusable Java lint rules"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

dependencies {
    compileOnly("com.puppycrawl.tools:checkstyle:13.9.0")

    testImplementation("com.puppycrawl.tools:checkstyle:13.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "java-lint"
                description = project.description
                url = "https://github.com/LarsEckart/java-lint"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "LarsEckart"
                        name = "Lars Eckart"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/LarsEckart/java-lint.git"
                    developerConnection = "scm:git:ssh://github.com/LarsEckart/java-lint.git"
                    url = "https://github.com/LarsEckart/java-lint"
                }
            }
        }
    }
}

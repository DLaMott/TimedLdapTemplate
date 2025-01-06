plugins {
    id("java")
    id("maven-publish")

}

java {
    withJavadocJar()
    withSourcesJar()
}

group = "io.github.dlamott"  // Update group to your GitHub namespace
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.data:spring-data-commons:3.4.0")
    implementation("org.springframework.ldap:spring-ldap-core:3.2.8")
    implementation("org.projectlombok:lombok:1.18.36")
    implementation("com.google.guava:guava:31.1-jre")

    // UnboundID LDAP SDK for the example and testing
    testImplementation("com.unboundid:unboundid-ldapsdk:6.0.8")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "io.github.dlamott"
            artifactId = "timed-ldap-template"
            version = "1.0-SNAPSHOT"

            // POM file configuration
            pom {
                name.set("Timed LDAP Template")
                description.set("A library that extends Spring LDAP Template with metrics tracking.")
                url.set("https://github.com/DLaMott/TimedLdapTemplate") // Replace with your project URL
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("dlamott")
                        name.set("Dylan")
                        email.set("dylanlamott@gmail.com") // Replace with your email
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/DLaMott/TimedLdapTemplate.git") // Replace with your repository URL
                    developerConnection.set("scm:git:ssh://github.com/DLaMott/TimedLdapTemplate.git") // Replace with your repository URL
                    url.set("https://github.com/DLaMott/TimedLdapTemplate") // Replace with your repository URL
                }
            }
        }

        repositories {
            maven {
                name = "SonatypeOSS"
                url = uri("https://central.sonatype.com/api/v1/publisher/upload")
                credentials {
                    username = (project.findProperty("sonatypeUsername") ?: System.getenv("SONATYPE_USERNAME")).toString()
                    password = (project.findProperty("sonatypePassword") ?: System.getenv("SONATYPE_PASSWORD")).toString()
                }
            }
        }
    }
}



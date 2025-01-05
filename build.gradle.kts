plugins {
    id("java")
}

group = "org.ldap"
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
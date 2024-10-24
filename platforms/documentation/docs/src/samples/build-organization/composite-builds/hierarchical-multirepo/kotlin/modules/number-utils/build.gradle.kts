plugins {
    `java-library`
    `maven-publish`
}

group = "org.sample"
version = "1.0"

publishing {
    repositories {
        maven {
            url = file("../../local-repo")
        }
    }
    publications {
        create<MavenPublication>("maven") { from(components["java"]) }
    }
}

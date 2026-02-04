plugins {
    id("java")
}

group = "me.beanes"
version = "1.3"


repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "codemc-release"
        url = uri("https://repo.codemc.io/repository/maven-releases/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        url = uri("https://maven.elmakers.com/repository")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.2")
    compileOnly("io.netty:netty-all:4.1.72.Final")
}

tasks.test {
    useJUnitPlatform()
}

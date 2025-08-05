plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.com.github.johnrengelman.shadow)
    alias(libs.plugins.io.papermc.paperweight.userdev)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
    maven {
        url = uri("https://gitlab.com/api/v4/groups/5826166/-/packages/maven")
    }
}

dependencies {
    paperweight.paperDevBundle(libs.versions.io.papermc.paper.paper.api)
    compileOnly(libs.net.mineclick.core.messenger)
    compileOnly(libs.net.mineclick.core.global)
    compileOnly(libs.com.comphenix.protocol.protocollib)
    compileOnly(libs.org.projectlombok.lombok)
    annotationProcessor(libs.org.projectlombok.lombok)
}

paperweight.reobfArtifactConfiguration.set(io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION)

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    assemble {
        dependsOn(reobfJar)
    }
    shadowJar {
        finalizedBy(reobfJar)
        archiveFileName.set("Game-unmapped.jar")
    }
    reobfJar {
        outputJar.set(layout.buildDirectory.file("libs/Game.jar"))
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(tasks.named("jar")) {
            classifier = ""
        }
        artifact(tasks.named("reobfJar")) {
            classifier = "remapped"
        }
    }
    repositories {
        maven {
            name = "gitlab-maven"
            url = uri("https://gitlab.com/api/v4/projects/13759867/packages/maven")

            //val tokenProperty = findProperty("mineclickGitlabToken") as String?
            val ciJobToken: String? = System.getenv("CI_JOB_TOKEN")

            //(tokenProperty ?: ciJobToken)?.let {
            ciJobToken?.let {
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = it
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
}
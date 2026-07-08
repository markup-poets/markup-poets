pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "markup-poet-kmp"
include(":markup-table")
include(":markup-document")
include(":markup-asciidoc-writer")
include(":markup-markdown-writer")
include(":markup-graph")
include(":markup-dot-writer")

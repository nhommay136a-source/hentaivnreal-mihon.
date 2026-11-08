pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}
rootProject.name = "HentaiVNReal"
include(":src:vi:hentaivnreal")

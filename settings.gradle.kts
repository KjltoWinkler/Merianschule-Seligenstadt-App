rootProject.name = "MerianschuleSeligenstadtApp"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

if (file("androidApp").exists()) {
    include(":androidApp")
}
if (file("desktopApp").exists()) {
    include(":desktopApp")
}
if (file("webApp").exists()) {
    include(":webApp")
}
include(":shared")
if (file("server").exists()) {
    include(":server")
}
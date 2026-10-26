import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop") {
        mainRun {
            mainClass.set("de.kjltowinkler.msapp.desktop.MainKt")
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "de.kjltowinkler.msapp.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Merianschule"
            packageVersion = "1.0.0"
            description = "Merianschule Seligenstadt App"
            copyright = "© 2026 Merianschule Seligenstadt"
            vendor = "Merianschule Seligenstadt"

            linux {
                packageName = "merianschule"
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.png"))
                shortcut = true
                menuGroup = "Education"
                appCategory = "Education"
            }
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.ico"))
                menu = true
                menuGroup = "Merianschule Seligenstadt"
                upgradeUuid = "b97950c0-1c39-44be-bdf1-bfaae4c0eb51"
                dirChooser = true
                shortcut = true
                perUserInstall = true
            }
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.icns"))
                bundleID = "de.kjltowinkler.msapp"
                dockName = "Merianschule"
            }
        }
    }
}

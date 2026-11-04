import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
//    kotlin("multiplatform") version "2.1.0"
//    id("com.android.library") version "8.9.1"
//    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21" // Optional, if you need serialization
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
//    id("maven-publish")
    id("build-sobo-resources")
}

soboStringResourcesConfig {
    resourcesDir.set("src/commonMain/composeResources")
    outputDir.set(layout.buildDirectory.dir("generated/source/kmp/main/kotlin/com/krzysobo/sobomobilelib/generated").get().asFile.path)
    resourcesOutputPackage.set("com.krzysobo.sobomobilelib.generated")
}

group = "com.krzysobo"
version = "1.0.0"

kotlin {
    // AGP 9: Android target provided by com.android.kotlin.multiplatform.library
    android {
        namespace = "com.krzysobo.sobomobilelib"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // needed for Compose Multiplatform resources (composeResources) and Android res/
        androidResources {
            enable = true
        }
    }

//    androidTarget {
////        publishLibraryVariants("release")
//    }

    jvm("desktop")

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.components.ui.tooling.preview)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.compose.material.icons.extended)  // icons for desktop/others
                implementation(libs.multiplatform.settings)
                implementation("com.google.accompanist:accompanist-drawablepainter:0.37.3")
                kotlin.srcDir(layout.buildDirectory.dir("generated/source/kmp/main/kotlin"))
                implementation(project(":soboAppTpl", ""))
                // Add other common dependencies (e.g., kotlinx.serialization if needed)
            }
        }
        getByName("androidMain") {
            dependencies {
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.activity.compose)

                // Android-specific dependencies, if any
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.compose.material.icons.extended)  // icons for desktop/others

                // Desktop-specific dependencies, if any
            }
        }
    }
}

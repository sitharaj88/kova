plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Export Kova + the multiplatform ViewModel so Swift sees real types
            // (StateViewModel, NativeStateFlow, ViewModelHost, ...) instead of opaque ones.
            export("in.sitharaj.kova:kova-core:0.1.0")
            export("in.sitharaj.kova:kova-viewmodel:0.1.0")
            export(libs.androidx.lifecycle.viewmodel)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("in.sitharaj.kova:kova-core:0.1.0")
            api("in.sitharaj.kova:kova-viewmodel:0.1.0")
            api("in.sitharaj.kova:kova-annotations:0.1.0")
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

// Kova codegen: generates <flow>Native accessors for @NativeExport ViewModels,
// only where they are needed — the iOS compilations.
dependencies {
    add("kspIosArm64", "in.sitharaj.kova:kova-ksp:0.1.0")
    add("kspIosSimulatorArm64", "in.sitharaj.kova:kova-ksp:0.1.0")
    add("kspIosX64", "in.sitharaj.kova:kova-ksp:0.1.0")
}

android {
    namespace = "com.example.tasks.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
}

android {
    namespace = "sitharaj.kova.annotations"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

mavenPublishing {
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
    }
}

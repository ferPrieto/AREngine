import prieto.fernando.android.plugin.BuildType
import prieto.fernando.android.plugin.androidPluginId

plugins {
    id("com.android.application")
    id("prieto.fernando.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
}

androidPlugin {
    buildType = BuildType.App
}

android {
    defaultConfig {
        applicationId = androidPluginId()
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
    }
    sourceSets {
        getByName("main").java.srcDirs("build/generated/source/navigation-args")
    }
}

dependencies {
    implementation(project(ProjectModules.core))
    implementation(project(ProjectModules.navigation))
    implementation(project(ProjectModules.presentation))
    implementation(project(ProjectModules.arEngineCommon))
    implementation(project(ProjectModules.rendering))

    implementation(Dependencies.AndroidX.fragmentKtx)
    implementation(Dependencies.AndroidX.coreKtx)
    implementation(Dependencies.AndroidX.lifecycleLivedataKtx)
    annotationProcessor(Dependencies.AndroidX.lifecycleCompiler)
    implementation(Dependencies.AndroidX.archComponents)

    implementation(Dependencies.AndroidX.constraintlayout)
    implementation(Dependencies.AndroidX.legacySupport)
    implementation(Dependencies.AndroidX.Navigation.fragmentKtx)
    implementation(Dependencies.AndroidX.Navigation.uiKtx)

    implementation(Dependencies.Huawei.arEngine)

    androidTestImplementation(TestDependencies.AndroidX.runner)
    androidTestImplementation(TestDependencies.AndroidX.rules)
    androidTestImplementation(TestDependencies.AndroidX.core)
    androidTestImplementation(TestDependencies.AndroidX.coreKtx)
}

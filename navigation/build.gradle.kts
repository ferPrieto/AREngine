plugins {
    id("com.android.library")
    id("prieto.fernando.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    sourceSets {
        getByName("main").java.srcDirs("build/generated/source/navigation-args")
    }
}

dependencies {
    implementation(Dependencies.AndroidX.Navigation.fragmentKtx)
    implementation(Dependencies.AndroidX.Navigation.uiKtx)
}

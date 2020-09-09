plugins {
    id("com.android.library")
    id("prieto.fernando.android.plugin")
}

dependencies {
    implementation(project(ProjectModules.arSdk))
    implementation(Dependencies.AndroidX.annotation)
    implementation(Dependencies.AndroidX.coreKtx)
}
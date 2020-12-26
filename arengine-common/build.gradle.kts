plugins {
    id("com.android.library")
    id("prieto.fernando.android.plugin")
}

dependencies {
    implementation(Dependencies.AndroidX.annotation)
    implementation(Dependencies.AndroidX.coreKtx)
    implementation(Dependencies.Huawei.arEngine)
}
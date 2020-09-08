plugins {
    id("com.android.library")
    id("prieto.fernando.android.plugin")
}

dependencies {
    implementation(project(":HUAWEI_AR_SDK_v2.10.0"))
    implementation(project(ProjectModules.arEngineCommon))
    implementation(Dependencies.Rendering.javaGlObject)
}
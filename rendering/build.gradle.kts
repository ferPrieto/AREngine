plugins {
    id("com.android.library")
    id("prieto.fernando.android.plugin")
}

dependencies {
    implementation(project(ProjectModules.arEngineCommon))
    implementation(Dependencies.Rendering.javaGlObject)
    implementation(Dependencies.Huawei.arEngine)
}
repositories {
    jcenter()
    google()
    mavenCentral()
    maven {
        url = uri("http://developer.huawei.com/repo/")
    }
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.android.tools.build:gradle:4.0.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")

    implementation(gradleApi())
}

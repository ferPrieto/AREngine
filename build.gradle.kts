buildscript {
    val kotlin_version by extra("1.3.72")
    repositories {
        google()
        jcenter()
        maven {
            url = uri("https://developer.huawei.com/repo/")
        }
    }
    dependencies {
        classpath(BuildDependencies.androidGradle)
        classpath(BuildDependencies.kotlinGradlePlugin)
        classpath(BuildDependencies.hiltGradlePlugin)
        classpath(BuildDependencies.safeArgsPlugin)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven {
            url = uri("https://developer.huawei.com/repo/")
        }
    }
}

task("clean") {
    delete(rootProject.buildDir)
}

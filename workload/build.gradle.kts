plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    ksp(project(":test-processor"))
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}

kotlin {
    sourceSets.main {
        kotlin.srcDirs("build/generated/ksp/main/kotlin", "build/generated/ksp/main/java")
    }
    sourceSets.test {
        kotlin.srcDirs("build/generated/ksp/test/kotlin", "build/generated/ksp/test/java")
    }
}
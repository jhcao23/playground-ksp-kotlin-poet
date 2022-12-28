val kspVersion: String by project
val kotlinVersion: String by project
val javapoetVersion: String by project
val kotlinpoetVersion: String by project

plugins {
    kotlin("jvm")
}

group = "technology.touchmars.ksp"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:javapoet:$javapoetVersion")
    implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinpoetVersion")

    testImplementation(kotlin("test"))
}
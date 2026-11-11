plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":core:model"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}

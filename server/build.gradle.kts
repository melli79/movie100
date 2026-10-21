plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "de.gruetzmann.movie100"
version = "1.0.0"
application {
    mainClass = "de.gruetzmann.movie100.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNeg)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.json)
    implementation(libs.ulid)
    implementation(libs.movie.api)
    implementation(libs.jetbrains.exposed)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
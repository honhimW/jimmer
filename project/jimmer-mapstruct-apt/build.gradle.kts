plugins {
    `kotlin-convention`
}

dependencies {
    api(libs.kotlin.stdlib)
    compileOnly(libs.mapstruct.processor)
    implementation(projects.jimmerCore)
}

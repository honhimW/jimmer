plugins {
    `java-convention`
}

group = "io.github.honhimw"

dependencies {
    implementation(projects.jimmerMapstructApt)
    implementation(projects.jimmerCore)
    implementation(projects.jimmerDtoCompiler)

    implementation(libs.javax.validation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.spring.core)
    implementation(libs.intellij.annotations)
    implementation(libs.javapoet)
}

plugins {
    `kotlin-convention`
}

dependencies {
    api(libs.jspecify)
    implementation(libs.javax.validation.api)
    api(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.mapstruct)

    testImplementation(libs.mapstruct)
    testImplementation(libs.lombok)

    testAnnotationProcessor(projects.jimmerApt)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.mapstruct.processor)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.invalid")
    options.compilerArgs.add("-Ajimmer.generate.dynamic.pojo=true")
}

tasks.register("build_version", Copy::class) {
    val versionParts = (project.version as String).split('.')
    val tokens = mapOf(
        "major" to versionParts[0],
        "minor" to versionParts[1],
        "patch" to versionParts[2],
    )

    var packageName = (project.group as String).replace('.', '/')

    from("src/main/java/${packageName}") {
        include("JimmerVersion.java.in")
        rename { it.replace(".java.in", ".java") }
        filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokens)
    }
    into(layout.buildDirectory.dir("generated/sources/versions/java/main/${packageName}"))
}

sourceSets.main.configure {
    java.srcDir(layout.buildDirectory.dir("generated/sources/versions/java/main"))
}

tasks.maybeCreate("compileJava").dependsOn("build_version")
tasks.maybeCreate("compileKotlin").dependsOn("build_version")
tasks.maybeCreate("sourcesJar").dependsOn("build_version")

group = "app.braydog2010"

patches {
    about {
        name = "BrayDog2010 Patches"
        description = "Patches for Venabox Hub and TikTok 46.2.3, built for Morphe."
        source = "git@github.com:BrayDog2010/morphe-patches.git"
        author = "BrayDog2010"
        contact = "na"
        website = "https://github.com/BrayDog2010/morphe-patches"
        license = "GPLv3"
    }
}

dependencies {
    compileOnly(libs.morphe.patcher)

    // Used by JsonGenerator.
    implementation(libs.gson)

    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
        args(project.version.toString())
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-Xcontext-parameters", "-Xcontext-receivers"))
    }
}
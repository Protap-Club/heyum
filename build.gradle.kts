// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.google.services) apply false
}

tasks.register("verifyNoHardcodedDomain") {
    group = "verification"
    description = "Checks that there are no hardcoded references to heyum.com in the codebase."
    doLast {
        val rootDir = project.rootDir
        val invalidDomain = "heyum.com"
        var foundInvalid = false
        
        rootDir.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "kt" || file.extension == "xml" || file.extension == "swift" || file.name == "Info.plist")) {
                if (!file.absolutePath.contains("/build/") && !file.absolutePath.contains("/.gradle/")) {
                    val content = file.readText()
                    if (content.contains(invalidDomain, ignoreCase = true)) {
                        println("ERROR: Hardcoded domain reference '$invalidDomain' found in file: ${file.absolutePath}")
                        foundInvalid = true
                    }
                }
            }
        }
        if (foundInvalid) {
            throw GradleException("Build failed because hardcoded domain '$invalidDomain' was found. Use BASE_URL or heyum.co instead.")
        } else {
            println("Verification passed: No hardcoded references to '$invalidDomain' found.")
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" || it.name == "preBuild" }.configureEach {
        dependsOn(":verifyNoHardcodedDomain")
    }
}
rootProject.name = "Botella-API-Kraken"

pluginManagement {
    val kotlinVersion: String by settings
    val googleKspVersion: String by settings
    val springbootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val kotlinSpringPluginVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useVersion(kotlinVersion)
                "com.google.devtools.ksp" -> useVersion(googleKspVersion)
                "org.springframework.boot" -> useVersion(springbootVersion)
                "io.spring.dependency-management" -> useVersion(springDependencyManagementVersion)
                "org.jetbrains.kotlin.plugin.spring" -> useVersion(kotlinSpringPluginVersion)
            }
        }
    }
}

enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google { content {
            includeGroupByRegex("androidx\\..*")
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google\\.android.*")
            includeGroupByRegex("com\\.google\\.testing\\.platform.*")
        } }
    }
}
rootProject.name = "InformationTheory"
include(":app")

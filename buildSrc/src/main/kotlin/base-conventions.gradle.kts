plugins {
  id("net.kyori.indra")
  id("net.kyori.indra.checkstyle")
  id("net.kyori.indra.licenser.spotless")
  id("org.jetbrains.gradle.plugin.idea-ext")
}

val rootDirectory = isolated.rootProject.projectDirectory

indraSpotlessLicenser {
  licenseHeaderFile(rootDirectory.file("LICENSE_HEADER"))
}

spotless {
  java {
    importOrderFile(rootDirectory.file(".spotless/kyori.importorder"))
    trimTrailingWhitespace()
    endWithNewline()
    leadingTabsToSpaces(2)
    removeUnusedImports()
  }
}

indra {
  github("PaperMC", "adventure-platform-mod") {
    ci(true)
  }
  javaVersions().target(25)
  mitLicense()
  checkstyle(libs.versions.checkstyle.get())

  signWithKeyFromPrefixedProperties("papermc")
  configurePublications {
    pom {
      developers {
        developer {
          id = "kashike"
          timezone = "America/Vancouver"
        }

        developer {
          id = "lucko"
          name = "Luck"
          url = "https://lucko.me"
          email = "git@lucko.me"
        }

        developer {
          id = "zml"
          name = "zml"
          timezone = "America/Vancouver"
        }

        developer {
          id = "Electroid"
        }

        developer {
          id = "jmp"
          name = "Jason Penilla"
          url = "https://github.com/jpenilla"
        }
      }
    }
  }
}

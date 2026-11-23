import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

val Project.libs: LibrariesForLibs
  get() = extensions.getByType()

fun Project.configureUnstableAdventureStrategy() {
  configurations.configureEach {
    resolutionStrategy.capabilitiesResolution.all {
      if (candidates.size == 2) {
        // Select unstable Paper variant over stable Kyori variant
        val unstable = candidates.find { c -> c.id.displayName.startsWith("io.papermc") }
        val stable = candidates.find { c -> c.id.displayName.startsWith("net.kyori") }
        if (unstable != null && stable != null) {
          select(unstable)
        }
      }
    }
  }
}

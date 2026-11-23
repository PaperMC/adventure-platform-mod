import java.time.Duration
import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RunGameTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask

plugins {
  id("net.fabricmc.fabric-loom")
  alias(libs.plugins.configurateTransformations)
  id("standard-conventions")
  id("mod-publishing-conventions")
}

publishMods.modrinth {
  file = tasks.jar.flatMap { it.archiveFile }
  modLoaders = listOf("fabric")
  requires("fabric-api")
}

dependencies {
  vineflowerDecompilerClasspath(libs.vineflower)
  sequenceOf<(Any) -> Dependency?>(
    ::implementation, ::api, ::compileOnly
  ).forEach { it(platform(fabricApiLibs.bom)) }
  api(fabricApiLibs.base)
  implementation(fabricApiLibs.networking.api.v1)
  implementation(fabricApiLibs.command.api.v2)

  minecraft(libs.minecraft)
  implementation(libs.fabric.loader)

  testImplementation(libs.fabric.loader.junit)

  api(project(":adventure-platform-mod-shared"))
  include(project(":adventure-platform-mod-shared"))
}

configurations {
  include {
    extendsFrom(configurations.jarInJar.get())
  }
  runtimeClasspath {
    extendsFrom(vineflowerDecompilerClasspath.get())
  }
}

sourceSets {
  main {
    java.srcDirs(
      "src/accessor/java",
      "src/mixin/java"
    )
    resources.srcDirs(
      "src/accessor/resources/",
      "src/mixin/resources/"
    )
  }
}

loom.splitEnvironmentSourceSets()

// create a secondary set, with a configuration name matching the source set name
// this configuration is available at compile- and runtime, and not published
fun createSecondarySet(name: String, action: Action<SourceSet> = Action { }): SourceSet {
  val set = sourceSets.create(name) {
    compileClasspath += sourceSets.named("client").get().compileClasspath
    runtimeClasspath += sourceSets.named("client").get().runtimeClasspath
    action(this)
  }

  val setConfig = configurations.create(name)

  configurations.named(set.compileClasspathConfigurationName) {
    extendsFrom(setConfig)
  }
  configurations.named(set.runtimeClasspathConfigurationName) {
    extendsFrom(setConfig)
  }

  dependencies {
    set.implementationConfigurationName(sourceSets.named("client").map { it.output })
  }

  return set
}

// The testmod is not split, not worth the effort
val testmod = createSecondarySet("testmod") {
  java.srcDirs("src/testmodMixin/java")
  resources.srcDirs("src/testmodMixin/resources")
}

// Automated game tests, kept apart from the manual testmod
fabricApi {
  configureTests {
    createSourceSet = true
    modId = "adventure-platform-fabric-gametest"
    enableGameTests = true
    enableClientGameTests = true
    eula = true
    clearRunDirectory = true
  }
}
val gametest = sourceSets.named("gametest")
// Loom only adds the server run to check, so add the client run too
tasks.check {
  dependsOn("runClientGameTest")
}

val enablePermissionsApiCompat = true
val permissionsApiCompat = if (enablePermissionsApiCompat) createSecondarySet("permissionsApiCompat") else createSecondarySet("dummyPermissionsApiCompat")

configurations.named("clientAnnotationProcessor") {
  extendsFrom(configurations.annotationProcessor.get())
}

sourceSets {
  test {
    compileClasspath += main.get().compileClasspath
    runtimeClasspath += main.get().runtimeClasspath
    compileClasspath += getByName("client").compileClasspath
    runtimeClasspath += getByName("client").runtimeClasspath
  }
}

loom {
  accessWidenerPath = file("src/main/resources/adventure-platform-fabric.accesswidener")
  runs {
    register("testmodClient") {
      sourceSet = "testmod"
      client()
    }
    register("testmodServer") {
      sourceSet = "testmod"
      server()
    }

    configureEach {
      generateRunConfig = true
      jvmArguments.addAll(
        // "-Dmixin.debug.countInjections=true",
        // "-Dmixin.debug.strict=true", // Breaks FAPI :(
      )
    }
  }

  mods {
    register("adventure-platform-fabric") {
      sourceSet(sourceSets.main.get())
      sourceSet(sourceSets.named("client").get())
      if (enablePermissionsApiCompat) sourceSet(permissionsApiCompat)
      sourceSet("main", ":adventure-platform-mod-shared")
    }
    register("adventure-platform-fabric-testmod") {
      sourceSet(testmod)
      sourceSet("main", ":test-resources")
    }
  }

  decompilerOptions.named("vineflower") {
    options.put("win", "0")
  }

  runtimeOnlyLog4j.set(true)
}

tasks.withType(RunGameTask::class).configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(indra.javaVersions().target().map { v -> JavaLanguageVersion.of(v) })})
}

// Resources shared with the NeoForge testmod, consumed as a regular project dependency
val testResources = configurations.dependencyScope("testResources")
val testResourcesDirs = configurations.resolvable("testResourcesDirs") {
  extendsFrom(testResources.get())
  attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.RESOURCES))
  }
}

dependencies {
  if (enablePermissionsApiCompat) "testmodRuntimeOnly"(permissionsApiCompat.output)
  if (enablePermissionsApiCompat) "gametestRuntimeOnly"(permissionsApiCompat.output)
  testResources(project(":test-resources"))
  "testmodRuntimeOnly"(files(testResourcesDirs))
  if (enablePermissionsApiCompat) "permissionsApiCompat"(libs.fabric.permissionsApi) {
    isTransitive = false
  }

  // Testmod-specific dependencies
  "testmod"(fabricApiLibs.fabric.api)
  "gametestImplementation"(fabricApiLibs.fabric.api)
}

// Create a testmod jar
val testmodJar = tasks.register("testmodJar", Jar::class) {
  from(testmod.output)
  from(testResourcesDirs)
  archiveClassifier = "testmod"
}

tasks.build {
  dependsOn(testmodJar)
}

// Run the game tests against the built jars, installed like a player would
val gametestJar = tasks.register("gametestJar", Jar::class) {
  from(gametest.map { it.output })
  archiveClassifier = "gametest"
}
val productionTestMods = configurations.dependencyScope("productionTestMods")
val productionTestModFiles = configurations.resolvable("productionTestModFiles") {
  extendsFrom(productionTestMods.get())
  isTransitive = false
}
dependencies {
  productionTestMods(fabricApiLibs.fabric.api)
  // Not bundled in the Fabric API jar
  productionTestMods(fabricApiLibs.gametest.api.v1)
  productionTestMods(fabricApiLibs.client.gametest.api.v1)
}

tasks.register("productionServerGameTest", ServerProductionRunTask::class) {
  mods.from(tasks.jar, gametestJar, productionTestModFiles)
  jvmArgs.add("-Dfabric-api.gametest")
  runDir = layout.buildDirectory.dir("run/productionServerGameTest")
  // Cut off a hung run instead of letting it use up the CI job
  timeout = Duration.ofMinutes(10)
}
tasks.register("productionClientGameTest", ClientProductionRunTask::class) {
  mods.from(tasks.jar, gametestJar, productionTestModFiles)
  jvmArgs.add("-Dfabric.client.gametest")
  // Fail instead of waiting on Fabric Loader's error window, which nobody can close under XVFB
  jvmArgs.add("-Dfabric.noGui=true")
  runDir = layout.buildDirectory.dir("run/productionClientGameTest")
  // Cut off a hung run instead of letting it use up the CI job
  timeout = Duration.ofMinutes(10)
  // The client tests start a dedicated server in the run directory
  doFirst {
    runDir.get().asFile.mkdirs()
    runDir.file("eula.txt").get().asFile.writeText("eula=true")
  }
}

tasks {
  jar {
    if (enablePermissionsApiCompat) from(permissionsApiCompat.output)
  }

  sourcesJar {
    if (enablePermissionsApiCompat) from(permissionsApiCompat.allSource)
  }
}

val generateTemplates = createProcessResourceTemplates("generateTemplates", sourceSets.main.get())
val generateTestmodTemplates = createProcessResourceTemplates("generateTestmodTemplates", testmod)
val generateGametestTemplates = createProcessResourceTemplates("generateGametestTemplates", gametest.get())

tasks.withType(GenerateSourcesTask::class).configureEach {
  dependsOn(generateTemplates, generateTestmodTemplates, generateGametestTemplates)
}

// Workaround for both loom and indra doing publication logic in an afterEvaluate :(
indra.includeJavaSoftwareComponentInPublications(false)
publishing {
  publications.named("maven", MavenPublication::class) {
    from(components["java"])
  }
}

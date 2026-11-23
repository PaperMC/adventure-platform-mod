import java.time.Duration
import net.fabricmc.loom.task.RunGameTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask
import xyz.jpenilla.resourcefactory.fabric.Environment

plugins {
  id("net.fabricmc.fabric-loom")
  alias(libs.plugins.resourceFactory)
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

// TODO(26.4): re-enable once PermissionChecker supports Identifier-keyed permissions
//  https://github.com/PaperMC/adventure/issues/1457
val enablePermissionsApiCompat = false
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
  accessWidenerPath = file("src/main/resources/adventure-platform-fabric.classtweaker")
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
  // Let the game use a software Vulkan device, the only one on machines without a GPU like CI
  jvmArgs.add("-Drenderpearl.vulkan.allowcpudevice=true")
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

// Mod metadata
val fabricLoaderRequirement = ">=${libs.versions.fabricLoader.get()}"

sourceSets.main {
  resourceFactory.fabricModJson {
    author("kashike")
    author("zml")
    author("Luck")
    author("Electroid")
    contact {
      homepage = "https://github.com/PaperMC/adventure-platform-fabric"
      issues = "https://github.com/PaperMC/adventure-platform-fabric/issues"
      sources = "https://github.com/PaperMC/adventure-platform-fabric"
    }
    mitLicense()
    icon("assets/adventure-platform-fabric/logo.png")

    mainEntrypoint("net.kyori.adventure.platform.fabric.impl.AdventureFabricCommon")
    mainEntrypoint("net.kyori.adventure.platform.fabric.impl.compat.permissions.PermissionsApiIntegration")
    clientEntrypoint("net.kyori.adventure.platform.fabric.impl.client.AdventureFabricClient")
    entrypoint("adventure-internal:sidedproxy/client", "net.kyori.adventure.platform.modcommon.impl.client.ClientProxy")
    entrypoint("adventure-internal:sidedproxy/server", "net.kyori.adventure.platform.modcommon.impl.server.DedicatedServerProxy")
    accessWidener = "adventure-platform-fabric.classtweaker"
    mixin("adventure-platform-fabric.accessor.mixins.json")
    mixin("adventure-platform-fabric.mixins.json")
    mixin("adventure-platform-mod-shared.accessor.mixins.json")
    mixin("adventure-platform-mod-shared.mixins.json")
    mixin("adventure-platform-fabric.client.mixins.json") { environment = Environment.CLIENT }
    mixin("adventure-platform-mod-shared.client.mixins.json") { environment = Environment.CLIENT }

    modMenu { library() }

    depends("minecraft", "~26.4-")
    depends("fabricloader", fabricLoaderRequirement)
    depends("fabric-api-base", "*")
    depends("fabric-command-api-v2", "*")
    recommends("fabric-networking-api-v1", "*")
  }
}

testmod.resourceFactory.fabricModJson {
  id = "${rootProject.name.lowercase()}-testmod"
  name = "${rootProject.name} Tester"
  description.unsetConvention()
  author("zml")
  contact {
    homepage = "https://github.com/KyoriPowered/adventure-platform-fabric"
    issues = "https://github.com/KyoriPowered/adventure-platform-fabric/issues"
    sources = "https://github.com/KyoriPowered/adventure-platform-fabric"
  }
  mitLicense()
  mainEntrypoint("net.kyori.adventure.platform.test.fabric.AdventureFabricTester")
  mixin("adventure-platform-fabric-testmod.mixins.json")
  depends("fabricloader", fabricLoaderRequirement)
  depends("adventure-platform-fabric", "*")
  depends("fabric-api", "*")
}

gametest {
  resourceFactory.fabricModJson {
    id = "${project.name}-gametest"
    name = "${project.name} game tests"
    description.unsetConvention()
    mitLicense()
    mainEntrypoint("net.kyori.adventure.platform.test.fabric.gametest.AdventureGameTestMod")
    entrypoint("fabric-client-gametest", "net.kyori.adventure.platform.test.fabric.gametest.client.AdventureClientGameTest")
    entrypoint("fabric-gametest", "net.kyori.adventure.platform.test.fabric.gametest.server.CommandSourceGameTests")
    entrypoint("fabric-gametest", "net.kyori.adventure.platform.test.fabric.gametest.server.ConversionGameTests")
    entrypoint("fabric-gametest", "net.kyori.adventure.platform.test.fabric.gametest.server.MixinAuditGameTest")
    mixin("adventure-platform-fabric-gametest.mixins.json")
    depends("fabricloader", fabricLoaderRequirement)
    depends("adventure-platform-fabric", "*")
    depends("fabric-api", "*")
  }
}

// Workaround for both loom and indra doing publication logic in an afterEvaluate :(
indra.includeJavaSoftwareComponentInPublications(false)
publishing {
  publications.named("maven", MavenPublication::class) {
    from(components["java"])
  }
}

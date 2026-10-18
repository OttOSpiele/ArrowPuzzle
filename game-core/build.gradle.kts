plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
tasks.test { useJUnitPlatform() }
dependencies { testImplementation("org.junit.jupiter:junit-jupiter:5.11.4") }

tasks.register<JavaExec>("benchmarkLevels") {
 description = "Generate reference-stage puzzle levels and export quality CSV artifacts"
 group = "verification"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.arrow.GeneratorBenchmark")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("benchmarkSeeds") ?: "7").toString())
}

tasks.register<JavaExec>("benchmarkExactCover") {
 group = "verification"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.arrow.ExactCoverBenchmark")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("exactCoverSeeds") ?: "10").toString(),
  (project.findProperty("exactCoverNodes") ?: "600").toString())
}

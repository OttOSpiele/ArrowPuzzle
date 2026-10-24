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

tasks.register<JavaExec>("benchmarkExactCoverAB") {
 group = "verification"
 description = "Compare frozen #221 exact-cover solver with current code at matched seeds and budgets"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.arrow.ExactCoverComparison")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("exactCoverABSeeds") ?: "10").toString())
}

tasks.register<JavaExec>("benchmarkExactCoverStages") {
 group = "verification"
 description = "Stage 9-80 Exact Cover statistics and eight-board reference catalog"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.arrow.ExactCoverStageBenchmark")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("exactStageSeeds") ?: "16").toString())
}

tasks.register<JavaExec>("benchmarkExactCoverCatalog") {
 group = "verification"
 description = "Fast representative Exact Cover catalog with failure and quality statistics"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.arrow.ExactCoverStageBenchmark")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("exactCatalogSeeds") ?: "8").toString(), "catalog")
}

tasks.register<JavaExec>("benchmarkAnts") {
 group = "verification"
 description = "Reproducible AntsBlockCollector choice and random-player balancing CSV"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.ants.AntsBlockBenchmark")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("antsSeeds") ?: "8").toString())
}

tasks.register<JavaExec>("benchmarkAntsTimed") {
 group = "verification"
 description = "Measure time-stepped AntsBlockCollector slot occupancy and waiting stacks"
 dependsOn(tasks.testClasses)
 classpath = sourceSets.test.get().runtimeClasspath
 mainClass.set("de.ottospiele.ants.AntsBlockTimedBenchmark")
 args(layout.buildDirectory.dir("benchmark").get().asFile.absolutePath,
  (project.findProperty("antsSeeds") ?: "8").toString())
}

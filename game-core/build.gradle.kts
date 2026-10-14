plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
tasks.test { useJUnitPlatform() }
dependencies { testImplementation("org.junit.jupiter:junit-jupiter:5.11.4") }

// Runs the app's platform-independent logic (scheduling + snooze challenges) on a plain JVM,
// so it can be tested without the Android SDK.
plugins { kotlin("jvm") version "2.0.21" }
kotlin { jvmToolchain(21) }
sourceSets {
    main { kotlin.srcDir("../app/src/main/java/com/maxalarm/core") }
    test { kotlin.srcDir("../app/src/test/java/com/maxalarm/core") }
}
dependencies { testImplementation("junit:junit:4.13.2") }
tasks.test { testLogging { events("passed", "failed"); showStandardStreams = true } }

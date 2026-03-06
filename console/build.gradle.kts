plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.romualdsigning.studentgradecalc.console.GradeCalcCliKt")
}

dependencies {
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}


plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "top.projectxero.ca"
version = "1.0.0"

repositories {
    mavenCentral()
}

// 和 CA 其它构建产物一样统一放到仓库根的 build/，
// 不额外产生 caaccel/build/。
layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("build/caaccel"))

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

sourceSets {
    main {
        kotlin.srcDir("src")
    }
    test {
        kotlin.srcDir("test")
    }
    create("bench") {
        kotlin.srcDir("bench")
    }
}

dependencies {
    "benchImplementation"(kotlin("stdlib"))
    "benchImplementation"("org.mozilla:rhino:1.9.2")
}

sourceSets["bench"].apply {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

// 主 jar 保持 Java 8 目标以兼容 Android；基准任务跑在开发机 JVM 上，
// Rhino 1.9.2 本身要求 Java 11+，所以只把 bench 配置的解析目标抬到 17。
configurations.matching { it.name.startsWith("bench") && it.isCanBeResolved }.configureEach {
    attributes {
        attribute(
            org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
            17,
        )
    }
}

tasks.jar {
    archiveFileName.set("caaccel.jar")
    destinationDirectory.set(rootProject.layout.projectDirectory.dir("build"))
}

val testClassNames = listOf("AccelTest", "MatchStringTest", "MoreAccelTest")
val caaccelTestTasks = testClassNames.map { name ->
    tasks.register<JavaExec>("run$name") {
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set(name)
    }
}

tasks.register("caaccelTest") {
    group = "verification"
    description = "Run caaccel standalone test programs"
    dependsOn(caaccelTestTasks)
}

tasks.register<JavaExec>("bench") {
    group = "verification"
    description = "Run matchString benchmark against Rhino"
    classpath = sourceSets["bench"].runtimeClasspath
    mainClass.set("BenchMatchString")
}

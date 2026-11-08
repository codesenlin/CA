plugins {
    kotlin("multiplatform") version "2.4.21"
}

group = "ca"

// CA 根目录已经有 cabuildtools 的 build 输出，Kotlin 产物放到独立子目录避免冲突。
layout.buildDirectory.set(layout.projectDirectory.dir("build/ca-kotlin"))
version = "0.1.0"

kotlin {
    js {
        browser()
        binaries.executable()
        compilerOptions {
            target = "es5"
            useEsClasses = false
        }
    }
}

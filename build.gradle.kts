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
    sourceSets {
        val jsMain by getting {
            // 源码直接放在 src/<分类>/ 下，避免 jsMain/kotlin/ca/kotlin 五层嵌套。
            kotlin.setSrcDirs(listOf("src"))
        }
    }
}

// Kotlin/JS 默认锁里会带一批已披露漏洞的 webpack 开发期依赖。
// 在 npm install 前向生成的根 package.json 注入 Yarn resolutions，
// 让 kotlin-js-store/yarn.lock 也能固定到已修复版本。
tasks.matching { it.name == "kotlinNpmInstall" }.configureEach {
    doFirst {
        val pkg = layout.buildDirectory.file("js/package.json").get().asFile
        if (!pkg.exists()) return@doFirst
        val resolutions = """{
            "minimatch": "11.0.1",
            "glob": "14.0.2",
            "serialize-javascript": "7.1.3",
            "diff": "8.0.3"
        }""".trimIndent().replace("\n", "")
        val text = pkg.readText()
        if (text.contains("\"resolutions\": {}")) {
            pkg.writeText(text.replace("\"resolutions\": {}", "\"resolutions\": $resolutions"))
        }
    }
}

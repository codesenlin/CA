package ca.kotlin.core


/**
 * modules/utils/Threads.js 的 Kotlin/JS 版本。
 *
 * Threads 必须在原来的加载位置安装，不能和 Internal 一起提前 onCreate；
 * 由 CaKotlinPending.installAll() 在启动阶段统一安装。
 */
private val threadsApi: dynamic = js("({})")
private var executor: dynamic = js("null")

private fun pool(): dynamic = executor

fun installThreads() {
    threadsApi.executor = js("null")

    threadsApi.onCreate = {
        executor = js("java.util.concurrent.Executors").newCachedThreadPool()
        threadsApi.executor = executor
    }

    threadsApi.run = { f: dynamic ->
        pool().submit(js("new java.lang.Runnable(f)"))
    }

    threadsApi.call = { f: dynamic ->
        pool().submit(js("new java.util.concurrent.Callable(f)"))
    }

    fun awaitValue(f: dynamic, timeout: dynamic, hasTimeout: Boolean): dynamic {
        val holder = js("({ value: null, error: null })")
        val latch = js("new java.util.concurrent.CountDownLatch(1)")
        pool().submit(js("new java.lang.Runnable({ run: function() { try { holder.value = f(); } catch (e) { holder.error = e; } finally { latch.countDown(); } } })"))
        if (hasTimeout) latch.await(timeout, js("java.util.concurrent.TimeUnit").MILLISECONDS)
        else latch.await()
        if (holder.error !== null) js("throw holder.error")
        return holder.value
    }

    threadsApi.await = { f: dynamic ->
        awaitValue(f, 0, false)
    }

    threadsApi.awaitTimeout = { f: dynamic, timeout: dynamic ->
        awaitValue(f, timeout, true)
    }

    threadsApi.awaitDefault = { f: dynamic, timeout: dynamic, defaultValue: dynamic ->
        try {
            awaitValue(f, timeout, true)
        } catch (e: dynamic) {
            defaultValue
        }
    }

    threadsApi.awaitPromise = { promise: dynamic, timeout: dynamic, defaultValue: dynamic ->
        val lock = js("new java.util.concurrent.Semaphore(0)")
        var released = false
        var result: dynamic = defaultValue
        var err: dynamic = js("null")

        promise(
            { v: dynamic ->
                if (!released) {
                    result = v
                    released = true
                    lock.release()
                }
            },
            { e: dynamic ->
                if (!released) {
                    err = e
                    released = true
                    lock.release()
                }
            }
        )

        if (js("Boolean(timeout)")) {
            lock.tryAcquire(timeout, js("java.util.concurrent.TimeUnit").MILLISECONDS)
        } else {
            lock.acquire()
        }
        if (js("!defaultValue && err !== null")) js("throw err")
        result
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Threads", threadsApi)
    }
}

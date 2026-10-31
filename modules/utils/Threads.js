MapScript.loadModule("Threads", {
	onCreate : function() {
		this.executor = java.util.concurrent.Executors.newCachedThreadPool();
		// QuickJS 的 context 是单线程的：JS 线程阻塞等 Future 时，
		// 工作线程上的回调没法回到 JS（会死锁/超时）。这些同步等待
		// 在 QuickJS 下直接内联执行。
		try {
			this.isQuickJS = String(ScriptInterface.getScriptEngine ? ScriptInterface.getScriptEngine() : "").indexOf("QuickJS") >= 0;
		} catch (e) {
			this.isQuickJS = false;
		}
	},
	run : function(f) {
		return this.executor.submit(new java.lang.Runnable(f));
	},
	call : function(f) {
		return this.executor.submit(new java.util.concurrent.Callable(f));
	},
	await : function(f) {
		if (this.isQuickJS) return f();
		return this.executor.submit(new java.util.concurrent.Callable(f)).get();
	},
	awaitTimeout : function(f, timeout) {
		if (this.isQuickJS) return f();
		return this.executor.submit(new java.util.concurrent.Callable(f)).get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
	},
	awaitDefault : function(f, timeout, defaultValue) {
		if (this.isQuickJS) {
			try {
				return f();
			} catch (e) {
				return defaultValue;
			}
		}
		var future = this.executor.submit(new java.util.concurrent.Callable(f));
		try {
			return future.get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch(e) {
			return defaultValue;
		}
	},
	awaitPromise : function(promise, timeout, defaultValue) {
		if (this.isQuickJS) {
			// QuickJS 单线程：不能阻塞等回调（会死锁）。
			// 这里只触发异步流程，直接返回默认值；调用方继续跑，
			// 需要结果的场景要改成回调式（见 CA.showAgreementSync 的 QuickJS 分支）。
			try {
				promise(function() {}, function() {});
			} catch (e) {}
			return defaultValue;
		}
		var lock = new java.util.concurrent.Semaphore(0);
		var released = false, result = defaultValue, err = null;
		promise(function(v) {
			if (released) return;
			result = v;
			released = true;
			lock.release();
		}, function(e) {
			if (released) return;
			err = e;
			released = true;
			lock.release();
		});
		if (timeout) {
			lock.tryAcquire(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
		} else {
			lock.acquire();
		}
		if (!defaultValue && err) {
			throw err;
		}
		return result;
	}
});
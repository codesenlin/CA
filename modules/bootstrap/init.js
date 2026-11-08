Loader.load(function () {
	// Kotlin bundle 先加载，随后由 installAll() 按原启动顺序安装全部模块。
	Loader.fromFile("../../build/outputs/kotlin-js/Internal.kotlin.js")
	CaKotlinPending.installAll();
});

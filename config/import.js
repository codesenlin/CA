/**
 * generateImport（生成命令库的导入信息）用的配置。
 *
 * androidJarPath 留 null = 自动找 SDK 里最新的 platforms/android-NN/android.jar；
 * 也可以用环境变量 ANDROID_HOME 指定 SDK。
 */
module.exports = {
	androidJarPath : null,

	// 留 null 时自动探测（一般不用填）
	shellPath : null,
	androidSdk : null
};

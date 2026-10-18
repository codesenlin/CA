/**
 * shell / 发布流程的配置。
 *
 * 这里只写「和机器无关的默认值 + 平台差异」；具体路径由 cabuildtools/shellconfig.js
 * 自动探测：cadroid 项目位置、Android SDK、Build Tools、JDK。
 *
 * 想覆盖的话用环境变量（不用改这个文件）：
 *   CA_CADROID     cadroid 项目路径（默认找 CA 旁边的 cadroid/ 或 ca/）
 *   ANDROID_HOME   Android SDK 路径（默认还会读 cadroid/local.properties 的 sdk.dir）
 *   JAVA_HOME      JDK 路径（默认找 Studio 自带的 JBR 或 ~/.jdks 下的 17）
 *
 * 解析结果会写到 build/generated/shellConfig.json，排查问题时看那份就行。
 */
module.exports = {
	// 下面几项留 null = 自动探测；填了就用填的
	shellPath : null,
	androidSdk : null,
	javaHome : null,

	// Build Tools 优先用这个版本（没装则取已安装的最高版本）
	buildToolVersion : "34.0.0",

	// Gradle 构建出的 classes.dex（这个 dex 的 CRC32 会写进脚本，运行时校验）
	// AGP 8：release 经 R8 精简，产物在 minifyReleaseWithR8 下；AGP 7.3 及更早是 mergeDexRelease/out
	dexPath : "./app/build/intermediates/dex/release/minifyReleaseWithR8/classes.dex",

	// 以下路径都相对 cadroid 项目
	releaseSignPath : "./app/signatures/release.signature",   // 正式版证书 DER（异或密钥）
	debugSignPath : "./app/signatures/debug.signature",       // 调试版证书 DER
	hotfixPrivateKey : "./app/signatures/privateKey.pem",     // 热更新签名私钥
	keystorePath : "./app/signatures/release.keystore",       // 签名密钥（alias: testkey）
	keyPasswordPath : "./app/signatures/release.password",    // 密钥密码文件

	// 密钥轮换：默认不启用（单密钥签名）
	// 启用时填 lineage 文件路径 + 从旧到新的每把密钥，见 README「配置」一节
	lineagePath : null,
	lineageSigners : []
};

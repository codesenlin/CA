MapScript.loadModule("ctx", (function (global) {
	var cx;
	if ("ModPE" in global) { //以ModPE脚本加载(BlockLauncher及衍生App)
		MapScript.host = "BlockLauncher";
		MapScript.baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/com.mojang/ca/";
		cx = com.mojang.minecraftpe.MainActivity.currentMainActivity.get();
	} else if ("activity" in global) { //以AutoJS脚本加载（UI模式）
		MapScript.host = "AutoJs";
		MapScript.baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.xero.ca.script/";
		cx = activity;
	} else if ("context" in global) { //以AutoJS脚本加载（非UI模式）
		MapScript.host = "AutoJsNoUI";
		MapScript.baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.xero.ca.script/";
		cx = context;
	} else if ("ScriptInterface" in global) { //在Android脚本外壳中加载
		MapScript.host = "Android";
		MapScript.baseDir = ScriptInterface.getContext().getDir("rhino", 0).getAbsolutePath() + "/";
		cx = ScriptInterface.getContext();
	} else if ("World" in global) { //在Inner Core中加载
		MapScript.host = "InnerCore";
		MapScript.baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/com.mojang/ca/";
		cx = Packages.zhekasmirnov.launcher.utils.UIUtils.getContext();
	} else {
		MapScript.host = "Unknown";
		MapScript.baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/com.mojang/ca/";
		cx = com.mojang.minecraftpe.MainActivity.currentMainActivity.get();
	}
	new java.io.File(MapScript.baseDir).mkdirs();
	return cx;
})(this));

MapScript.loadModule("gHandler", new android.os.Handler(ctx.getMainLooper()));

/*LOADER
if (variables.buildConfig.variants == "release") {
	postprocessor = function(src) {
		var jsmin = require("jsmin").jsmin;
		return jsmin(src, 2);
	};
}
*/
CA.Library.recommended = ["default", "addition", "basicedu"];
CA.Library.inner["default"] = Loader.fromFile("../data/idlibs/default.json");
CA.Library.inner["addition"] = Loader.fromFile("../data/idlibs/addition.json");
CA.Library.inner["education"] = Loader.fromFile("../data/idlibs/education.json");
CA.Library.inner["experiment"] = Loader.fromFile("../data/idlibs/experiment.json");
CA.Library.inner["basicedu"] = Loader.fromFile("../data/idlibs/basicedu.json");
CA.Library.defaultDisabled = Object.keys(CA.Library.inner).filter(function(e) { 
	return CA.Library.recommended.indexOf(e) < 0;
});

Common.themelist = {
	"light" : {
		"name" : Intl.get("common.theme.default")
	},
	"dark" : {
		"name" : Intl.get("common.theme.dark"),
		"bgcolor" : "#202020",
		"float_bgcolor" : "#404040",
		"message_bgcolor" : "#202020",
		"textcolor" : "#FFFFFF",
		"promptcolor" : "#C0C0C0",
		"highlightcolor" : "#FFFF00",
		"criticalcolor" : "#FFB040",
		"go_bgcolor" : "#616161",
		"go_textcolor" : "#FAFAFA",
		"go_touchbgcolor" : "#EEEEEE",
		"go_touchtextcolor" : "#000000"
	}
	/* 新建主题格式
	"light" : {						//主题ID ： light
		"name" : "默认风格",			//主题名称
		"bgcolor" : "#FAFAFA",		//主界面背景色
		"float_bgcolor" : "#F5F5F5",	//浮动栏（即滑动时与屏幕保持静止的栏）背景色
		"message_bgcolor" : "#FAFAFA",	//浮动界面背景色
		"textcolor" : "#212121",		//普通文本颜色
		"promptcolor" : "#9E9E9E",	//提示文本颜色
		"highlightcolor" : "#0000FF",	//高亮文本颜色
		"criticalcolor" : "#FF0000",	//警示文本颜色
		"go_bgcolor" : "#EEEEEE",	//GO按钮（主要动作按钮）背景色
		"go_textcolor" : "#000000",	//GO按钮文本颜色
		"go_touchbgcolor" : "#616161",	//GO按钮按下时背景色
		"go_touchtextcolor" : "#FAFAFA"	//GO按钮按下时文本颜色
	}
	*/
};

CA.tips = CA.defalutTips = Loader.fromFile("../data/tips.json");
MapScript.loadModule("UIHelp", {
	/** 设置/列表项通用 vmaker：标题 + 描述两行（holder.title / holder.desp） */
	settingItem: function (holder) {
		var view = new G.LinearLayout(ctx);
		view.setOrientation(G.LinearLayout.VERTICAL);
		view.setPadding(15 * G.dp, 10 * G.dp, 15 * G.dp, 10 * G.dp);
		view.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
		var title = holder.title = new G.TextView(ctx);
		title.setGravity(G.Gravity.CENTER | G.Gravity.LEFT);
		title.setLayoutParams(new G.LinearLayout.LayoutParams(-1, -2));
		Common.applyStyle(title, "textview_default", 2);
		view.addView(title);
		var desp = holder.desp = new G.TextView(ctx);
		desp.setPadding(0, 3 * G.dp, 0, 0);
		desp.setLayoutParams(new G.LinearLayout.LayoutParams(-1, -2));
		Common.applyStyle(desp, "textview_prompt", 1);
		view.addView(desp);
		return view;
	}
});

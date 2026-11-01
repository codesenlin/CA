MapScript.loadModule("NetworkUI", {
	/**
	 * 网络模块共用的列表页外壳：标题栏（点击打开操作菜单）+ 列表 + 关闭按钮。
	 * self 需提供 adpt(SimpleListAdapter controller)、contextMenu、contextMenu 所需的
	 * appendPage / popup 等字段；opts 可覆盖 popupKey、headerView、dividerHeight。
	 */
	createListPage : function(self, title, onItemClick, opts) {
		opts = opts || {};
		self.linear = new G.LinearLayout(ctx);
		self.linear.setOrientation(G.LinearLayout.VERTICAL);
		self.linear.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 0);
		Common.applyStyle(self.linear, "message_bg");

		self.header = new G.LinearLayout(ctx);
		self.header.setOrientation(G.LinearLayout.HORIZONTAL);
		self.header.setPadding(0, 0, 0, 10 * G.dp);
		self.header.setOnClickListener(new G.View.OnClickListener({onClick : function(v) {try {
			Common.showOperateDialog(self.contextMenu);
			return true;
		} catch(e) {erp(e)}}}));

		self.title = new G.TextView(ctx);
		self.title.setText(title || "");
		self.title.setGravity(G.Gravity.LEFT | G.Gravity.CENTER);
		self.title.setPadding(10 * G.dp, 0, 10 * G.dp, 0);
		Common.applyStyle(self.title, "textview_default", 4);
		self.header.addView(self.title, new G.LinearLayout.LayoutParams(0, -2, 1.0));

		self.menu = new G.TextView(ctx);
		self.menu.setText("▼");
		self.menu.setPadding(10 * G.dp, 0, 10 * G.dp, 0);
		self.menu.setGravity(G.Gravity.CENTER);
		Common.applyStyle(self.menu, "button_highlight", 3);
		self.header.addView(self.menu, new G.LinearLayout.LayoutParams(-2, -1));
		self.linear.addView(self.header, new G.LinearLayout.LayoutParams(-1, -2));

		self.more = new G.TextView(ctx);
		self.more.setGravity(G.Gravity.CENTER);
		self.more.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 15 * G.dp);
		self.more.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
		Common.applyStyle(self.more, "textview_prompt", 2);

		self.list = new G.ListView(ctx);
		self.list.setAdapter(self.adpt.self);
		if (opts.headerView) self.list.addHeaderView(opts.headerView);
		if (opts.dividerHeight !== undefined) self.list.setDividerHeight(opts.dividerHeight);
		self.list.setOnItemClickListener(new G.AdapterView.OnItemClickListener({onItemClick : function(parent, view, pos, id) {try {
			if (view == self.more) {
				self.appendPage();
				return;
			}
			onItemClick(parent, parent.getAdapter().getItem(pos), view);
		} catch(e) {erp(e)}}}));
		self.linear.addView(self.list, new G.LinearLayout.LayoutParams(-1, 0, 1.0));

		self.exit = new G.TextView(ctx);
		self.exit.setText("关闭");
		self.exit.setGravity(G.Gravity.CENTER);
		self.exit.setPadding(10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp);
		Common.applyStyle(self.exit, "button_critical", 3);
		self.exit.setOnClickListener(new G.View.OnClickListener({onClick : function(v) {try {
			self.popup.exit();
		} catch(e) {erp(e)}}}));
		self.linear.addView(self.exit, new G.LinearLayout.LayoutParams(-1, -2));

		self.popup = new PopupPage(self.linear, opts.popupKey || "feedback.Issues");
		self.popup.on("exit", function() {
			if (self.callback) self.callback();
		});
		PWM.registerResetFlag(self, opts.resetFlag || "linear");
	},
	/** 反馈列表的「仅显示未处理 / 显示所有」两个菜单项 */
	issueStateMenu : function(self) {
		return [
			NetworkUI.issueStateMenuEntry(self, "open"),
			NetworkUI.issueStateMenuEntry(self, "all")
		];
	},
	/** 反馈列表的「仅显示未处理 / 显示所有」菜单项 */
	issueStateMenuEntry : function(self, state) {
		return {
			text : state == "open" ? "仅显示未处理" : "显示所有",
			hidden : function() {
				return self.issueState == state;
			},
			onclick : function(v, tag) {
				self.issueState = state;
				self.reload();
			}
		};
	},
	/** 通用「关闭」按钮（弹窗底部，点击退出对应 popup） */
	closeButton : function(popup) {
		return L.TextView({
			text : "关闭",
			padding : [10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp],
			gravity : L.Gravity("center"),
			layout : { width : -1, height : -2 },
			style : "button_critical",
			fontSize : 3,
			onClick : function() {try {
				popup.exit();
			} catch(e) {erp(e)}}
		});
	},
	/** 分页加载通用外壳：进度/loading 守卫/fetch/null 处理/G.ui 应用 */
	appendPage : function(self, sync, opts) {
		if (!sync) {
			return NetworkUI.progress(opts.loadingText || "正在加载……", function() {
				self.appendPage(true);
			});
		}
		if (self.loading) return Common.toast("正在加载中……");
		if (opts.canLoad && !opts.canLoad()) return;
		self.loading = true;
		var page;
		try {
			page = opts.fetch();
		} catch(e) {
			Log.e(e);
		}
		if (!page) {
			self.loading = false;
			return Common.toast(opts.failText || "加载失败");
		}
		G.ui(function() {try {
			opts.apply(page);
			self.loading = false;
			if (opts.afterApply) opts.afterApply(page);
		} catch(e) {erp(e)}});
	},
	/** 列表「加载更多」footer/header 的显示与文字更新 */
	updateMore : function(self, hasNext, text, useHeader) {
		if (text) self.more.setText(text);
		if (hasNext && !self.moreVisible) {
			self.moreVisible = true;
			if (useHeader) self.list.addHeaderView(self.more);
			else self.list.addFooterView(self.more);
		} else if (!hasNext && self.moreVisible) {
			self.moreVisible = false;
			if (useHeader) self.list.removeHeaderView(self.more);
			else self.list.removeFooterView(self.more);
		}
	},
	/** 通用异步进度对话框：showProgressDialog + setText + async(task) */
	progress : function(text, task) {
		var dialog = Common.showProgressDialog();
		if (text) dialog.setText(text);
		dialog.async(task);
		return dialog;
	},
	/** 网络列表项通用 vmaker：标题 + 描述两行 */
	listItem : function(holder) {
		var layout = new G.LinearLayout(ctx),
			text1 = holder.text1 = new G.TextView(ctx),
			text2 = holder.text2 = new G.TextView(ctx);
		layout.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
		layout.setOrientation(G.LinearLayout.VERTICAL);
		layout.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 15 * G.dp);
		text1.setLayoutParams(new G.LinearLayout.LayoutParams(-1, -2));
		text1.setEllipsize(G.TextUtils.TruncateAt.END);
		text1.setSingleLine(true);
		layout.addView(text1);
		text2.setPadding(0, 5 * G.dp, 0, 0);
		text2.setLayoutParams(new G.LinearLayout.LayoutParams(-1, -2));
		Common.applyStyle(text2, "textview_prompt", 1);
		layout.addView(text2);
		return layout;
	}
});

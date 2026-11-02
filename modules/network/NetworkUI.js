MapScript.loadModule("NetworkUI", {
	/**
	 * 网络模块共用的列表页外壳：标题栏（点击打开操作菜单）+ 列表 + 关闭按钮。
	 * self 需提供 adpt(SimpleListAdapter controller)、contextMenu、contextMenu 所需的
	 * appendPage / popup 等字段；opts 可覆盖 popupKey、headerView、dividerHeight。
	 */
	createListPage: function (self, title, onItemClick, opts) {
		opts = opts || {};
		self.linear = new G.LinearLayout(ctx);
		self.linear.setOrientation(G.LinearLayout.VERTICAL);
		self.linear.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 0);
		Common.applyStyle(self.linear, "message_bg");

		self.header = new G.LinearLayout(ctx);
		self.header.setOrientation(G.LinearLayout.HORIZONTAL);
		self.header.setPadding(0, 0, 0, 10 * G.dp);
		self.header.setOnClickListener(new G.View.OnClickListener({
			onClick: function (v) {
				try {
					Common.showOperateDialog(self.contextMenu);
					return true;
				} catch (e) { erp(e) }
			}
		}));

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
		self.list.setOnItemClickListener(new G.AdapterView.OnItemClickListener({
			onItemClick: function (parent, view, pos, id) {
				try {
					if (view == self.more) {
						self.appendPage();
						return;
					}
					onItemClick(parent, parent.getAdapter().getItem(pos), view);
				} catch (e) { erp(e) }
			}
		}));
		self.linear.addView(self.list, new G.LinearLayout.LayoutParams(-1, 0, 1.0));

		self.exit = new G.TextView(ctx);
		self.exit.setText("关闭");
		self.exit.setGravity(G.Gravity.CENTER);
		self.exit.setPadding(10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp);
		Common.applyStyle(self.exit, "button_critical", 3);
		self.exit.setOnClickListener(new G.View.OnClickListener({
			onClick: function (v) {
				try {
					self.popup.exit();
				} catch (e) { erp(e) }
			}
		}));
		self.linear.addView(self.exit, new G.LinearLayout.LayoutParams(-1, -2));

		self.popup = new PopupPage(self.linear, opts.popupKey || "feedback.Issues");
		self.popup.on("exit", function () {
			if (self.callback) self.callback();
		});
		PWM.registerResetFlag(self, opts.resetFlag || "linear");
	},
	/** 反馈列表的「仅显示未处理 / 显示所有」两个菜单项 */
	issueStateMenu: function (self) {
		return [
			NetworkUI.issueStateMenuEntry(self, "open"),
			NetworkUI.issueStateMenuEntry(self, "all")
		];
	},
	/** 反馈列表的「仅显示未处理 / 显示所有」菜单项 */
	issueStateMenuEntry: function (self, state) {
		return {
			text: state == "open" ? "仅显示未处理" : "显示所有",
			hidden: function () {
				return self.issueState == state;
			},
			onclick: function (v, tag) {
				self.issueState = state;
				self.reload();
			}
		};
	},
	/** 通用「关闭」按钮（弹窗底部，点击退出对应 popup） */
	closeButton: function (popup) {
		return L.TextView({
			text: "关闭",
			padding: [10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp],
			gravity: L.Gravity("center"),
			layout: { width: -1, height: -2 },
			style: "button_critical",
			fontSize: 3,
			onClick: function () {
				try {
					popup.exit();
				} catch (e) { erp(e) }
			}
		});
	},
	/** 分页加载通用外壳：进度/loading 守卫/fetch/null 处理/G.ui 应用 */
	appendPage: function (self, sync, opts) {
		if (!sync) {
			return NetworkUI.progress(opts.loadingText || "正在加载……", function () {
				self.appendPage(true);
			});
		}
		if (self.loading) return Common.toast("正在加载中……");
		if (opts.canLoad && !opts.canLoad()) return;
		self.loading = true;
		var page;
		try {
			page = opts.fetch();
		} catch (e) {
			Log.e(e);
		}
		if (!page) {
			self.loading = false;
			return Common.toast(opts.failText || "加载失败");
		}
		G.ui(function () {
			try {
				opts.apply(page);
				self.loading = false;
				if (opts.afterApply) opts.afterApply(page);
			} catch (e) { erp(e) }
		});
	},
	/** 列表「加载更多」footer/header 的显示与文字更新 */
	updateMore: function (self, hasNext, text, useHeader) {
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
	progress: function (text, task) {
		var dialog = Common.showProgressDialog();
		if (text) dialog.setText(text);
		dialog.async(task);
		return dialog;
	},
	/**
	 * 反馈话题详情页外壳：标题栏/列表等 children + popup + 可选关闭按钮。
	 * opts: { popupKey?, closeButton? }
	 */
	issueDetailPage: function (self, children, opts) {
		opts = opts || {};
		var linear = L.LinearLayout({
			orientation: L.LinearLayout("vertical"),
			padding: [0, 15 * G.dp, 0, 0],
			style: "message_bg",
			children: children
		});
		self.popup = new PopupPage(linear, opts.popupKey || "feedback.IssueDetail");
		if (opts.closeButton) linear.addView(NetworkUI.closeButton(self.popup));
		self.popup.on("exit", function () {
			if (self.callback) self.callback();
		});
		PWM.registerResetFlag(self, "popup");
		return self.popup;
	},
	/**
	 * 反馈话题详情页的标题栏：可选返回按钮 + 标题 + 操作菜单。
	 * self 需提供 title/contextMenu，返回头部 LinearLayout。
	 */
	issueDetailHeader: function (self, hasBack) {
		var children = [];
		if (hasBack) {
			children.push(L.TextView({
				text: "< 返回",
				padding: [10 * G.dp, 10 * G.dp, 10 * G.dp, 10 * G.dp],
				layout: { width: -2, height: -2 },
				style: "button_critical",
				fontSize: 2,
				onClick: function () {
					try {
						self.popup.exit();
					} catch (e) { erp(e) }
				}
			}));
		}
		children.push(self.title = L.TextView({
			singleLine: true,
			ellipsize: G.TextUtils.TruncateAt.END,
			gravity: L.Gravity("left|center"),
			padding: [10 * G.dp, 0, 10 * G.dp, 0],
			style: "textview_default",
			fontSize: 4,
			layout: { width: 0, height: -2, weight: 1.0 }
		}));
		children.push(L.TextView({
			text: "▼",
			padding: [10 * G.dp, 0, 10 * G.dp, 0],
			gravity: L.Gravity("center"),
			style: "button_highlight",
			fontSize: 3,
			layout: { width: -2, height: -2 }
		}));
		return L.LinearLayout({
			orientation: L.LinearLayout("horizontal"),
			padding: [15 * G.dp, 0, 15 * G.dp, 10 * G.dp],
			gravity: L.Gravity("center"),
			onClick: function () {
				try {
					Common.showOperateDialog(self.contextMenu);
				} catch (e) { erp(e) }
			},
			children: children
		});
	},
	/**
	 * 评论输入条：输入框 + 发送按钮。
	 * opts: { hint?, onSend(text, input) }，返回 { view, input }。
	 */
	commentBar: function (opts) {
		opts = opts || {};
		var inputOpts = {
			layout: { width: 0, height: -2, weight: 1.0 },
			padding: [10 * G.dp, 10 * G.dp, 0, 10 * G.dp],
			imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
			style: "edittext_default",
			fontSize: 3
		};
		if (opts.hint) {
			inputOpts.hint = opts.hint;
			inputOpts.focusableInTouchMode = true;
		}
		var input = L.EditText(inputOpts);
		var view = L.LinearLayout({
			layout: { width: -1, height: -2 },
			orientation: L.LinearLayout("horizontal"),
			style: "bar_float",
			children: [
				input,
				L.TextView({
					layout: { width: -2, height: -1 },
					gravity: L.Gravity("center"),
					text: "发送",
					padding: [10 * G.dp, 10 * G.dp, 10 * G.dp, 10 * G.dp],
					style: "button_reactive_auto",
					fontSize: 3,
					onClick: function () {
						try {
							var s = String(input.text);
							if (!s) return Common.toast("内容不可为空！");
							opts.onSend(s, input);
						} catch (e) { erp(e) }
					}
				})
			]
		});
		return { view: view, input: input };
	},
	/**
	 * 编辑/新建话题对话框：标题 + 正文 + 确定。
	 * opts: { popupName, dialogTitle, titleHint, contentText?, contentHint,
	 *         contentField, data, onSubmit, onDismiss }
	 */
	editIssueDialog: function (opts) {
		G.ui(function () {
			try {
				var title, content, popup, data = opts.data || {}, field = opts.contentField || "content";
				popup = PopupPage.showDialog(opts.popupName, L.ScrollView({
					style: "message_bg",
					child: L.LinearLayout({
						orientation: L.LinearLayout("vertical"),
						padding: [15 * G.dp, 15 * G.dp, 15 * G.dp, 0],
						children: [
							L.TextView({
								text: opts.dialogTitle,
								padding: [0, 0, 0, 10 * G.dp],
								layout: { width: -1, height: -2 },
								style: "textview_default",
								fontSize: 4
							}),
							title = L.EditText({
								text: data.title,
								hint: opts.titleHint,
								singleLine: true,
								padding: [0, 0, 0, 0],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 3,
								layout: { width: -1, height: -2 }
							}),
							content = L.EditText({
								text: opts.contentText,
								hint: opts.contentHint,
								padding: [0, 20 * G.dp, 0, 0],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 2,
								layout: { width: -1, height: -2 }
							}),
							L.TextView({
								text: "确定",
								padding: [10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp],
								gravity: L.Gravity("center"),
								layout: { width: -1, height: -2 },
								style: "button_critical",
								fontSize: 3,
								onClick: function () {
									try {
										data.title = String(title.text);
										data[field] = String(content.text);
										if (!data.title) return Common.toast("标题不能为空！");
										if (opts.onSubmit) opts.onSubmit(data);
										popup.exit();
									} catch (e) { erp(e) }
								}
							})
						]
					})
				}), -1, -2);
				if (opts.onDismiss) popup.on("exit", opts.onDismiss);
			} catch (e) { erp(e) }
		})
	},
	/**
	 * 通用表单弹窗：标题 + 输入框 + 可选说明 + 按钮。
	 * opts: { popupName, title, fields, note?, buttons, padding? }
	 * fields: { name, hint, value?, inputType?, password?, singleLine? }
	 * buttons: { text, onClick(fields, popup), style?, fontSize?, padding? }
	 */
	formDialog: function (opts) {
		G.ui(function () {
			try {
				var popup, fields = {}, children = [], i, f, b, inputOpts, buttonOpts;
				children.push(L.TextView({
					text: opts.title,
					padding: [0, 0, 0, 10 * G.dp],
					layout: { width: -1, height: -2 },
					gravity: L.Gravity("center"),
					style: "textview_default",
					fontSize: 4
				}));
				for (i = 0; i < opts.fields.length; i++) {
					f = opts.fields[i];
					inputOpts = {
						hint: f.hint,
						singleLine: f.singleLine !== false,
						padding: [0, i === 0 ? 0 : 20 * G.dp, 0, 0],
						imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
						style: "edittext_default",
						fontSize: 3,
						layout: { width: -1, height: -2 }
					};
					if (f.value !== undefined) inputOpts.text = f.value;
					if (f.inputType) inputOpts.inputType = L.InputType(f.inputType);
					if (f.password) inputOpts.transformationMethod = L.asClass(L.PasswordTransformationMethod).instance;
					fields[f.name] = L.EditText(inputOpts);
					children.push(fields[f.name]);
				}
				if (opts.note) {
					children.push(L.TextView({
						text: opts.note.text,
						padding: opts.note.padding || [0, 20 * G.dp, 0, 10 * G.dp],
						layout: { width: -1, height: -2 },
						style: opts.note.style || "textview_highlight",
						fontSize: opts.note.fontSize || 2
					}));
				}
				for (i = 0; i < opts.buttons.length; i++) {
					b = opts.buttons[i];
					buttonOpts = {
						text: b.text,
						padding: b.padding || [10 * G.dp, 10 * G.dp, 10 * G.dp, 10 * G.dp],
						gravity: L.Gravity("center"),
						layout: { width: -1, height: -2 },
						style: b.style || "button_critical",
						fontSize: b.fontSize || 3,
						onClick: function (fields, button) {
							return function () {
								try {
									button.onClick(fields, popup);
								} catch (e) { erp(e) }
							};
						}(fields, b)
					};
					children.push(L.TextView(buttonOpts));
				}
				popup = PopupPage.showDialog(opts.popupName, L.ScrollView({
					style: "message_bg",
					child: L.LinearLayout({
						orientation: L.LinearLayout("vertical"),
						padding: opts.padding || [15 * G.dp, 15 * G.dp, 15 * G.dp, 5 * G.dp],
						children: children
					})
				}), -1, -2);
				return { popup: popup, fields: fields };
			} catch (e) { erp(e) }
		});
	},
	/** 网络列表项通用 vmaker：标题 + 描述两行 */
	listItem: function (holder) {
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

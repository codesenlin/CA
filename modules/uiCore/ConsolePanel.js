MapScript.loadModule("ConsolePanel", {
	/**
	 * 构建控制台/终端界面（DebugUtils 与 WSServer 共用）：
	 * 列表 + 输入栏 + 执行按钮 + 历史提示，注册到 PWM。
	 * self 需要提供 print / exec / history / lines / vmaker / vbinder。
	 * opts: { afterBuild: function } 在 popup 创建后、注册 PWM 前调用。
	 */
	build : function(self, popupKey, opts) {
		opts = opts || {};
		self.adapter = SimpleListAdapter.getController(new SimpleListAdapter(self.lines, self.vmaker, self.vbinder));

		self.main = new G.LinearLayout(ctx);
		self.main.setOrientation(G.LinearLayout.VERTICAL);

		self.bar = new G.LinearLayout(ctx);
		self.bar.setLayoutParams(new G.LinearLayout.LayoutParams(-1, -2));
		self.bar.setOrientation(G.LinearLayout.HORIZONTAL);
		Common.applyStyle(self.bar, "bar_float");

		self.cmd = new G.EditText(ctx);
		self.cmd.setLayoutParams(new G.LinearLayout.LayoutParams(-1, -2, 1.0));
		self.cmd.setFocusableInTouchMode(true);
		self.cmd.setPadding(5 * G.dp, 10 * G.dp, 0, 10 * G.dp);
		self.cmd.setImeOptions(G.EditorInfo.IME_FLAG_NO_FULLSCREEN);
		Common.applyStyle(self.cmd, "edittext_default", 3);
		self.bar.addView(self.cmd);
		Common.postIME(self.cmd);

		self.eval = new G.TextView(ctx);
		self.eval.setLayoutParams(new G.LinearLayout.LayoutParams(-2, -1));
		self.eval.setGravity(G.Gravity.CENTER);
		self.eval.setText(">");
		self.eval.setPadding(10 * G.dp, 10 * G.dp, 10 * G.dp, 10 * G.dp);
		Common.applyStyle(self.eval, "button_reactive", 3);
		self.eval.setOnTouchListener(new G.View.OnTouchListener({onTouch : function touch(v, e) {try {
			switch (e.getAction()) {
				case e.ACTION_DOWN:
				Common.applyStyle(v, "button_reactive_pressed", 3);
				break;
				case e.ACTION_CANCEL:
				case e.ACTION_UP:
				Common.applyStyle(v, "button_reactive", 3);
			}
			return false;
		} catch(e) {return erp(e), true}}}));
		self.eval.setOnClickListener(new G.View.OnClickListener({onClick : function(v) {try {
			if (!self.cmd.getText().length()) return;
			var s = String(self.cmd.getText());
			self.print(s);
			self.print("\n");
			self.exec(s);
			self.cmd.setText("");
		} catch(e) {erp(e)}}}));
		self.bar.addView(self.eval);

		self.prompt = new G.ListView(ctx);
		self.prompt.setLayoutParams(new G.LinearLayout.LayoutParams(-1, 0, 1.0));
		self.prompt.setDividerHeight(0);
		Common.applyStyle(self.prompt, "message_bg");
		self.prompt.setOnItemClickListener(new G.AdapterView.OnItemClickListener({onItemClick : function(parent, view, pos, id) {try {
			if (self.history[pos]) {
				self.history[self.lines.length - 1] = String(self.cmd.getText());
				self.cmd.setText(self.history[pos]);
				self.cmd.setSelection(self.cmd.length());
			}
		} catch(e) {erp(e)}}}));
		self.prompt.setOnItemLongClickListener(new G.AdapterView.OnItemLongClickListener({onItemLongClick : function(parent, view, pos, id) {try {
			Common.setClipboardText(self.lines[pos]);
			Common.toast("内容已复制");
			return true;
		} catch(e) {return erp(e), true}}}));
		self.prompt.setAdapter(self.adapter.self);
		self.main.addView(self.prompt);
		self.main.addView(self.bar);

		self.popup = new PopupPage(self.main, popupKey);

		PWM.registerResetFlag(self, "main");
		if (opts.afterBuild) opts.afterBuild();
		PWM.registerResetFlag(self, "main");
		return self;
	}
});

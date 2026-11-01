MapScript.loadModule("PushService", {
	apiHost: NetworkUtils.urlBase.api,
	version: 1,
	perPage: 20,
	onCreate: function () {
		if (MapScript.host != "Android") return;
		this.nms = ctx.getSystemService(ctx.NOTIFICATION_SERVICE);
		if (!this.nms) return;
		if (android.os.Build.VERSION.SDK_INT >= 26) {
			this.channel = new android.app.NotificationChannel("capush", "推送信息", this.nms.IMPORTANCE_DEFAULT);
			this.channel.setDescription("命令助手自带推送，仅命令助手在后台时工作");
			this.nms.createNotificationChannel(this.channel);
		}
	},
	initialize: function () {
		this.load();
		this.notify();
	},
	load: function () {
		this.enabled = !CA.settings.disablePush && MapScript.host == "Android";
		this.disabledTags = CA.settings.disabledPushTags ? CA.settings.disabledPushTags : CA.settings.disabledPushTags = [];
		this.lastPushId = isNaN(CA.settings.lastPushId) ? -1 : CA.settings.lastPushId;
	},
	save: function () {
		CA.settings.lastPushId = this.lastPushId;
	},
	getIntent: function (post) {
		var intent = null;
		try {
			if (post.url) {
				intent = android.content.Intent.parseUri(post.url, 0x7);
				intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
			}
		} catch (e) {
			Log.e(e);
		}
		return intent;
	},
	showNotification: function (o) {
		var builder, nof, id;
		id = parseInt(o.id);
		if (android.os.Build.VERSION.SDK_INT >= 26) {
			builder = new android.app.Notification.Builder(ctx, this.channel.id);
		} else {
			builder = new android.app.Notification.Builder(ctx);
		}
		builder.setContentTitle(String(o.name));
		if (o.desc) builder.setContentText(String(o.desc));
		builder.setAutoCancel(true);
		builder.setSmallIcon(com.xero.ca.R.mipmap.icon_small);
		builder.setLargeIcon(G.BitmapFactory.decodeResource(ctx.getResources(), com.xero.ca.R.mipmap.icon_small));
		intent = this.getIntent(o);
		if (intent) builder.setContentIntent(AndroidBridge.createPendingIntent("activity", intent, ["updateCurrent"], id));
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			nof = builder.build();
		} else {
			nof = builder.getNotification();
		}
		this.nms.notify("capush", id, nof);
		return true;
	},
	cancelNotification: function (o) {
		this.nms.cancel("capush", parseInt(o.id));
	},
	showDialog: function (o) {
		var intent = this.getIntent(o);
		if (intent) {
			Common.showConfirmDialog({
				title: o.name,
				description: o.desc,
				buttons: [Common.intl.open, Common.intl.close],
				callback: function (index) {
					if (index != 0) return;
					AndroidBridge.startActivity(intent);
				}
			});
		} else {
			Common.showConfirmDialog({
				title: o.name,
				description: o.desc,
				buttons: [Common.intl.ok]
			});
		}
	},
	getPosts: function (since, limit, direction, sort) {
		return JSON.parse(NetworkUtils.queryPage(this.apiHost + "/push?" + NetworkUtils.toQueryString({
			since: since,
			limit: limit,
			dir: direction,
			sort: sort
		})));
	},
	getTags: function () {
		return JSON.parse(NetworkUtils.queryPage(this.apiHost + "/push/tags"));
	},
	readPushs: function () {
		var pushs = this.getPosts(this.lastPushId + 1, 10, "forward", "desc"), firstTime = this.lastPushId < 0;
		if (pushs.length) {
			this.lastPushId = parseInt(pushs[0].id);
		}
		if (firstTime) {
			return [];
		} else {
			return pushs;
		}
	},
	peekLatestPush: function () {
		return this.getPosts(0, 1, "forward", "desc");
	},
	shouldShowPush: function (push) {
		var i;
		for (i = 0; i < push.tags.length; i++) {
			if (push.tags[i] == "hiddenForPush") return false;
			if (this.disabledTags.indexOf(push.tags[i]) >= 0) return false;
		}
		if (push.minver > this.version) return false;
		if (push.maxver < this.version) return false;
		return true;
	},
	shouldShowAsDialog: function (push) {
		var i;
		for (i = 0; i < push.tags.length; i++) {
			if (push.tags[i] == "important" || push.tags[i] == "showAsDialog") return true;
		}
		return false;
	},
	showPushs: function (pushs) {
		var i;
		for (i = 0; i < pushs.length; i++) {
			pushs[i].tags = pushs[i].tags.split("|");
			if (this.shouldShowPush(pushs[i])) {
				if (this.shouldShowAsDialog(pushs[i])) {
					this.showDialog(pushs[i]);
				} else {
					this.showNotification(pushs[i]);
				}
			}
		}
	},
	doCheckPush: function () {
		Threads.run(function () {
			try {
				var pushs;
				try {
					pushs = PushService.readPushs();
				} catch (e) {
					Log.e(e);
				}
				if (!pushs) return;
				PushService.save();
				G.ui(function () {
					try {
						PushService.showPushs(pushs);
					} catch (e) { erp(e) }
				});
			} catch (e) { erp(e) }
		});
	},
	notify: function () {
		var now = Date.now();
		if (!this.enabled) return;
		if (now - this.lastCheck < 3600000) return; // 1h
		this.lastCheck = now;
		this.doCheckPush();
	},
	showSettings: function self(title) {
		if (!self.base) {
			self.base = [{
				name: "启用推送",
				type: "boolean",
				get: function () {
					return PushService.enabled;
				},
				set: function (v) {
					CA.settings.disablePush = !v;
					PushService.load();
				}
			}];
			self.offline = [{
				text: "部分选项因目前处于离线状态而不可用",
				type: "text"
			}];
			self.tagsHeader = [{
				name: "标签管理",
				type: "tag"
			}];
			self.historyPost = [{
				name: "历史推送",
				type: "tag"
			}, {
				name: "查看历史推送信息",
				type: "custom",
				onclick: function (fset) {
					PushService.showHistoryPost();
				}
			}];
			self.getTagEnabled = function () {
				return PushService.disabledTags.indexOf(this.id) < 0;
			}
			self.setTagEnabled = function (v) {
				if (v) {
					Common.removeSet(PushService.disabledTags, this.id);
				} else {
					Common.addSet(PushService.disabledTags, this.id);
				}
			}
			var buildSettings = function (tags) {
				if (tags) {
					tags = tags.map(function (e) {
						return {
							id: e.id,
							name: e.name,
							description: e.desc,
							type: "boolean",
							get: self.getTagEnabled,
							set: self.setTagEnabled
						};
					});
					return self.base.concat(self.tagsHeader, tags, self.historyPost);
				}
				return self.base.concat(self.offline);
			};
			self.getArray = function (callback) {
				NetworkUI.progress("正在加载……", function () {
					var tags = PushService.tags;
					if (!tags) {
						try {
							tags = PushService.tags = PushService.getTags();
						} catch (e) {
							Log.e(e);
						}
					}
					callback(buildSettings(tags));
				});
			}
		}
		self.getArray(function (arr) {
			Common.showSettings(title, arr);
		});
	},
	showHistoryPost: function self(callback) {
		G.ui(function () {
			try {
				if (!self.linear) {
					self.contextMenu = [{
						text: "刷新",
						onclick: function (v, tag) {
							self.reload();
						}
					}];
					self.vmaker = NetworkUI.listItem;
					self.vbinder = function (holder, e, i, a) {
						holder.text1.setText(e.name);
						Common.applyStyle(holder.text1, "item_default", 3);
						holder.text2.setText("发布于" + e.posted + "\n\n" + (e.desc.length > 60 ? e.desc.slice(0, 59) + ".." : e.desc));
						holder.text2.setVisibility(e.desc ? G.View.VISIBLE : G.View.GONE);
					}
					self.reload = function () {
						if (self.loading) return Common.toast("正在加载中……");
						self.loading = true;
						self.posts.length = 0;
						self.adpt.notifyChange();
						NetworkUI.progress("正在加载……", function () {
							var first, data;
							try {
								first = PushService.getPosts(0, PushService.perPage, "forward", "desc");
								if (first.length == PushService.perPage) {
									data = {
										next: parseInt(first[first.length - 1].id) - 1
									};
								} else {
									data = {
										next: -1
									};
								}
								data.pages = [first];
							} catch (e) { Log.e(e) }
							if (!data) return Common.toast("推送信息列表加载失败");
							self.postData = data;
							G.ui(function () {
								try {
									self.addPageData(first);
									self.loading = false;
								} catch (e) { erp(e) }
							});
						});
					}
					self.appendPage = function (sync) {
						NetworkUI.appendPage(self, sync, {
							failText: "推送信息列表加载失败",
							fetch: function () {
								var page = PushService.getPosts(self.postData.next, PushService.perPage, "backward", "desc");
								self.postData.pages.push(page);
								if (page.length == PushService.perPage) {
									self.postData.next = parseInt(page[page.length - 1].id) - 1;
								} else self.postData.next = -1;
								return page;
							},
							apply: function (page) {
								self.addPageData(page);
							}
						});
					}
					self.addPageData = function (page) {
						var off = self.posts.length, i;
						self.posts.length += page.length;
						for (i = 0; i < page.length; i++) {
							self.posts[i + off] = page[i];
						}
						self.adpt.notifyChange();
						NetworkUI.updateMore(self, self.postData.next >= 0, self.postData.next >= 0 ? "显示下" + PushService.perPage + "个推送信息……" : null);
					}
					self.adpt = SimpleListAdapter.getController(new SimpleListAdapter(self.posts = [], self.vmaker, self.vbinder));

					NetworkUI.createListPage(self, "推送信息", function (parent, data) {
						var intent = PushService.getIntent(data);
						if (intent) AndroidBridge.startActivity(intent);
					});
				}
				self.callback = callback;
				self.popup.enter();
				self.reload();
			} catch (e) { erp(e) }
		})
	}
});
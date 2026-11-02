MapScript.loadModule("GiteeFeedback", {
	name: "GiteeFeedback",
	author: "ProjectXero",
	version: [1, 0, 0],
	uuid: "3c7b3f7f-bda9-4ed9-a336-cdee2ebae433",
	targetOwner: "projectxero",
	targetRepo: "ca",
	perPage: 20,
	maxFeedbackCount: 50,
	databaseDelay: 2000,
	initialize: function () {
		if (MapScript.host == "Android") {
			this.clientId = String(ScriptInterface.getGiteeClientId());
			this.clientSecret = String(ScriptInterface.getGiteeClientSecret());
			this.redirectUrl = "https://projectxero.gitee.io/ca/feedback.html";
		}
		if (CA.settingsVersion < Date.parse("2019-04-05")) {
			this.upgradeRecentFeedback();
		}
	},
	getAuthorizeUrl: function () {
		return "https://gitee.com/oauth/authorize?" + NetworkUtils.toQueryString({
			client_id: this.clientId,
			redirect_uri: this.redirectUrl,
			response_type: "code"
		});
	},
	acquireAccessTokenAnonymous: function () {
		this.accessType = "anonymous";
		this.accessToken = undefined;
		this.accessData = null;
	},
	acquireAccessTokenOAuth: function (authorizationCode) {
		var d = JSON.parse(NetworkUtils.postPage("https://gitee.com/oauth/token?" + NetworkUtils.toQueryString({
			grant_type: "authorization_code",
			code: authorizationCode,
			client_id: this.clientId,
			redirect_uri: this.redirectUrl,
			client_secret: this.clientSecret
		})));
		this.accessType = "oauth";
		this.accessToken = d.access_token;
		d.expiredDate = d.created_at + d.expires_in;
		this.accessData = d;
	},
	acquireAccessToken: function (userName, password) {
		var d = JSON.parse(NetworkUtils.postPage("https://gitee.com/oauth/token", NetworkUtils.toQueryString({
			grant_type: "password",
			username: userName,
			password: password,
			client_id: this.clientId,
			client_secret: this.clientSecret,
			scope: "user_info issues notes"
		}), "application/x-www-form-urlencoded"));
		this.accessType = "basic";
		this.accessToken = d.access_token;
		d.expiredDate = d.created_at + d.expires_in;
		this.accessData = d;
	},
	refreshAccessToken: function (force) {
		if (this.accessData) {
			if (force || this.accessData.expiredDate < Date.now()) {
				var d = JSON.parse(NetworkUtils.postPage("https://gitee.com/oauth/token?" + NetworkUtils.toQueryString({
					grant_type: "refresh_token",
					refresh_token: this.accessData.refresh_token
				})));
				this.accessToken = d.access_token;
				d.expiredDate = d.created_at + d.expires_in;
				this.accessData = d;
				return 1;
			}
			return 0;
		}
		return -1;
	},
	getRecentFeedback: function (state) {
		var i, e, data, a = CA.settings.recentFeedback, r;
		if (this.userInfo) {
			try {
				r = this.getUserIssues(this.userInfo.name, state, 1);
			} catch (e) { Log.e(e) }
			if (!r) r = [];
			if (!a) a = {};
			for (i = 0; i < r.length; i++) {
				e = r[i];
				e.updated_utc = Date.parse(e.updated_at);
				if (e.number in a) {
					e.lastModified = a[e.number].lastModified;
				} else {
					e.lastModified = e.updated_utc;
				}
				e.isNew = e.lastModified < e.updated_utc;
			}
			return r;
		} else if (a) {
			r = [];
			for (i in a) {
				try {
					data = this.getIssue(i);
					if (state != "all" && data.state != state) continue;
					data.updated_utc = Date.parse(data.updated_at);
					data.lastModified = a[i].lastModified;
					data.isNew = data.lastModified < data.updated_utc;
					r.push(data);
				} catch (e) { Log.e(e) }
			}
			r.sort(function (a, b) {
				return b.updated_utc - a.updated_utc;
			});
			if (r.length > this.maxFeedbackCount) {
				for (i = this.maxFeedbackCount; i < r.length; i++) {
					delete a[r[i].number];
				}
				r.length = this.maxFeedbackCount;
			}
			return r;
		}
		return [];
	},
	updateRecentFeedback: function (data) {
		var i, a;
		if (!CA.settings.recentFeedback) CA.settings.recentFeedback = {};
		a = CA.settings.recentFeedback;
		if (data.number in a) {
			a[data.number].lastModified = Date.parse(data.updated_at);
		} else {
			a[data.number] = {
				lastModified: Date.parse(data.updated_at)
			};
		}
	},
	upgradeRecentFeedback: function () {
		var old = CA.settings.recentFeedback, r, i;
		if (!old) return;
		r = {};
		for (i = 0; i < old.length; i++) {
			r[old[i].number] = {
				lastModified: old[i].lastModified
			}
		}
	},
	getUserInfo: function () {
		return JSON.parse(NetworkUtils.queryPage("https://gitee.com/api/v5/user?" + NetworkUtils.toQueryString({ access_token: this.accessToken })));
	},
	getIssues: function (state, page) {
		return JSON.parse(NetworkUtils.queryPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues?" + NetworkUtils.toQueryString({
			state: state,
			sort: "created",
			direction: "desc",
			page: page,
			per_page: this.perPage
		})));
	},
	getUserIssues: function (userName, state, page) {
		return JSON.parse(NetworkUtils.queryPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues?" + NetworkUtils.toQueryString({
			state: state,
			sort: "updated",
			direction: "desc",
			page: page,
			per_page: this.maxFeedbackCount,
			creator: userName
		})));
	},
	createIssue: function (title, body) {
		return JSON.parse(NetworkUtils.postPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/issues", JSON.stringify({
			"access_token": this.accessToken,
			"repo": this.targetRepo,
			"title": title,
			"body": body
		}), "application/json;charset=UTF-8"));
	},
	getIssue: function (number) {
		return JSON.parse(NetworkUtils.queryPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues/" + number));
	},
	updateIssue: function (number, map) {
		return JSON.parse(NetworkUtils.request("https://gitee.com/api/v5/repos/" + this.targetOwner + "/issues/" + number, "PATCH", JSON.stringify({
			"access_token": this.accessToken,
			"repo": this.targetRepo,
			"title": map.title,
			"body": map.body,
			"state": map.state
		}), "application/json;charset=UTF-8"));
	},
	getIssueComment: function (id) {
		return JSON.parse(NetworkUtils.queryPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues/comments/" + id));
	},
	getIssueComments: function (number, page) {
		return JSON.parse(NetworkUtils.queryPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues/" + number + "/comments?" + NetworkUtils.toQueryString({
			page: page,
			per_page: this.perPage
		})));
	},
	createIssueComment: function (number, body) {
		return JSON.parse(NetworkUtils.postPage("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues/" + number + "/comments", JSON.stringify({
			"access_token": this.accessToken,
			"body": body
		}), "application/json;charset=UTF-8"));
	},
	updateIssueComment: function (id, body) {
		return JSON.parse(NetworkUtils.request("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues/comments/" + id, "PATCH", JSON.stringify({
			"access_token": this.accessToken,
			"body": body
		}), "application/json;charset=UTF-8"));
	},
	deleteIssueComment: function (id) {
		return JSON.parse(NetworkUtils.request("https://gitee.com/api/v5/repos/" + this.targetOwner + "/" + this.targetRepo + "/issues/comments/" + id, "DELETE", JSON.stringify({
			"access_token": this.accessToken
		}), "application/json;charset=UTF-8"));
	},
	showIssues: function self(callback) {
		G.ui(function () {
			try {
				if (!self.linear) {
					self.contextMenu = [{
						text: "刷新",
						onclick: function (v, tag) {
							self.reload();
						}
					}].concat(NetworkUI.issueStateMenu(self));
					self.vmaker = NetworkUI.listItem;
					self.vbinder = function (holder, e, i, a) {
						holder.text1.setText(e.state == "open" ? e.title : "[已处理]" + e.title);
						Common.applyStyle(holder.text1, e.state == "open" ? "item_default" : "item_disabled", 3);
						holder.text2.setText(e.body ? (e.body.length > 60 ? e.body.slice(0, 59) + ".." : e.body) : "(无)");
					}
					self.reload = function () {
						if (self.loading) return Common.toast("正在加载中……");
						self.loading = true;
						self.issues.length = 0;
						self.adpt.notifyChange();
						NetworkUI.progress("正在加载……", function () {
							var data;
							try {
								data = {
									next: 1,
									pages: [GiteeFeedback.getIssues(self.issueState, 1)]
								};
							} catch (e) { Log.e(e) }
							if (!data) return Common.toast("Issue列表加载失败");
							self.issueData = data;
							self.loading = false;
							self.appendPage(true);
						});
					}
					self.appendPage = function (sync) {
						NetworkUI.appendPage(self, sync, {
							failText: "Issue列表加载失败",
							fetch: function () {
								var page;
								if (self.issueData.next < self.issueData.pages.length) {
									page = self.issueData.pages[self.issueData.next];
								} else {
									page = GiteeFeedback.getIssues(self.issueState, self.issueData.next);
									self.issueData.pages[self.issueData.next] = page;
								}
								if (page.length != 0) {
									self.issueData.next++;
									if (self.issueData.next >= self.issueData.pages.length) {
										self.issueData.pages[self.issueData.next] = GiteeFeedback.getIssues(self.issueState, self.issueData.next);
									}
									if (self.issueData.pages[self.issueData.next].length == 0) self.issueData.next = NaN;
								} else self.issueData.next = NaN;
								return page;
							},
							apply: function (page) {
								var i, off = self.issues.length;
								self.issues.length += page.length;
								for (i = 0; i < page.length; i++) self.issues[i + off] = page[i];
								self.adpt.notifyChange();
								NetworkUI.updateMore(self, self.issueData.next, self.issueData.next ? "显示下" + GiteeFeedback.perPage + "个Issues……" : null);
							}
						});
					}
					self.adpt = SimpleListAdapter.getController(new SimpleListAdapter(self.issues = [], self.vmaker, self.vbinder));
					self.issueState = "all";

					NetworkUI.createListPage(self, "Issues", function (parent, data) {
						GiteeFeedback.showIssueDetail(data.number);
					});
				}
				self.callback = callback;
				self.popup.enter();
				self.reload();
			} catch (e) { erp(e) }
		})
	},
	showIssueDetail: function self(number, callback) {
		G.ui(function () {
			try {
				if (!self.linear) {
					self.contextMenu = [{
						text: "刷新",
						onclick: function (v, tag) {
							self.reload();
						}
					}, {
						text: "倒序浏览",
						hidden: function () {
							return self.viewOrder == "desc";
						},
						onclick: function (v, tag) {
							self.viewOrder = "desc";
							self.reload();
						}
					}, {
						text: "正序浏览",
						hidden: function () {
							return self.viewOrder == "asc";
						},
						onclick: function (v, tag) {
							self.viewOrder = "asc";
							self.reload();
						}
					}];
					self.vmaker = function (holder) {
						var layout = holder.linear = new G.LinearLayout(ctx),
							text1 = holder.text1 = new G.TextView(ctx),
							text2 = holder.text2 = new G.TextView(ctx);
						layout.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
						layout.setOrientation(G.LinearLayout.VERTICAL);
						layout.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 15 * G.dp);
						text1.setLayoutParams(new G.LinearLayout.LayoutParams(-2, -2));
						text1.setEllipsize(G.TextUtils.TruncateAt.END);
						text1.setSingleLine(true);
						layout.addView(text1);
						text2.setPadding(0, 5 * G.dp, 0, 0);
						text2.setLayoutParams(new G.LinearLayout.LayoutParams(-2, -2));
						Common.applyStyle(text2, "textview_prompt", 1);
						layout.addView(text2);
						return layout;
					}
					self.vbinder = function (holder, e, i, a) {
						holder.linear.setGravity(e.fromThis && self.viewOrder == "asc" ? G.Gravity.RIGHT : G.Gravity.LEFT);
						holder.text1.setText(e.user.name);
						Common.applyStyle(holder.text1, "item_default", 3);
						holder.text2.setText(e.body);
					}
					self.reload = function () {
						if (self.loading) return Common.toast("正在加载中……");
						self.loading = true;
						self.comments.length = 0;
						self.adpt.notifyChange();
						NetworkUI.progress("正在加载……", function () {
							var data;
							try {
								data = {
									topic: GiteeFeedback.getIssue(self.currentNumber)
								};
								data.totalPages = Math.ceil(data.topic.comments / GiteeFeedback.perPage);
								data.next = self.viewOrder == "desc" ? data.totalPages : 1;
							} catch (e) { Log.e(e) }
							self.loading = false;
							if (!data) return Common.toast("评论列表加载失败");
							self.commentData = data;
							G.ui(function () {
								try {
									self.title.setText(data.topic.title);
									self.issueBody.setText(data.topic.body + "\n\n#" + data.topic.number + " by " + data.topic.user.name + "\n" + new Date(data.topic.created_at).toLocaleString());
								} catch (e) { erp(e) }
							});
							self.appendPage(true);
						});
					}
					self.appendPage = function (sync) {
						NetworkUI.appendPage(self, sync, {
							failText: "评论列表加载失败",
							fetch: function () {
								return GiteeFeedback.getIssueComments(self.currentNumber, self.commentData.next);
							},
							apply: function (page) {
								var i, off = self.comments.length;
								self.comments.length += page.length;
								if (self.viewOrder == "desc") {
									page.reverse();
									if (self.commentData.next > 1) self.commentData.next--;
									else self.commentData.next = NaN;
								} else {
									if (self.commentData.next < self.commentData.totalPages) self.commentData.next++;
									else self.commentData.next = NaN;
								}
								for (i = 0; i < page.length; i++) {
									page[i].fromThis = page[i].user.id == GiteeFeedback.myUserId;
									self.comments[i + off] = page[i];
								}
								self.adpt.notifyChange();
								NetworkUI.updateMore(self, self.commentData.next, self.commentData.next ? "显示剩下" + (self.commentData.totalPages - self.commentData.next + 1) + "页……" : null);
							}
						});
					}
					self.adpt = SimpleListAdapter.getController(new SimpleListAdapter(self.comments = [], self.vmaker, self.vbinder));
					self.viewOrder = "asc";

					self.issueBody = new G.TextView(ctx);
					self.issueBody.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 15 * G.dp);
					self.issueBody.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
					Common.applyStyle(self.issueBody, "textview_default", 2);

					NetworkUI.createListPage(self, "", function () { }, {
						headerView: self.issueBody,
						dividerHeight: 0,
						popupKey: "feedback.IssueDetail"
					});
				}
				self.currentNumber = number;
				self.callback = callback;
				self.popup.enter();
				self.reload();
			} catch (e) { erp(e) }
		})
	},
	showRecentFeedback: function self(callback) {
		G.ui(function () {
			try {
				if (!self.linear) {
					self.contextMenu = [{
						text: "刷新",
						onclick: function (v, tag) {
							if (self.nextReload > Date.now()) return Common.toast("请不要频繁刷新页面");
							self.reload();
						}
					}, {
						text: "切换账号",
						onclick: function (v, tag) {
							GiteeFeedback.showLogin(function () {
								self.reload();
							});
						}
					}].concat(NetworkUI.issueStateMenu(self), [{
						text: "查看所有反馈",
						onclick: function (v, tag) {
							GiteeFeedback.showIssues();
						}
					}, {
						text: "查看常见问题解答(FAQ)",
						onclick: function (v, tag) {
							AndroidBridge.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://gitee.com/projectxero/ca/wikis/pages?title=FAQ"))
								.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
						}
					}, {
						text: "查看反馈说明",
						onclick: function (v, tag) {
							GiteeFeedback.showAgreement();
						}
					}]);
					self.vmaker = NetworkUI.listItem;
					self.vbinder = function (holder, e, i, a) {
						holder.text1.setText((e.state == "open" ? (e.isNew ? "[有新消息]" : "") : e.state == "rejected" ? "[已拒绝]" : "[已处理]") + e.title);
						Common.applyStyle(holder.text1, e.state == "open" ? "item_default" : "item_disabled", 3);
						holder.text2.setText("最近更新于 " + Updater.toChineseDate(e.updated_utc));
					}
					self.reload = function () {
						if (self.loading) return Common.toast("正在加载中……");
						self.loading = true;
						self.adpt.setArray([]);
						NetworkUI.progress("正在加载……", function () {
							var i, e, a = GiteeFeedback.getRecentFeedback(self.issueState), rejectCount = 0, rejectLatest = -Infinity, latest = -Infinity;
							for (i = 0; i < a.length; i++) {
								e = a[i];
								e.created_utc = Date.parse(e.created_at);
								latest = Math.max(latest, e.created_utc);
								if (e.state == "rejected") {
									rejectCount++;
									rejectLatest = Math.max(rejectLatest, e.created_utc);
								}
							}
							self.rejectTime = rejectCount > 2 ? rejectLatest + 24 * 3600 * 1000 * Math.pow(3, rejectCount - 3) : -Infinity;
							self.nextAdd = latest + 60 * 1000;
							self.nextReload = Date.now() + 20 * 1000;
							try {
								GiteeFeedback.myUserId = GiteeFeedback.userInfo ? GiteeFeedback.userInfo.id : GiteeFeedback.getUserInfo().id;
							} catch (e) { Log.e(e) }
							self.loading = false;
							G.ui(function () {
								try {
									self.title.setText("最近反馈 - " + (GiteeFeedback.userInfo ? GiteeFeedback.userInfo.name : "匿名"));
									self.adpt.setArray(a);
								} catch (e) { erp(e) }
							});
						});
					}
					self.addIssue = function () {
						if (!GiteeFeedback.accessToken) return Common.toast("您尚未登录，因此不能创建反馈");
						if (self.rejectTime > Date.now()) return Common.toast("因为您发布了无效的反馈，为防止服务器资源继续被浪费，您已被暂时禁止发布反馈！");
						if (self.nextAdd > Date.now()) return Common.toast("服务器忙，请1分钟后重试");
						GiteeFeedback.showEditIssue({
							title: "",
							body: "",
							newIssue: true
						}, function (o) {
							NetworkUI.progress("正在创建……", function () {
								var d, l;
								try {
									d = GiteeFeedback.createIssue(o.title, o.body);
								} catch (e) { Log.e(e) }
								if (!d) return Common.toast("话题创建失败");
								java.lang.Thread.sleep(GiteeFeedback.databaseDelay); //等待数据库更新
								GiteeFeedback.updateRecentFeedback(d);
								G.ui(function () {
									try {
										self.reload();
									} catch (e) { erp(e) }
								});
							});
						});
					}
					self.adpt = SimpleListAdapter.getController(new SimpleListAdapter([], self.vmaker, self.vbinder, null, true));
					self.issueState = "all";

					self.add = new G.TextView(ctx);
					self.add.setGravity(G.Gravity.CENTER);
					self.add.setText("新建反馈话题");
					self.add.setPadding(15 * G.dp, 15 * G.dp, 15 * G.dp, 15 * G.dp);
					self.add.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
					Common.applyStyle(self.add, "textview_prompt", 2);

					NetworkUI.createListPage(self, "最近反馈", function (parent, data, view) {
						if (view == self.add) return self.addIssue();
						GiteeFeedback.showFeedbackDetail(data.number, 0, function () {
							self.reload();
						});
					}, { headerView: self.add, popupKey: "feedback.Recent" });
				}
				self.callback = callback;
				self.popup.enter();
				self.reload();
			} catch (e) { erp(e) }
		})
	},
	showFeedbackDetail: function self(number, readOnly, callback) {
		G.ui(function () {
			try {
				if (!self.popup) {
					self.getWritable = function () {
						var t;
						if (!GiteeFeedback.userInfo) return false;
						if (self.readOnly > 0) {
							return true;
						} else if (self.readOnly < 0) {
							return false;
						} else {
							t = self.commentData;
							return t && t.topic.state == "open";
						}
					}
					self.setState = function (state) {
						var topic = self.commentData.topic;
						NetworkUI.progress("正在标记……", function () {
							var d;
							try {
								d = GiteeFeedback.updateIssue(topic.number, {
									state: state
								});
							} catch (e) { Log.e(e) }
							if (!d) return Common.toast("话题修改失败");
							java.lang.Thread.sleep(GiteeFeedback.databaseDelay); //等待数据库更新
							G.ui(function () {
								try {
									self.reload();
								} catch (e) { erp(e) }
							});
						});
					}
					self.contextMenu = [{
						text: "刷新",
						onclick: function (v, tag) {
							self.reload();
						}
					}, {
						text: "编辑主题",
						hidden: function () {
							return !self.writable;
						},
						onclick: function (v, tag) {
							var topic = self.commentData.topic;
							GiteeFeedback.showEditIssue(topic, function (o) {
								NetworkUI.progress("正在保存……", function () {
									var d;
									try {
										d = GiteeFeedback.updateIssue(topic.number, topic);
									} catch (e) { Log.e(e) }
									if (!d) return Common.toast("话题修改失败");
									java.lang.Thread.sleep(GiteeFeedback.databaseDelay); //等待数据库更新
									G.ui(function () {
										try {
											self.reload();
										} catch (e) { erp(e) }
									});
								});
							});
						}
					}, {
						text: "标记为已处理",
						hidden: function () {
							return !self.writable;
						},
						onclick: function (v, tag) {
							Common.showConfirmDialog({
								title: "确定将该反馈标记为已处理？",
								description: "*此操作无法撤销，且标记之后您将无法编辑反馈",
								callback: function (id) {
									if (id != 0) return;
									self.setState("closed");
								}
							});
						}
					}];
					self.vmaker = function (holder) {
						return NetworkUI.listItem(holder, {
							padding: [20 * G.dp, 15 * G.dp, 20 * G.dp, 15 * G.dp],
							width: -2,
							text1Style: "item_default",
							text1FontSize: 3
						});
					}
					self.vbinder = function (holder, e, i, a) {
						holder.linear.setGravity(e.fromThis ? G.Gravity.RIGHT : G.Gravity.LEFT);
						holder.text1.setText(e.fromThis && !GiteeFeedback.userInfo ? "匿名用户" : e.user.name);
						holder.text2.setText(e.body);
					}
					self.reload = function () {
						if (self.loading) return Common.toast("正在加载中……");
						self.loading = true;
						self.comments.length = 0;
						self.adpt.notifyChange();
						NetworkUI.progress("正在加载……", function () {
							var data;
							try {
								data = {
									topic: GiteeFeedback.getIssue(self.currentNumber)
								};
								data.totalPages = Math.ceil(data.topic.comments / GiteeFeedback.perPage);
								data.next = data.totalPages;
							} catch (e) { Log.e(e) }
							self.loading = false;
							if (!data) return Common.toast("评论列表加载失败");
							GiteeFeedback.updateRecentFeedback(data.topic);
							self.commentData = data;
							self.writable = self.getWritable();
							self.appendPage(true);
							G.ui(function () {
								try {
									self.title.setText(data.topic.title);
									if (self.writable && !self.talkVisible) {
										self.talkVisible = true;
										self.list.addFooterView(self.talk);
									} else if (!self.writable && self.talkVisible) {
										self.talkVisible = false;
										self.list.removeFooterView(self.talk);
									}
								} catch (e) { erp(e) }
							});
						});
					}
					self.appendPage = function (sync) {
						NetworkUI.appendPage(self, sync, {
							failText: "评论列表加载失败",
							canLoad: function () {
								return !isNaN(self.commentData.next);
							},
							fetch: function () {
								return GiteeFeedback.getIssueComments(self.currentNumber, self.commentData.next);
							},
							apply: function (page) {
								var i;
								if (self.commentData.next > 1) self.commentData.next--;
								else self.commentData.next = NaN;
								self.comments.length += page.length;
								for (i = self.comments.length - 1; i >= page.length; i--) self.comments[i] = self.comments[i - page.length];
								for (i = 0; i < page.length; i++) {
									page[i].fromThis = page[i].user.id == GiteeFeedback.myUserId;
									self.comments[i] = page[i];
								}
								if (!self.commentData.next) {
									self.comments.unshift({
										"fromThis": true,
										"body": self.commentData.topic.body || self.commentData.topic.title,
										"created_at": self.commentData.topic.created_at,
										"user": self.commentData.topic.user
									});
								}
								self.adpt.notifyChange();
								NetworkUI.updateMore(self, self.commentData.next, null, true);
							},
							afterApply: function (page) {
								if (page.length < 3 && !isNaN(self.commentData.next)) self.appendPage(false);
							}
						});
					}
					self.addComment = function (text) {
						NetworkUI.progress("正在发送……", function () {
							var d, l;
							try {
								d = GiteeFeedback.createIssueComment(self.currentNumber, text);
							} catch (e) { Log.e(e) }
							if (!d) Common.toast("评论创建失败");
							java.lang.Thread.sleep(GiteeFeedback.databaseDelay); //等待数据库更新
							G.ui(function () {
								try {
									self.reload();
								} catch (e) { erp(e) }
							});
						});
					}
					self.clickData = function (data, remote) {
						var text = data.body;
						var match;
						match = /\[Issue#(.+)\]/i.exec(text);
						if (match) {
							return GiteeFeedback.showIssueDetail(match[1]);
						}
					}
					self.adpt = SimpleListAdapter.getController(new SimpleListAdapter(self.comments = [], self.vmaker, self.vbinder));

					self.talkBar = NetworkUI.commentBar({
						hint: "发送评论",
						onSend: function (s) {
							self.addComment(s);
							self.talkbox.text = "";
						}
					});
					self.talk = self.talkBar.view;
					self.talkbox = self.talkBar.input;
					NetworkUI.issueDetailPage(self, [
						NetworkUI.issueDetailHeader(self, false),
						self.list = L.ListView({
							adapter: self.adpt.self,
							dividerHeight: 0,
							stackFromBottom: true,
							layout: { width: -1, height: 0, weight: 1.0 },
							onItemClick: function (parent, view, pos, id) {
								try {
									if (view == self.more) {
										self.appendPage();
										return;
									}
									var data = self.adpt.array[pos];
									self.clickData(data);
								} catch (e) { erp(e) }
							},
							_talkView: self.talk,
							_moreView: self.more = L.TextView({
								gravity: L.Gravity("center"),
								text: "显示更多",
								padding: [15 * G.dp, 15 * G.dp, 15 * G.dp, 15 * G.dp],
								layout: { width: -1, height: -2 },
								style: "textview_prompt",
								fontSize: 2
							})
						}),
					], { closeButton: true });
				}
				self.currentNumber = number;
				self.readOnly = readOnly;
				self.callback = callback;
				self.popup.enter();
				self.reload();
			} catch (e) { erp(e) }
		})
	},
	showEditIssue: function self(o, callback, onDismiss) {
		NetworkUI.editIssueDialog({
			popupName: "feedback.EditIssue",
			dialogTitle: o.newIssue ? "新建反馈话题" : "编辑反馈话题",
			titleHint: "在此处用一句话描述反馈的问题或建议",
			contentText: o.body,
			contentHint: "在这里补充说明发生问题时的现象、复现步骤与报错信息。如果是建议，请在这里说明提出建议的原因",
			contentField: "body",
			data: o,
			onSubmit: callback,
			onDismiss: onDismiss
		});
	},
	checkLogin: function (callback) {
		if (this.accessToken) {
			this.showRecentFeedback(callback);
		} else {
			this.load();
			if (this.accessType) {
				Common.showProgressDialog(function (dia) {
					dia.setText("正在自动登录...");
					if (GiteeFeedback.accessType == "anonymous") {
						GiteeFeedback.acquireAccessTokenAnonymous();
						GiteeFeedback.save(null);
					} else {
						try {
							GiteeFeedback.refreshAccessToken(true);
							GiteeFeedback.save(GiteeFeedback.getUserInfo());
						} catch (e) {
							Log.e(e);
							GiteeFeedback.accessType = GiteeFeedback.accessData = null;
							GiteeFeedback.save(null);
							return Common.toast("登录失败\n" + e);
						}
					}
					GiteeFeedback.showRecentFeedback(callback);
				});
			} else {
				this.showLogin(function () {
					GiteeFeedback.showRecentFeedback(callback);
				});
			}
		}
	},
	showLogin: function self(callback) {
		NetworkUI.formDialog({
			popupName: "feedback.GiteeLogin",
			title: "登录码云",
			padding: [15 * G.dp, 15 * G.dp, 15 * G.dp, 0],
			fields: [{
				name: "username",
				hint: "用户名(邮箱)",
				inputType: "TYPE_CLASS_TEXT|TYPE_TEXT_VARIATION_EMAIL_ADDRESS"
			}, {
				name: "password",
				hint: "密码",
				password: true,
				singleLine: false
			}],
			note: {
				text: "您需要一个账户才能创建和回复反馈，您也可以选择使用匿名用户来查看反馈",
				style: "textview_highlight",
				fontSize: 2,
				padding: [0, 20 * G.dp, 0, 10 * G.dp]
			},
			buttons: [{
				text: "登录",
				onClick: function (fields, popup) {
					if (!fields.username.length()) return Common.toast("用户名不能为空");
					if (!fields.password.length()) return Common.toast("密码不能为空");
					Common.showProgressDialog(function (dia) {
						dia.setText("正在登录...");
						try {
							GiteeFeedback.acquireAccessToken(fields.username.text, fields.password.text);
							GiteeFeedback.save(GiteeFeedback.getUserInfo());
						} catch (e) {
							Log.e(e);
							return Common.toast("登录失败，请检查您是否已连接互联网且用户名与密码正确\n" + e);
						}
						G.ui(function () {
							try {
								popup.exit();
								if (callback) callback();
							} catch (e) { erp(e) }
						});
					});
				}
			}, {
				text: "匿名使用",
				onClick: function (fields, popup) {
					Common.showProgressDialog(function (dia) {
						dia.setText("正在登录...");
						try {
							GiteeFeedback.acquireAccessTokenAnonymous();
							GiteeFeedback.save(null);
						} catch (e) {
							erp(e, true);
							return Common.toast("登录失败\n" + e);
						}
						G.ui(function () {
							try {
								popup.exit();
								if (callback) callback();
							} catch (e) { erp(e) }
						});
					});
				}
			}, {
				text: "使用浏览器登录或注册",
				padding: [10 * G.dp, 10 * G.dp, 10 * G.dp, 20 * G.dp],
				onClick: function (fields) {
					GiteeFeedback.startOAuth(callback);
					fields.__popup.exit();
				}
			}]
		});
	},
	load: function () {
		this.settings = CA.settings.feedbackSettings || (CA.settings.feedbackSettings = {});
		this.accessType = this.settings.accessType;
		this.accessData = this.settings.accessData;
	},
	save: function (userInfo) {
		this.settings = CA.settings.feedbackSettings || (CA.settings.feedbackSettings = {});
		this.settings.accessType = this.accessType;
		this.settings.accessData = this.accessData;
		this.userInfo = userInfo;
	},
	startOAuth: function (callback) {
		this.oauthCallback = callback;
		AndroidBridge.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(this.getAuthorizeUrl()))
			.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
	},
	callbackOAuth: function (code) {
		Common.showProgressDialog(function (dia) {
			dia.setText("正在登录...");
			try {
				GiteeFeedback.acquireAccessTokenOAuth(code);
				GiteeFeedback.save(GiteeFeedback.getUserInfo());
			} catch (e) {
				Log.e(e);
				return Common.toast("登录失败\n" + e);
			}
			G.ui(function () {
				try {
					PopupPage.show();
					if (GiteeFeedback.oauthCallback) GiteeFeedback.oauthCallback();
				} catch (e) { erp(e) }
			});
		});
	},
	showFeedbacks: function (callback) {
		if (this.clientId) {
			if (CA.settings.readFeedbackAgreement) {
				this.checkLogin(callback);
			} else {
				this.showAgreement(function () {
					GiteeFeedback.checkLogin(callback);
				});
			}
		} else {
			Common.toast("您目前使用的版本无法创建反馈");
			this.showIssues(callback);
		}
	},
	showAgreement: function (callback) {
		Common.showConfirmDialog({
			title: "反馈使用说明",
			description: ISegment.rawJson({
				extra: [
					"请务必看完本说明！\n",
					"\n1. 所有人反馈的内容都是公开的，请注意保护自己的隐私。",
					"\n2. 在反馈之前，请先查看常见问题解答(在最近反馈界面点击右上角三角显示的菜单内)。如果常见问题解答能解决你的问题，请不要重复反馈。",
					"\n3. 您如果重复反馈或反馈与命令助手无关的内容，这条反馈会被标注为“已拒绝”。如果您提出的被拒绝反馈数量超过两个，您会被暂时禁止反馈。",
					"\n4. 在自己的反馈下多次发送重复或无关反馈的内容会让这条反馈被标注为“已拒绝”。",
					"\n5. 请将您想反馈的内容表达尽可能具体。过于简单的反馈很可能会被标注为“已拒绝”。",
					"\n6. 您的未处理的反馈随时都可能收到回复，推荐一天检查一次反馈。被标注为“已处理”或“已拒绝”的反馈不能被回复。",
					"\n7. 如果您有很难用文字解释或者涉及隐私的反馈，建议私聊作者反馈：QQ:2687587184 电子邮箱:projectxero@163.com。",
					"\n\n您随时都可以在最近反馈界面点击右上角三角显示的菜单内再次查看本说明"
				],
				color: "textcolor"
			}),
			buttons: [
				"我了解了"
			],
			callback: function (id) {
				CA.settings.readFeedbackAgreement = true;
				if (callback) callback();
			}
		});
	}
});
MapScript.loadModule("ListAdapterBase", {
	/** 注册 DataSetObserver（各 ListAdapter 共用） */
	register : function(dso, p) {
		if (dso.indexOf(p) >= 0) return;
		dso.push(p);
	},
	/** 注销 DataSetObserver */
	unregister : function(dso, p) {
		var i = dso.indexOf(p);
		if (i >= 0) dso.splice(i, 1);
	},
	/**
	 * 生成 G.ListAdapter 的公共方法；各适配器只需要再补 getView，
	 * 有差异的 getItem/getItemId/getItemViewType 等通过 opts 传入覆盖。
	 */
	create : function(opts) {
		var arr = opts.array;
		return {
			getCount : opts.getCount || function() {
				return arr.length;
			},
			getItem : opts.getItem || function(pos) {
				if (pos == -1) return opts.controller;
				return arr[pos];
			},
			getItemId : opts.getItemId || function(pos) {
				return pos;
			},
			getItemViewType : opts.getItemViewType || function(pos) {
				return 0;
			},
			getViewTypeCount : opts.getViewTypeCount || function() {
				return 1;
			},
			hasStableIds : opts.hasStableIds || function() {
				return false;
			},
			isEmpty : opts.isEmpty || function() {
				return arr.length === 0;
			},
			areAllItemsEnabled : opts.areAllItemsEnabled || function() {
				return true;
			},
			isEnabled : opts.isEnabled || function(pos) {
				return pos >= 0 && pos < arr.length;
			},
			registerDataSetObserver : function(p) {
				ListAdapterBase.register(opts.dso, p);
			},
			unregisterDataSetObserver : function(p) {
				ListAdapterBase.unregister(opts.dso, p);
			}
		};
	},
	/** 列表项绑定异常时的兜底视图 */
	errorView : function(e) {
		var a = new G.TextView(ctx);
		a.setText(e + "\n" + e.stack);
		erp(e);
		return a;
	}
});

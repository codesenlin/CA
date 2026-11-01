MapScript.loadModule("SimpleListAdapter", (function () {
	var r = function (arr, maker, binder, params, readOnly) {
		//arr是列表数组，maker(holder, params)生成基础view，binder(holder, element, index, array, params)修改view使其实现指定的界面，readOnly表示是否会从外部修改这个数组
		var src = readOnly ? arr : arr.slice(), holders = [], dso = [], controller;
		controller = new SimpleListAdapter.Controller(src, holders, dso, maker, binder, params, readOnly ? null : arr, readOnly);
		var adapter = ListAdapterBase.create({
			array: src,
			controller: controller,
			dso: dso
		});
		controller.adapter = adapter;
		adapter.getView = function (pos, convert, parent) {
			var holder;
			try {
				if (!convert || !(convert.getTag() in holders)) {
					holder = {};
					convert = maker(holder, params);
					holder.self = convert;
					convert.setTag(holders.length.toString());
					holders.push(holder);
				}
				holder = holders[convert.getTag()];
				holder.pos = parseInt(pos);
				binder(holder, controller.array[pos], parseInt(pos), controller.array, params);
				return convert;
			} catch (e) {
				return ListAdapterBase.errorView(e);
			}
		};
		return new G.ListAdapter(adapter);
	}
	r.Controller = function (array, holders, dso, maker, binder, params, sync, readOnly) {
		this.array = array;
		this.holders = holders;
		this.dso = dso;
		this.maker = maker;
		this.binder = binder;
		this.params = params;
		this.sync = sync;
		this.readOnly = !!readOnly;
	}
	r.Controller.prototype = {
		clearHolder: function () {
			var i;
			for (i in this.holders) {
				this.holders[i].self.setTag("");
			}
			this.holders.length = 0;
			this.notifyChange();
		},
		getHolder: function (view) {
			return this.holders[view.getTag()];
		},
		notifyChange: function (hasSync) {
			if (!hasSync && this.sync) {
				this.setArray(this.sync);
			}
			this.dso.forEach(function (e) {
				if (e) e.onChanged();
			});
		},
		notifyInvalidate: function () {
			this.dso.forEach(function (e) {
				if (e) e.onInvalidated();
			});
		},
		rebind: function (pos) {
			var i;
			for (i in this.holders) {
				if (this.holders[i].pos == pos) {
					this.binder(this.holders[i], this.array[pos], parseInt(pos), this.array, this.params);
				}
			}
		},
		setArray: function (a) {
			var i;
			if (this.array != a) {
				if (this.readOnly) {
					// readOnly 适配器没有外部同步数组，直接换引用即可；
					// 这样 Accel.filterIDTexts 返回的 Java int[] 不用逐项转成 JS 数组。
					this.array = a;
					this.adapter._replaceArray(a);
				} else {
					this.array.length = a.length;
					for (i = 0; i < a.length; i++) this.array[i] = a[i];
				}
			}
			this.notifyChange(true);
		},
		setSync: function (a) {
			this.sync = a;
			this.notifyChange(false);
		}
	}
	r.getController = function (adapter) {
		var r = adapter.getItem(-1);
		r.self = adapter;
		return r;
	}
	return r;
})());
({
    sourceUrl: "https://cdn.jsdelivr.net/gh/codesenlin/CA@dev/libraries.json",

    sync: function(callback) {
        var self = this;
        Threads.run(function() { try {
            var index = JSON.parse(NetworkUtils.queryPage(self.sourceUrl));
            var changed = false;
            (index.libraries || []).forEach(function(item) {
                try {
                    var uri = "remote://" + item.uuid;
                    if (CA.Library.inner[uri]) return;
                    var code = NetworkUtils.queryPage(item.url);
                    var obj = eval("(" + code + ")");
                    if (!obj || typeof obj !== "object") throw "无效：" + item.name;
                    CA.Library.inner[uri] = obj;
                    // 走版本判断：本地已有更高版本时保留本地的
                    CA.Library.addLibrary(uri);
                    changed = true;
                } catch(e) {
                    Log.e(e);
                }
            });
            // 新加入的远程库也要排在它的前提包之后
            if (changed) CA.Library.orderLibraries();
            if (callback) callback(changed);
        } catch(e) {
            // 网络不通（jsDelivr 被 reset 之类）属于常见情况，静默记日志即可，别弹上报框
            erp(e, true);
            if (callback) callback(false);
        }});
    }
})

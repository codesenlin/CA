({
    sourceUrl: "https://cdn.jsdelivr.net/gh/codesenlin/CA@main/libraries.json",

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
                    CA.Library.enableLibrary(uri);
                    changed = true;
                } catch(e) {
                    Log.e(e);
                }
            });
            if (callback) callback(changed);
        } catch(e) {
            erp(e);
            if (callback) callback(false);
        }});
    }
})

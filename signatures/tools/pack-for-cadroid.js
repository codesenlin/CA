const fs = require("fs");
const path = require("path");
const zlib = require("zlib");
const os = require("os");

// 配置（输入/证书相对 signatures/ 目录；输出可传参覆盖）
const BASE = path.join(__dirname, "..");
const INPUT_JS = path.join(BASE, "build", "main.js");
const KEY_PEM  = path.join(BASE, "keys", "testkey.x509.pem");
const OUTPUT   = process.argv[2] || "~/StudioProjects/cadroid/app/src/main/assets/script.js";

// 展开 ~
function expandHome(p) {
    if (p.startsWith("~/")) return path.join(os.homedir(), p.slice(2));
    return p;
}

// 1. 读 JS 源码
const js = fs.readFileSync(INPUT_JS);

// 2. GZIP 压缩
const compressed = zlib.gzipSync(js);

// 3. 读 PEM
const pem = fs.readFileSync(KEY_PEM, "utf-8");

// 4. 从 PEM 提取 DER（去掉 -----BEGIN/END----- 和换行，再 base64 解码）
const b64 = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
const certDer = Buffer.from(b64, "base64");

// 5. 逐位 XOR
const result = Buffer.alloc(compressed.length);
for (let i = 0; i < compressed.length; i++) {
    result[i] = compressed[i] ^ certDer[i % certDer.length];
}

// 6. 写出
const outPath = expandHome(OUTPUT);
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, result);

console.log(`生成完成，大小 ${result.length} 字节`);
console.log(`输出：${outPath}`);
console.log(`密钥长度：${certDer.length} 字节`);

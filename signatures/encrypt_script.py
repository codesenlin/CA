import gzip
import base64
import re

# ===== 配置 =====
RAW_JS = 'main.js'          # cabuild 生成的原始 JS
PEM    = 'testkey.x509.pem'            # 你签名 cadroid 用的证书
OUTPUT = 'script.js'

# 1. 读取原始 JS
with open(RAW_JS, 'rb') as f:
    js = f.read()
print(f"原始 JS: {len(js)} 字节")

# 2. GZIP 压缩
compressed = gzip.compress(js)
print(f"GZIP 后: {len(compressed)} 字节")

# 3. 从 PEM 提取证书 DER
with open(PEM, 'rb') as f:
    pem = f.read()
b64 = re.sub(rb'-----[^-]+-----', b'', pem).strip()
cert_der = base64.b64decode(b64)
print(f"证书 DER: {len(cert_der)} 字节")

# 4. 逐字节异或
result = bytearray(compressed)
for i in range(len(result)):
    result[i] ^= cert_der[i % len(cert_der)]

# 5. 写入
with open(OUTPUT, 'wb') as f:
    f.write(result)
print(f"输出: {OUTPUT} ({len(result)} 字节)")

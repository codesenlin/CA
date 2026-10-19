import gzip
import base64
import re
import sys

# ===== 用法 =====
#   python3 decrypt_script.py <加密的 script.js> <证书文件>
# 证书可以是 PEM（x509）或已经是 DER；密钥就是 CADroid 运行时读到的
# PackageManager 签名证书字节（见 encrypt_script.py 第 3~4 步）。
#
# 官方包（原作者）用的是他自己的签名证书，可以从官方 APK 里取出来：
#   unzip -o release.apk "META-INF/*.RSA"
#   openssl pkcs7 -inform DER -in META-INF/APPKEY.RSA -print_certs -out author-cert.pem
#   openssl x509 -in author-cert.pem -outform DER -out author-cert.der      # 407 字节
#
# 校验是不是同一张证书：sha256 的 base64（cadroid 的 verifyApk 里记过原作者的）
#   HmzSXz/O6M/qIPo8mvhmFuXusTaKk3caC/vjP+ymxzw=

SRC = sys.argv[1] if len(sys.argv) > 1 else 'script.js'
KEY = sys.argv[2] if len(sys.argv) > 2 else 'testkey.x509.pem'

def load_der(path):
    data = open(path, 'rb').read()
    if data[:1] == b'\x30':                      # 已经是 DER
        return data
    b64 = re.sub(rb'-----[^-]+-----', b'', data)  # PEM -> DER
    return base64.b64decode(re.sub(rb'\s+', b'', b64))

key = load_der(KEY)
data = open(SRC, 'rb').read()
print(f'密文: {len(data)} 字节')
print(f'密钥: {KEY} ({len(key)} 字节)')

# 逐字节异或（和加密同一个操作）
plain = bytes(b ^ key[i % len(key)] for i, b in enumerate(data))
if plain[:2] != b'\x1f\x8b':
    print('解出来不是 gzip，密钥不对')
    sys.exit(1)

js = gzip.decompress(plain).decode('utf-8')
out = SRC.rsplit('.', 1)[0] + '.decrypted.js'
open(out, 'w', encoding='utf-8').write(js)
print(f'解密成功: {len(js)} 字符 -> {out}')

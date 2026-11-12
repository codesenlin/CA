import fs from "node:fs";

// Node 14.14+ 的 fs.rmSync 自带递归删除和 force（不存在也不报错），
// 不用再手写 rmdir 递归。
export default function (context, args) {
	fs.rmSync("./build", { recursive: true, force: true });
}
export const input = "cli";

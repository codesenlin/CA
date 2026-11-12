"ui";

// Kotlin/JS bundle 的 UMD 包装使用 globalThis；Rhino 没有该全局时补一个。
if (typeof globalThis === "undefined") {
	var globalThis = this;
}
/*
	Command Assistant (命令助手)
	Copyright (C) 2017-2019  ProjectXero
	E-mail: projectxero@163.com

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program. If not, see [http://www.gnu.org/licenses/].
*/

// 宿主入口：按顺序展开 bootstrap 段，最后执行模块加载与生命周期。
// 这些 Loader.fromFile 由 cabuildtools 的 Loader AST 在构建期展开；
// 生成后的 assets/script.js 里就是完整脚本，不再依赖运行时 Loader。
Loader.fromFile("./bootstrap/hooks.cjs")
Loader.fromFile("./bootstrap/mapscript.cjs")
Loader.fromFile("./bootstrap/context.cjs")
Loader.fromFile("./bootstrap/logging.cjs")
Loader.fromFile("./bootstrap/loader.cjs")
Loader.fromFile("./bootstrap/init.cjs")

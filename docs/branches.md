# 分支约定

> 这个仓库同时承载「原作者官方版」和「我们的改造」，两条线不能混。
> 推错分支会把官方版覆盖掉，所以约定写在这里。

## 一、四个分支的职责

| 分支 | 归属 | 用途 | 能不能推 |
|---|---|---|---|
| `main` | **原作者官方版** | 上游发布内容，保持原样 | **不要推** |
| `dev` | 我们 | 开发主线，所有改造都落在这里 | 可以推 |
| `canary` | 我们 | 前沿版，跟 `dev` 对齐 | 可以推 |
| `beta` | 我们 | 测试版，跟 `dev` 对齐 | 可以推 |

`main` 是官方版的镜像，**任何情况下都不要往里推**。我们要发布的东西一律走 `dev → canary / beta`。

## 二、日常流程

```bash
# 1. 改完 → 先在 dev 上提交
git add -A && git commit -m "..."

# 2. 三个分支一起推（都是快进，不会冲突）
git push origin dev:dev dev:canary dev:beta
```

三个分支指向同一个提交是**正常状态**，不是失误。这样：

- 不会出现「dev 有、canary 没有」的中间态
- 不会每次推 `beta` 都卡在非快进上

## 三、同步官方更新

官方版更新后，把它合进 `dev`，不要反过来：

```bash
git fetch origin main

git log --oneline dev..origin/main   # 官方有哪些新提交
git log --oneline origin/main..dev   # 我们比官方多了什么

git merge origin/main                # 在 dev 上合并官方改动
git push origin dev:dev dev:canary dev:beta
```

合并可能出现冲突——因为我们改过的地方（`modules/CA.js`、`modules/CA.Library.js`）正是官方也在维护的。冲突时**保留双方意图**，不要图省事直接 `--ours`。

## 四、如果 beta 又分叉了

历史上有过这种情况：`beta` 上有些提交 `dev` 里没有（比如一份只发在 beta 的文档），这时推 `dev` 会是非快进。

**优先合并，不要强推**：

```bash
git tag backup-beta-$(date +%Y%m%d) origin/beta   # 先留个标记
git merge origin/beta                              # 合进 dev
git push origin dev:dev dev:canary dev:beta
```

强推（`--force-with-lease`）会从远程抹掉那些提交，只在**确认内容已经没用**时才用，而且要先打 tag。

## 五、当前状态（2026-10-24）

```
main      d7c1bc3   原作者官方版，未改动
dev       dc04305   我们的开发线
canary    dc04305
beta      dc04305
```

本地有一个备份标签 `backup-beta-20261024`，指向合并前的 `beta`。

## 六、相关仓库

`cadroid`（Android 外壳）是独立仓库，分支只有 `main`：

```bash
git push origin master:main
```

它的 `master` 跟踪的是 `origin/main`（远程没有 `master` 分支）。

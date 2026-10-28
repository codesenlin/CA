# 分支约定

> 这个仓库的分支有着不同的角色：`main` 是历史主线（停更），`dev` 是现在的开发线，
> `canary` / `beta` 跟随 `dev`。推错分支会把别人的线覆盖掉，所以约定写在这里。

## 一、四个分支的职责

| 分支 | 角色 | 用途 | 能不能推 |
|---|---|---|---|
| `main` | 历史主线 | fork 的默认分支，内容停在 2026-10-18（旧的 `feat(library)` 那一批） | **不要再推** |
| `dev` | 开发主线 | 所有新改动都落在这里 | 可以推 |
| `canary` | 前沿版 | 跟 `dev` 对齐 | 可以推 |
| `beta` | 测试版 | 跟 `dev` 对齐 | 可以推 |

`main` 早期当过主线，后来切到 `dev`，两边已经分叉 84 个提交。
**不要再往 `main` 推**：它是别人（包括 GitHub 默认页）看到的那份，也当作对照用。
新的东西一律走 `dev → canary / beta`。

上游作者的原仓库没有配置成 remote；如果以后要同步，自行 `git remote add upstream …`，
流程见下面第三节。

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

如果要把上游作者的改动合进来，先把上游加成 remote：

```bash
git remote add upstream <上游仓库地址>
git fetch upstream

git log --oneline dev..upstream/main   # 上游有哪些新提交
git log --oneline upstream/main..dev   # 我们多了什么

git merge upstream/main                # 在 dev 上合并上游改动
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

## 五、当前状态（2026-10-28）

```
main      d7c1bc3   历史主线（停在 2026-10-18）
dev       （当前）  开发线，领先 main 84 个提交
canary    （远程）  最近一次推送是 a6e9afd
beta      （远程）  最近一次推送是 a60d3e5
```

本地只有 `dev` 一个分支（整理时删掉了已经并入 `dev` 的备份分支和标签）。
`dev` 上还有一批未推送的提交，推送时机自己把握。

## 六、相关仓库

`cadroid`（Android 外壳）是独立仓库，分支只有 `main`：

```bash
git push origin master:main
```

它的 `master` 跟踪的是 `origin/main`（远程没有 `master` 分支）。

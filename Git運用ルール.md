# Git 運用ルール

## 基本構成

-   メインブランチ: `main`
-   個人作業用ブランチ: `dev_name` (例: `dev_tanaka`)

## 他の人の変更を取り込む場合の作業フロー

### 1. ローカルの main ブランチを最新化する

```bash
# main ブランチに切り替え
git checkout main

# リモートの変更を取得
git pull origin main
```

### 2. 作業ブランチに最新の main の内容を取り込む

```bash
# 作業ブランチに切り替え
git checkout dev_name

# main の内容をマージ
git merge main
```

## 自分の変更を push する場合の作業フロー

### 1. 変更内容を確認

```bash
# 変更状態の確認
git status

# 変更差分の確認
git diff
```

### 2. 変更をコミット

```bash
# 変更をステージング
git add <変更したファイル>

# または全ての変更をステージング
git add .

# コミット
git commit -m "変更内容の説明"
```

### 3. リモートの変更を取り込む（プッシュ前に最新化）

```bash
# main の最新化
git checkout main
git pull origin main

# 作業ブランチに戻り main の変更を取り込む
git checkout dev_name
git merge main # または `git rebase main`
```

### 4. リモートにプッシュ

```bash
# 初めてリモートにプッシュする場合
git push -u origin dev_name

# 2 回目以降
git push origin dev_name
```

### 5. プルリクエスト作成

1. GitHub のリポジトリページにアクセス
2. 「Pull requests」タブをクリック
3. 「New pull request」ボタンをクリック
4. base: `main` ← compare: `dev_name` を選択
5. 内容を確認し、「Create pull request」をクリック
6. タイトルと説明を入力
7. 「Create pull request」をクリック

## 重要なルール

-   直接`main`ブランチへコミット・プッシュしない
-   プッシュする前に必ずローカルテストを実行する
-   こまめにコミットとプッシュを行う
-   コミットメッセージは具体的に記述する
-   プルリクエストのマージ後は不要になった作業ブランチを削除する

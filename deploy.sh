#!/bin/bash
#####################################################################
# Hidariude デプロイスクリプト (AlmaLinux 10)
# 
# 概要: Gitリポジトリからプルし、Mavenでビルドして
#       Tomcat 10にWARファイルをデプロイします
#
# 前提条件:
# - Java 24, Maven 3.9.11, Tomcat 10.1.46 がインストール済み
# - PostgreSQL 15 がポート5433で稼働中
# - データベース「hidariude」が作成済み
#####################################################################

set -e  # エラー時に即座に終了

# ログ出力用の色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ログ関数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

#####################################################################
# 環境変数設定
#####################################################################

export JAVA_HOME=/usr/lib/jvm/jdk-24.0.2-oracle-x64
export CATALINA_HOME=/opt/tomcat/apache-tomcat-10.1.46
export MAVEN_HOME=/opt/apache-maven-3.9.11
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH

# プロジェクト設定
PROJECT_NAME="hidariude"
REPO_DIR="/opt/${PROJECT_NAME}"
WAR_FILE_ORIGINAL="${PROJECT_NAME}-0.0.1-SNAPSHOT.war"
WAR_FILE_DEPLOY="${PROJECT_NAME}.war"
TOMCAT_USER="tomcat"  # Tomcatの実行ユーザー（環境に合わせて変更）

#####################################################################
# 事前確認
#####################################################################

log_info "=== 環境確認 ==="

# Java確認
if [ ! -d "$JAVA_HOME" ]; then
    log_error "JAVA_HOME が見つかりません: $JAVA_HOME"
    exit 1
fi
log_info "Java バージョン: $(java -version 2>&1 | head -n 1)"

# Maven確認
if [ ! -d "$MAVEN_HOME" ]; then
    log_error "MAVEN_HOME が見つかりません: $MAVEN_HOME"
    exit 1
fi
log_info "Maven バージョン: $(mvn -version | head -n 1)"

# Tomcat確認
if [ ! -d "$CATALINA_HOME" ]; then
    log_error "CATALINA_HOME が見つかりません: $CATALINA_HOME"
    exit 1
fi
log_info "Tomcat ディレクトリ: $CATALINA_HOME"

# PostgreSQL確認
if ! systemctl is-active --quiet postgresql-15; then
    log_warn "PostgreSQL が起動していません。起動を試みます..."
    systemctl start postgresql-15
    sleep 3
fi
log_info "PostgreSQL ステータス: $(systemctl is-active postgresql-15)"

#####################################################################
# Gitリポジトリ操作
#####################################################################

log_info "=== Gitリポジトリ更新 ==="

if [ ! -d "$REPO_DIR" ]; then
    log_warn "リポジトリが見つかりません: $REPO_DIR"
    log_info "リポジトリをクローンしてください:"
    log_info "  cd /opt"
    log_info "  git clone <repository-url> ${PROJECT_NAME}"
    exit 1
fi

cd "$REPO_DIR"
log_info "作業ディレクトリ: $(pwd)"

# Gitブランチ確認
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
log_info "現在のブランチ: $CURRENT_BRANCH"

# Git pull
log_info "最新のコードを取得中..."
git fetch origin
git pull origin "$CURRENT_BRANCH"

COMMIT_HASH=$(git rev-parse --short HEAD)
log_info "デプロイするコミット: $COMMIT_HASH"

#####################################################################
# Mavenビルド
#####################################################################

log_info "=== Mavenビルド開始 ==="

# クリーンビルド
log_info "既存のビルド成果物をクリーンアップ..."
mvn clean

log_info "WARファイルをビルド中..."
mvn package -DskipTests

# ビルド成果物の確認
if [ ! -f "target/${WAR_FILE_ORIGINAL}" ]; then
    log_error "WARファイルが生成されませんでした: target/${WAR_FILE_ORIGINAL}"
    exit 1
fi

WAR_SIZE=$(du -h "target/${WAR_FILE_ORIGINAL}" | cut -f1)
log_info "WARファイル生成完了: ${WAR_FILE_ORIGINAL} (${WAR_SIZE})"

#####################################################################
# Tomcat停止
#####################################################################

log_info "=== Tomcatの停止 ==="

if systemctl is-active --quiet tomcat; then
    log_info "Tomcatを停止中..."
    systemctl stop tomcat
    
    # Tomcatが完全に停止するまで待機
    WAIT_COUNT=0
    while systemctl is-active --quiet tomcat && [ $WAIT_COUNT -lt 30 ]; do
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
    done
    
    if systemctl is-active --quiet tomcat; then
        log_error "Tomcatの停止に失敗しました"
        exit 1
    fi
    log_info "Tomcat停止完了"
else
    log_info "Tomcatは既に停止しています"
fi

#####################################################################
# 既存アプリケーションの削除
#####################################################################

log_info "=== 既存アプリケーションの削除 ==="

# webappsディレクトリ内の既存アプリケーションを削除
if [ -d "$CATALINA_HOME/webapps/${PROJECT_NAME}" ]; then
    log_info "既存のデプロイディレクトリを削除: $CATALINA_HOME/webapps/${PROJECT_NAME}"
    rm -rf "$CATALINA_HOME/webapps/${PROJECT_NAME}"
fi

if [ -f "$CATALINA_HOME/webapps/${WAR_FILE_DEPLOY}" ]; then
    log_info "既存のWARファイルを削除: $CATALINA_HOME/webapps/${WAR_FILE_DEPLOY}"
    rm -f "$CATALINA_HOME/webapps/${WAR_FILE_DEPLOY}"
fi

# 古いバージョン付きWARファイルも削除（念のため）
if [ -f "$CATALINA_HOME/webapps/${WAR_FILE_ORIGINAL}" ]; then
    log_info "既存のバージョン付きWARファイルを削除: $CATALINA_HOME/webapps/${WAR_FILE_ORIGINAL}"
    rm -f "$CATALINA_HOME/webapps/${WAR_FILE_ORIGINAL}"
fi

if [ -d "$CATALINA_HOME/webapps/${PROJECT_NAME}-0.0.1-SNAPSHOT" ]; then
    log_info "既存のバージョン付きディレクトリを削除: $CATALINA_HOME/webapps/${PROJECT_NAME}-0.0.1-SNAPSHOT"
    rm -rf "$CATALINA_HOME/webapps/${PROJECT_NAME}-0.0.1-SNAPSHOT"
fi

# workディレクトリのクリーンアップ（キャッシュ削除）
if [ -d "$CATALINA_HOME/work/Catalina/localhost/${PROJECT_NAME}" ]; then
    log_info "作業ディレクトリをクリーンアップ"
    rm -rf "$CATALINA_HOME/work/Catalina/localhost/${PROJECT_NAME}"
fi

#####################################################################
# 新しいWARファイルのデプロイ
#####################################################################

log_info "=== 新しいWARファイルのデプロイ ==="

log_info "WARファイルをコピー中（${WAR_FILE_ORIGINAL} → ${WAR_FILE_DEPLOY}）..."
cp "target/${WAR_FILE_ORIGINAL}" "$CATALINA_HOME/webapps/${WAR_FILE_DEPLOY}"

# パーミッション設定（Tomcatユーザーが読めるように）
if id "$TOMCAT_USER" &>/dev/null; then
    chown "$TOMCAT_USER:$TOMCAT_USER" "$CATALINA_HOME/webapps/${WAR_FILE_DEPLOY}"
    log_info "パーミッション設定完了 (所有者: $TOMCAT_USER)"
else
    log_warn "Tomcatユーザー '$TOMCAT_USER' が見つかりません。パーミッション設定をスキップします"
fi

log_info "WARファイルのデプロイ完了: $CATALINA_HOME/webapps/${WAR_FILE_DEPLOY}"

#####################################################################
# Tomcat起動
#####################################################################

log_info "=== Tomcatの起動 ==="

systemctl start tomcat

# Tomcatが起動するまで待機
log_info "Tomcatの起動を待機中..."
WAIT_COUNT=0
while ! systemctl is-active --quiet tomcat && [ $WAIT_COUNT -lt 30 ]; do
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done

if ! systemctl is-active --quiet tomcat; then
    log_error "Tomcatの起動に失敗しました"
    log_error "ログを確認してください: $CATALINA_HOME/logs/catalina.out"
    exit 1
fi

log_info "Tomcat起動完了"

# アプリケーションの展開を待機
log_info "アプリケーションの展開を待機中..."
WAIT_COUNT=0
while [ ! -d "$CATALINA_HOME/webapps/${PROJECT_NAME}" ] && [ $WAIT_COUNT -lt 60 ]; do
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 1))
done

if [ -d "$CATALINA_HOME/webapps/${PROJECT_NAME}" ]; then
    log_info "アプリケーションの展開完了"
else
    log_warn "アプリケーションディレクトリが見つかりません（展開に時間がかかっている可能性があります）"
fi

#####################################################################
# デプロイ完了
#####################################################################

log_info "=== デプロイ完了 ==="
log_info "プロジェクト: ${PROJECT_NAME}"
log_info "ブランチ: ${CURRENT_BRANCH}"
log_info "コミット: ${COMMIT_HASH}"
log_info "WARファイル: ${WAR_FILE_DEPLOY}"
log_info ""
log_info "アプリケーションURL:"
log_info "  http://localhost:8080/${PROJECT_NAME}"
log_info ""
log_info "アプリケーション確認:"
log_info "  ls -la $CATALINA_HOME/webapps/ | grep ${PROJECT_NAME}"
log_info ""
log_info "Tomcatログ:"
log_info "  tail -f $CATALINA_HOME/logs/catalina.out"
log_info ""
log_info "ステータス確認:"
log_info "  systemctl status tomcat"

exit 0


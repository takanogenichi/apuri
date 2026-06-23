.PHONY: help setup downv kiro install dev build test lint typecheck apk aab sdk-info

# デフォルトターゲット
.DEFAULT_GOAL := help

##@ ヘルプ
help: ## コマンド一覧を表示
	@echo ""
	@echo "apuri - Make コマンド一覧"
	@echo "=========================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""

##@ セットアップ（HOST で実行）
setup: ## DevContainer 用セットアップ（初回のみ・HOST で実行）
	@bash .devcontainer/setup.sh

downv: ## DevContainer の Docker 停止＆ボリューム削除（HOST で実行）
	@cd .devcontainer && docker compose down -v
	@echo "Docker 停止＆ボリューム削除完了"

# ----------------------------------------------------------------------
# kiro-cli
# ----------------------------------------------------------------------
KIRO_IDP    ?= https://d-95674b0b93.awsapps.com/start
KIRO_REGION ?= ap-northeast-1

##@ kiro（app コンテナ内で実行）
kiro: ## [app内] kiro-cli を起動 (未ログインなら自動でSSOログイン / opus-4.8 / trust-all)
	@kiro-cli whoami >/dev/null 2>&1 || \
		kiro-cli login --license pro --identity-provider $(KIRO_IDP) --region $(KIRO_REGION) --use-device-flow
	@kiro-cli chat --trust-all-tools

##@ 開発（app コンテナ内で実行 / package.json 整備後に利用）
install: ## pnpm install
	@pnpm install

dev: ## 開発サーバー起動
	@pnpm run dev

build: ## プロダクションビルド
	@pnpm run build

test: ## テスト実行
	@pnpm run test

lint: ## ESLint 実行
	@pnpm run lint

typecheck: ## TypeScript 型チェック
	@pnpm run typecheck

##@ Android（app コンテナ内で実行 / Gradle Wrapper を利用）
apk: ## デバッグ APK をビルド（./gradlew assembleDebug）
	@./gradlew assembleDebug

aab: ## リリース AAB をビルド（./gradlew bundleRelease）
	@./gradlew bundleRelease

sdk-info: ## Android SDK / JDK の情報を表示
	@echo "JAVA_HOME=$$JAVA_HOME"; java -version
	@echo "ANDROID_SDK_ROOT=$$ANDROID_SDK_ROOT"; sdkmanager --list_installed

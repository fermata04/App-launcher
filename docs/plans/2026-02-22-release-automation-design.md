# Release Automation Design

**Date:** 2026-02-22
**Scope:** GitHub Actions による MSI ビルド・リリース・SHA256SUMS.txt 生成の自動化

---

## 背景

現在のリリース手順はすべて手動：
1. `./gradlew packageMsi` でローカルビルド
2. `certutil -hashfile` で SHA256 計算・SHA256SUMS.txt 作成
3. `gh release create` + `gh release upload` でアセットをアップロード

SHA256SUMS.txt の付け忘れが発生しうるため、全工程を自動化する。

---

## アーキテクチャ

シングルワークフロー（`.github/workflows/release.yml`）。
`v*` タグの push をトリガーに `windows-latest` ランナーで実行。

```
git tag v1.2.0 && git push --tags
         ↓
[GitHub Actions / windows-latest]
1. JDK 17 セットアップ
2. ./gradlew packageMsi
3. certutil で SHA256 計算 → SHA256SUMS.txt 生成
4. gh release create（コミットログから自動ノート生成）
5. MSI + SHA256SUMS.txt をアップロード
```

---

## コンポーネント

### ワークフローファイル
- **パス:** `.github/workflows/release.yml`
- **トリガー:** `push: tags: ['v*']`
- **ランナー:** `windows-latest`（MSI ビルドに Windows 必須）

### ビルドステップ
- `actions/checkout@v4`
- `actions/setup-java@v4`（JDK 17 / Temurin）
- `./gradlew packageMsi`（出力: `build/compose/binaries/main/msi/*.msi`）

### SHA256 生成
- `certutil -hashfile <msi> SHA256` で計算
- 出力を `<filename>  <hash>` の標準形式で `SHA256SUMS.txt` に書き出す
  - 既存の `verifyFileHash` のパース処理（`UpdateChecker.kt`）と互換

### リリース作成
- `gh release create $TAG --generate-notes` で自動ノート付きリリース作成
- `gh release upload` で MSI と SHA256SUMS.txt をアップロード

---

## データフロー

```
tag: v1.2.0
  → build.gradle.kts の version と照合（任意）
  → MSI: AppLauncher-1.2.0.msi
  → SHA256SUMS.txt:
      <hash>  AppLauncher-1.2.0.msi
  → GitHub Release: v1.2.0
      assets: AppLauncher-1.2.0.msi, SHA256SUMS.txt
```

---

## エラーハンドリング

| ケース | 対応 |
|--------|------|
| ビルド失敗 | gradlew が非ゼロ終了 → ワークフロー失敗、リリース作成しない |
| MSI ファイルが見つからない | シェルスクリプトで存在チェック → 失敗 |
| リリースが既に存在する | `gh release create` が失敗 → ワークフロー失敗 |

---

## スコープ外（今回対応しない）

- コード署名（Windows コード署名証明書が必要）
- タグと `build.gradle.kts` バージョンの自動同期
- テストの自動実行（現状テストなし）

---

## 合わせて対応するタスク

**isActive 未使用警告の修正**（`DropTargetArea.kt:24`）
`DropTargetArea` コンポーザブルの `isActive: Boolean` パラメータが関数本体で未使用。
`@Suppress("UNUSED_PARAMETER")` アノテーション、またはパラメータ削除で対応。

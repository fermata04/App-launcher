# Design: 管理者として実行機能

**日付:** 2026-02-28
**ステータス:** 承認済み

---

## 概要

登録したアプリをアプリごとの設定で「常に管理者権限で起動」できるようにする。
UAC ダイアログは Windows 標準の動作に委ねる。

---

## アーキテクチャ

### 技術選択: PowerShell `Start-Process -Verb RunAs`

追加ライブラリ不要。Windows 標準の UAC フローを使用。

```
powershell.exe -NonInteractive -Command
  Start-Process -FilePath '<path>' [-ArgumentList '<args>'] -Verb RunAs
```

---

## 変更ファイル

### 1. `AppEntry.kt` — データモデル拡張

`runAsAdmin: Boolean = false` を追加。

- デフォルト `false` で既存 JSON データと後方互換性あり
- `@Serializable` に含まれるため保存・読み込みは自動対応

### 2. `ProcessLauncher.kt` — 起動ロジック拡張

`launch()` メソッドで `entry.runAsAdmin` を分岐。

- `true` の場合: PowerShell `Start-Process -Verb RunAs` に委譲
- `false` の場合: 既存の ProcessBuilder ロジックをそのまま使用
- 引数のエスケープ: シングルクォートを `''` に変換（PowerShell エスケープ規則）
- `.lnk` ・ `.exe` ・その他すべての拡張子で統一した PowerShell パスを使用

### 3. `EditAppDialog.kt` — トグル追加

タグセクションの直前に1行のトグル行を追加。

```
[AdminPanelSettings アイコン]  管理者として起動  [Switch]
```

---

## エラーハンドリング

- UAC キャンセル時: PowerShell プロセスが非ゼロ終了コードを返す。既存の `catch (e: Exception)` で吸収し `false` を返す（既存動作と同じ）。
- ユーザーに UAC キャンセルの通知は不要（キャンセルは意図的な操作のため）。

---

## テスト方針

- 既存ユニットテスト（`UpdateCheckerTest`）への影響なし
- 手動スモークテスト: EditAppDialog でトグル ON → 保存 → 起動 → UAC ダイアログが表示されること

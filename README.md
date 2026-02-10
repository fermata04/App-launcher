# App Launcher for Windows 11

シンプルで使いやすいアプリケーションランチャーです。Kotlin + Compose Multiplatformで開発されています。

## 機能

- **ドラッグ＆ドロップでアプリを登録**: 実行ファイル（.exe, .bat, .cmd, .lnk, .msc）をウィンドウにドラッグ＆ドロップするだけで登録できます
- **直感的な並び替え**: 登録したアプリをドラッグ＆ドロップで簡単に並び替えできます
- **スムーズなアニメーション**: 並び替え時にはわかりやすいアニメーションが表示されます
- **自動アイコン取得**: 登録したアプリのアイコンが自動的に表示されます
- **設定の自動保存**: アプリの登録情報は自動的に保存され、次回起動時に復元されます

## ビルド要件

- **JDK 17以上** (推奨: Amazon Corretto 17 または Eclipse Temurin 17)
- **Gradle 8.5以上** (Gradle Wrapperが同梱されています)
- **Windows 11** (MSIインストーラー生成のため)
- **WiX Toolset 3.x** (MSIインストーラー生成に必要)

## ビルド手順

### 1. WiX Toolsetのインストール

MSIインストーラーを生成するには、WiX Toolsetが必要です。

1. https://wixtoolset.org/releases/ から WiX v3.x をダウンロード
2. インストーラーを実行してインストール
3. システム環境変数のPATHにWiXのbinフォルダを追加（例: `C:\Program Files (x86)\WiX Toolset v3.14\bin`）

### 2. アプリケーションの実行（開発時）

```bash
# プロジェクトディレクトリで実行
./gradlew run
```

### 3. MSIインストーラーの生成

```bash
# MSIインストーラーを生成
./gradlew packageMsi
```

生成されたMSIファイルは `build/compose/binaries/main/msi/` に出力されます。

### 4. その他のパッケージング

```bash
# EXEインストーラーを生成
./gradlew packageExe

# 配布可能なアプリケーションを生成（インストーラーなし）
./gradlew createDistributable
```

## プロジェクト構造

```
app-launcher/
├── build.gradle.kts          # ビルド設定
├── settings.gradle.kts       # Gradle設定
├── gradlew                   # Gradle Wrapper (Unix)
├── gradlew.bat               # Gradle Wrapper (Windows)
└── src/
    └── main/
        ├── kotlin/
        │   └── com/applauncher/
        │       ├── Main.kt           # エントリポイント
        │       ├── model/
        │       │   ├── AppEntry.kt   # アプリエントリのデータモデル
        │       │   └── AppLauncherState.kt  # 状態管理
        │       ├── ui/
        │       │   ├── Theme.kt      # UIテーマ
        │       │   ├── MainScreen.kt # メイン画面
        │       │   ├── AppListItem.kt    # リストアイテム
        │       │   ├── EditAppDialog.kt  # 編集ダイアログ
        │       │   └── DropTargetArea.kt # ドロップターゲット
        │       └── util/
        │           ├── IconExtractor.kt   # アイコン抽出
        │           ├── ProcessLauncher.kt # プロセス起動
        │           └── DropTargetHandler.kt # D&Dハンドラー
        └── resources/
            └── icon.ico              # アプリアイコン（要追加）
```

## 使い方

1. **アプリの登録**
   - 実行ファイルをウィンドウにドラッグ＆ドロップ
   - または「+」ボタンから手動で追加

2. **アプリの起動**
   - リスト内のアプリをクリック
   - または再生ボタンをクリック

3. **アプリの並び替え**
   - リストアイテムの左側のドラッグハンドル（⋮⋮）をドラッグして上下に移動

4. **アプリの編集・削除**
   - 「⋮」メニューボタンをクリック
   - または右クリックでコンテキストメニューを表示

## 設定ファイルの場所

アプリの登録情報は以下の場所に保存されます：
```
%USERPROFILE%\.applauncher\apps.json
```

## ライセンス

MIT License

## 技術スタック

- Kotlin 1.9.21
- Compose Multiplatform 1.5.11
- Material Design 3
- Kotlinx Serialization
- Kotlinx Coroutines
"# App-launcher" 

# Arukundesu — 歩行記録・経路提案アプリ

大学のプログラミング実践の授業で制作したAndroidアプリです。位置情報を使って歩いた道のりを記録し、地図と履歴で振り返れます。

このリポジトリは、以前の初期テンプレートから、ローカルの授業作品をもとにした公開用ソースへ更新しました。授業提出用リポジトリの履歴は含めていません。

## 機能

| 機能 | 実装 |
| --- | --- |
| 歩行記録 | GPSの位置更新から距離と経過時間を記録 |
| 地図表示 | 歩いた経路をGoogle Maps上に表示 |
| 結果表示 | 距離、時間、平均速度、概算消費カロリーを表示 |
| 履歴 | Roomデータベースに記録を保存し、一覧から再表示 |
| 経路提案 | 現在地から目的地への徒歩ルートを表示 |
| お気に入り | 地図の長押しで目的地を登録し、端末内に保存 |

## 技術

Java / Android Views・XML / Google Maps SDK / Fused Location Provider / Room / Gradle Kotlin DSL。アプリの主な実装言語はJavaです。

- Android 7.0（API 24）以降
- compileSdk / targetSdk：35
- JDK 17、Android SDK 35、Gradle Wrapperを使用

## 起動方法

1. Android Studioでこのフォルダを開き、SDK 35と必要な依存関係を用意します。
2. `secrets.properties.example` を `secrets.properties` にコピーします。
3. 自分のGoogle CloudプロジェクトのAPIキーを設定します。
4. Gradleを同期し、位置情報とGoogle Play servicesを使える端末・エミュレーターで実行します。
5. 位置情報の権限を許可し、歩行記録を開始します。

```properties
MAPS_API_KEY=自分のMaps SDK for Android用キー
DIRECTIONS_API_KEY=自分のDirections API用キー
```

環境変数 `MAPS_API_KEY` / `DIRECTIONS_API_KEY` も使えます。環境変数を優先します。キーを設定しなくてもソースをビルドできますが、地図と経路提案には自分の設定が必要です。

`secrets.properties`、`local.properties`、署名鍵、ビルド成果物はGitで除外しています。公開ソースに実際のAPIキーは含まれていません。

Maps SDK用キーにはAndroidアプリのパッケージ名・署名証明書とAPIの制限を設定してください。Directions APIへの直接呼び出しは授業版の実装を残しています。APKに埋め込んだキーは秘密にはできないため、本番サービスでは認証付きサーバー経由の呼び出しを検討してください。設定ファイルへの分離は、GitHubへのキー混入を防ぐための変更です。

[GoogleのAPIセキュリティガイド](https://developers.google.com/maps/api-security-best-practices)

## ソースの入口

`app/src/main/java/jp/ac/gifu_u/info/matsui/arukunzesu/`

- `RecordActivity.java`：位置情報、距離計測、記録保存
- `ResultActivity.java`：歩行結果と経路表示
- `HistoryActivity.java`：履歴一覧
- `SuggestionActivity.java`：徒歩ルートとお気に入り
- `AppDatabase.java` / `WalkRecordDao.java`：データベース

## 制作・公開について

授業での制作物を紹介する公開版です。元の提出先は [ProgrammingJIssen2025/final-assignment-Richa0516](https://github.com/ProgrammingJIssen2025/final-assignment-Richa0516) です。APIキーを含む提出履歴は転載していません。

ルート提案の初期目的地は岐阜大学周辺です。距離・速度・消費カロリーは授業作品の計算であり、GPS誤差や端末状態の影響を受けます。GoogleのAPI利用には各サービスの利用条件が適用されます。端末での実地歩行は公開作業では検証していません。

この公開によって第三者の依存ライブラリに新しいライセンスを付与するものではありません。包括的なオープンソースライセンスは今回付与していません。

## 公開前の確認

2026年10月6日：実際のAPIキーを設定しない状態で、assembleDebugとtestDebugUnitTestが成功しました。公開ソースにAPIキーの実値がないことを確認しています。既存テストは初期の基本テストで、GPS・地図・経路提案の実機動作全体を保証するものではありません。


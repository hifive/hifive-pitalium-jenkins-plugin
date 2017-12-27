# Pitalium plugin  for Jenkins
## Description
Jenkins ( https://jenkins.io/ ) を用いてPitaliumのリグレッションテストを行った際の，Junitテスト結果ページを見やすくするプラグイン

- Pitaliumによって作成されたスクリーンショット画像をテストごとに抽出してテスト結果ページに表示
  - 成功，失敗にかかわらずテスト結果画像が見つかれば抽出します．
- パッケージやクラスレベルのページでは，配下のテスト結果の内，失敗したものの集計テーブルを表示
- 集計テーブルをクリックすることで，クリックしたセルの条件を用いてテスト結果の抽出が可能

## Installation and Usage
- Jenkinsに`Pitaliumプラグイン`をインストールする．
- プロジェクトにJunitプラグインを導入する．
- `設定`を開く
  -  `Junitテスト結果の設定`に，`Pitaliumプラグイン`を追加
  - テスト結果画像が格納されているディレクトリの，ワークスペースからの相対パスを記入（デフォルトは`results`）
- ビルドを実行すると，テスト結果のページに自動的に画像と結果集計テーブルが作成されます．

## Features
- `src`：ソースファイル
  - `java.org.jenkinsci.plugins`：結果画像の探索などを行うメインのスクリプトファイル
  - `resources`：設定画面や結果画面に追加されるGUIを設定したファイル
- `webapp`：テーブルの表示や結果の条件抽出など，結果画面で利用するスクリプトファイル．

### 画像ファイルの取得方針
画像ファイルの取得にあたって，テスト結果の出力にいくつかの仮定を置いています（デフォルト設定のままであれば問題ないはず）．

- テストごとの探索ディレクトリは，テストの標準出力に，主力先フォルダを記した次の行があることを仮定
`[Save TestResult] (etc\2017_08_23_20_12_25\CompareExcludeSingleElementTest\result.json)`
  - ここから`\日付\クラス名\`を抽出して利用しています．
- 探索画像ファイル名は，テストケース名の規則`テストメソッド名[Capabilities [{capability=var, capability=var...}]]`を仮定
  - ディレクトリ内から，次のパターン`テストメソッド名_*_{platform}_{browser}*png`と一致する画像ファイルを抽出します．

### License
This product is licensed under the Apache License, Version 2.0.  
http://www.apache.org/licenses/LICENSE-2.0

### About Pitalium
Our product "Pitalium" developer site is located at  
http://www.htmlhifive.com/conts/web/view/pitalium/
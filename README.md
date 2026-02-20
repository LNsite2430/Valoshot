# Valoshot

## 導入方法
1. **プラグインの導入**:
   - `build/libs/valoshot-1.0.0.jar` をサーバーの `plugins` フォルダに入れる
   - サーバーを起動する
2. **リソースパックの適用**:
   - サーバーリソースパックとして、同梱または別途配布されている `ValoshotResourcePack` を設定（RPを有効にしない場合鉄の桑として表示される）
   - クライアント側で「サーバーリソースパック」を有効にして接続

## コマンド一覧
権限（Permission）設定により、使用可能なコマンドが異なる

| コマンド | 説明 | 必要な権限 |
| :--- | :--- | :--- |
| **/valoshot list** | 登録されているすべての銃の一覧を表示 | `valoshot.use` (デフォルト: 全員) |
| **/valoshot give <ID>** | 指定したIDの銃を取得 | `valoshot.give` (デフォルト: 全員) |
| **/valoshot reload** | 設定ファイル (`config.yml`) をリロード | `valoshot.admin` (デフォルト: OP) |

## 権限 (Permissions)

| ノード | 説明 | デフォルト |
| :--- | :--- | :--- |
| **valoshot.use** | 基本的なコマンドの使用権限 | `true` (全員) |
| **valoshot.give** | 武器の取得 (`/valoshot give`) 権限 | `true` (全員) |
| **valoshot.admin** | 管理者機能 (`/valoshot reload`) 権限 | `op` (管理者のみ) |


## 設定 (config.yml)
武器の性能は `config.yml` でカスタマイズ可能

### 設定項目例
```yaml
guns:
  vandal:
    display_name: "&e&lVandal"    # ゲーム内表示名
    category: "RIFLE"             # 武器カテゴリ
    material: "IRON_HOE"          # ベースアイテム
    custom_model_data: 1001       # モデルID
    head_damage: 32.0             # ヘッドショットダメージ
    body_damage: 11.0             # 胴体ダメージ
    leg_damage: 9.0               # 脚ダメージ
    fire_rate: 2                  # 連射速度 (tick単位, 小さいほど速い)
    magazine_size: 30             # 装弾数
    reload_time: 2.5              # リロード時間 (秒)
    accuracy: 0.15                # 静止時の拡散率 (0に近いほど高精度)
    ads_accuracy: 0.05            # ADS時の拡散率
    recoil: 0.25                  # 縦反動の強さ
    can_ads: true                 # ADS可能か
    ads_zoom: 2.5                 # ズーム倍率
```

## 開発環境
- **Minecraft Version**: 1.20.4 (Paper)
- **Java Version**: 17
- **Dependencies**: Paper API

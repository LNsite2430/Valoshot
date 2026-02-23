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
 display_name: ゲーム内での表示名（&でカラーコード使用可能）
 category: 武器種 (SIDEARM, SMG, SG, RIFLE, DMR, SNIPER, LMG, HEAVY)
 material: 使用するアイテム素材 (例: IRON_HOE)
 custom_model_data: リソースパック用の数値
 head_damage: 頭部命中時のダメージ
 body_damage: 胴体命中時のダメージ
 leg_damage: 脚部命中時のダメージ
 damage: 上記3つが未設定時のデフォルト値
 fire_rate: 連射間隔（数値が大きいほど遅い。1 tick = 0.05秒単位）
 magazine_size: マガジン容量
 reload_time: リロード時間（秒）
 accuracy: 静止時の射撃精度（小さいほど集弾性が高い）
 ads_accuracy: ADS(ズーム)時の射撃精度
 movement_spread: 移動・空中時の精度ペナルティの基本値
 can_ads: 右クリック（または指定キー）でのADSが可能か
 ads_zoom: ズーム倍率 (1.0〜)
 full_auto: 押しっぱなしで連射するか
 pellets: 1回の発射で出る弾数（ショットガン等で使用）
 recoil: 縦方向の反動（跳ね上がり）の強さ
 recoil_factor: 反動全体の重さ（数値が大きいほど制御が困難）
 move_spread_mult: 移動中の精度低下倍率
 ads_move_spread_mult: ADS移動中の精度低下倍率
 air_spread_mult: 空中での精度低下倍率
 ads_air_spread_mult: ADS空中での精度低下倍率
======================================================
【銃の種類】
Rifle: category: "RIFLE"
Sniper: category: "SNIPER"
Submachine gun: category: "SMG"
Shotgun: category: "SG"
Knife: category: "KNIFE"
```

## 開発環境
- **Minecraft Version**: 1.20.4 (Paper)
- **Java Version**: 17
- **Dependencies**: Paper API

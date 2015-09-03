
## WebSocket Clientの実装

#### Java EE 7 WebSocket API

標準。使いにくい予感。Encoder/Decoerの考え方が・・・。

#### Vert.X WebSocket Client

エージェント側も非同期で作りこむならありかな。
センター側との相性はよさそう。
NettyレベルではあるんだろーけどVert.xレベルでプロキシに対応していなさそう。
Issueに上がってるんだけどピンと来てない感じ。

クライアント証明書に対応しているのがすごいいい。
API自体はわかりやすい。（ほぼWebSocketのまま）


#### NV WebSocket Client

個人が作っているっぽいライブラリ。

よさげ、シンプル（WebSocketそのままな感じ）

#### SockJSクライアントを使う

接続容易性の観点からはほんとはこれがいいね。センタ側も影響するけどvert.xはsockjs対応してたと思うし。
ただsockjsだとwebsocketのフレーミングが使えないかもしれない。
wockjs＋xのxの部分次第だけどwebstompとかだとそんな気がするし、lengthとかつけるものは
ヘッダー分冗長になる。

トンネルプロトコルのヘッダ圧縮するならwebsocket前提にしてフレーミングを使った方が効率よさそう。


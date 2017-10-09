# 雰囲気でlagomについて理解する
## lagomとは
ここが詳しい
[Lagom で学ぶ Reactive Microservices Architecture](https://www.slideshare.net/negokaz/lagom-reactive-microservices-architecture)

## 作ったもの
[lagom公式のサンプル](https://github.com/dotta/activator-lagom-scala-chirper)を参考にしながら、レンタカーのサービスを作ってみた。

### とりあえず動かしてみる
1. sbtをインストール
1. 起動する
    ```
    sbt runAll
    ```
1. 参照してみる
    ```
    curl  http://localhost:9000/api/car/1AAA111
    {"name":"NotFound","detail":"car 1AAA111 not fount"}
    ```
1. 車を登録してみる
    ```
    curl -X POST -d '{"number":"1AAA111", "name":"Mustang"}' http://localhost:9000/api/car
    curl  http://localhost:9000/api/car/1AAA111
    {"number":"1AAA111","name":"Mustang","status":"ready"}
    ```
1. 貸出中にしてみる
    ```
    curl -X POST -d '{"number":"1AAA111"}' http://localhost:9000/api/car/rent
    curl  http://localhost:9000/api/car/1AAA111
    {"number":"1AAA111","name":"Mustang","status":"rent"}
    ```
1. 返却してみる
    ```
    curl -X POST -d '{"number":"1AAA111"}' http://localhost:9000/api/car/return
    curl  http://localhost:9000/api/car/1AAA111
    {"number":"1AAA111","name":"Mustang","status":"ready"}
    ```
    
### コードを追ってみる

#### CarRentalService
descriptorにサービスを定義している。

#### CarRentalServiceImpl
サービスの実装クラス。基本的には、レジストリからエンティティを取得してコマンドを送付する、ってことをやってる。
[参考資料](https://www.slideshare.net/negokaz/lagom-reactive-microservices-architecture)によると、エンティティはメモリ上に分散配置されているらしい。

#### エンティティ、コマンド、イベント、ステート
超ざっくり言えば、エンティティにコマンドを送ったらイベントを発火してステートを更新する、イメージ。

#### CarEntity
上記のようなことを書いてある。あとコマンドを発行したときにステートの状態によってどんな処理をするか書いている（今回は車を登録しようとして、もし登録済みならエラー、くらいしか書いていない）
多分、こいつがActorになって、誰か（SuperVisor）に監視される。

#### CarCommands
車に発行できるコマンド（GetCarReplyだけはコマンドじゃない）
```scala
// 新規車両を登録する
case class Register(car: Car) extends PersistentEntity.ReplyType[Done] with CarCommand

// 車両情報を取得する
case class GetCar() extends PersistentEntity.ReplyType[GetCarReply] with CarCommand

// ???
case class GetCarReply(car: Option[Car]) extends Jsonable

// 車両を貸し出す
case class Rent(carId: String) extends PersistentEntity.ReplyType[Done] with CarCommand

// 車両を返却する
case class Return(carId: String) extends PersistentEntity.ReplyType[Done] with CarCommand
```

#### CarEvent
車に発行するイベント
```scala
// 登録イベント
case class CarRegistered(number: String, name: String, timestamp: Instant = Instant.now()) extends CarEvent

// 貸し出しイベント
case class CarRented(number: String, timestamp: Instant = Instant.now()) extends CarEvent

// 返却イベント
case class CarReturned(number: String, timestamp: Instant = Instant.now()) extends CarEvent

```

#### CarState
ステート

#### CarEventProcessor
ここが一番わからなかった。自分の中ではDDDでいうところのデータソース層に相当するものと認識。
prepareメソッドでテーブルの初期化と、PreparedStatementの登録をしている。
defineEventHandlersメソッドで各イベントが発火した際のコールバック関数をセットしている。
```scala
  override def defineEventHandlers(builder: EventHandlersBuilder): EventHandlers = {
    builder.setEventHandler(classOf[CarRented], processCarRented)
    builder.setEventHandler(classOf[CarReturned], processCarReturned)
    builder.build()
  }

  private def processCarRented(event: CarRented, offset: UUID) = {
    // 貸出イベントの永続化とか
  }

  private def processCarReturned(event: CarReturned, offset: UUID) = {
    // 返却イベントの永続化とか
  }
```

おそらく自分は設計ミスしてて、今回はcarテーブルは不要だと思われる（イベントをリプレイしてステートを作る、という考え方なのでステートを管理するテーブルは不要）
ただ、もし参照用のテーブルを作りたいときは必要（今回の参照機能はステートを参照している）

## わかってないこと
* ログにこんなエラーでる
    ```
    [error] a.a.OneForOneStrategy - car_number is not a column defined in this metadata
    java.lang.IllegalArgumentException: car_number is not a column defined in this metadata
    ```
* エンティティのシャーディングの設定、確認方法

## 所感

* CQRSの設計の参考になった
* 個人的には黒魔術部分をちゃんと理解したいので、lagomのサンプルで全体の動きを理解してから、裏で使われている個々のコンポーネント（Akka Cluster Shardingとか）を勉強するといいと思った
* CQLごりごり書かなきゃいけないとこ超辛い

## 参考資料まとめ

Lagom で学ぶ Reactive Microservices Architecture @ 第3回Reactive System Meetup in 西新宿
https://www.slideshare.net/negokaz/lagom-reactive-microservices-architecture

kencharosさん
https://qiita.com/kencharos/items/05a5916d5b8c8aae2c26
他

takezoeさん
http://takezoe.hatenablog.com/entry/2016/04/10/230237
他

公式（scala）
https://www.lagomframework.com/documentation/1.3.x/scala/Home.html

Twitter的なサンプル
https://github.com/lagom/activator-lagom-java-chirper
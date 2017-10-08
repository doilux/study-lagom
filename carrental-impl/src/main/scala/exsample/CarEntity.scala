/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import java.util.Optional

import akka.Done
import com.lightbend.lagom.javadsl.persistence.PersistentEntity

class CarEntity extends PersistentEntity[CarCommand, CarEvent, CarState] {

  // コマンド発行時の振る舞いを定義する
  override def initialBehavior(snapshotState: Optional[CarState]): Behavior = {
    val b = newBehaviorBuilder(snapshotState.orElseGet(() => CarState(Option.empty)))

    // 車両登録コマンドのハンドラ
    b.setCommandHandler(
      classOf[Register],
      (cmd: Register, ctx: CommandContext[Done]) => {
        state.car match {
          case Some(_) =>
            ctx.invalidCommand(s"Car ${entityId} is already created")
            ctx.done()
          case None =>
            val c = cmd.car
            val event = CarRegistered(c.number, c.name)
            ctx.thenPersist(
              event,
              (evt: CarRegistered) => ctx.reply(Done)
            )
        }
      })

    // 車両登録イベントのハンドラ
    b.setEventHandler(
      classOf[CarRegistered],
      (evt: CarRegistered) => CarState(Car(evt.number, evt.name, "ready")))

    // 貸し出しコマンドのハンドラ
    b.setCommandHandler(
      classOf[Rent],
      (cmd: Rent, ctx: CommandContext[Done]) => {
        state.car match {
          case None =>
            ctx.invalidCommand(s"Car ${entityId} is not created")
            ctx.done()
          case Some(car) if car.status == "rent" =>
            ctx.reply(Done)
            ctx.done()
          case Some(car) =>
            val event = CarRented(car.number)
            ctx.thenPersist(
              event,
              (evt: CarRented) => ctx.reply(Done))
        }
      })

    // 貸し出しイベントのハンドラ
    b.setEventHandler(
      classOf[CarRented],
      (evt: CarRented) => state.rent(evt.number))

    // 返却コマンドのハンドラ
    b.setCommandHandler(
      classOf[Return],
      (cmd: Return, ctx: CommandContext[Done]) => {
        state.car match {
          case None =>
            ctx.invalidCommand(s"Car ${entityId} is not created")
            ctx.done()
          case Some(car) if car.status == "ready" =>
            ctx.reply(Done)
            ctx.done()
          case Some(car) =>
            ctx.thenPersist(
              CarReturned(car.number),
              (evt: CarReturned) => ctx.reply(Done))
        }
      })

    // 返却イベントのハンドラ
    b.setEventHandler(
      classOf[CarReturned],
      (evt: CarReturned) => state.ret(evt.number))

    // 車両情報取得コマンド（クエリ？）のハンドラ
    b.setReadOnlyCommandHandler(
      classOf[GetCar],
      (cmd: GetCar, ctx: ReadOnlyCommandContext[GetCarReply]) =>
        ctx.reply(GetCarReply(state.car))
    )

    b.build()
  }

}
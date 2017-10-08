/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import akka.Done
import com.lightbend.lagom.javadsl.persistence.PersistentEntity
import com.lightbend.lagom.serialization.Jsonable


sealed trait CarCommand extends Jsonable

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

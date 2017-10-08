/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import java.time.Instant

import com.lightbend.lagom.javadsl.persistence.{AggregateEvent, AggregateEventTag}
import com.lightbend.lagom.serialization.Jsonable

object CarEvent {
  val Tag = AggregateEventTag.of(classOf[CarEvent])
}

sealed trait CarEvent extends AggregateEvent[CarEvent] with Jsonable {
  override def aggregateTag(): AggregateEventTag[CarEvent] = CarEvent.Tag
}

// 登録イベント
case class CarRegistered(number: String, name: String, timestamp: Instant = Instant.now()) extends CarEvent

// 貸し出しイベント
case class CarRented(number: String, timestamp: Instant = Instant.now()) extends CarEvent

// 返却イベント
case class CarReturned(number: String, timestamp: Instant = Instant.now()) extends CarEvent
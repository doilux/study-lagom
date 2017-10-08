/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import com.lightbend.lagom.serialization.Jsonable

case class CarState(car: Option[Car]) extends Jsonable {
  def rent(friendUserId: String): CarState = car match {
    case None => throw new IllegalStateException("status can't change before car is registered")
    case Some(c) =>
      CarState(Some(c.copy(status = "rent")))
  }

  def ret(friendUserId: String): CarState = car match {
    case None => throw new IllegalStateException("status can't change before car is registered")
    case Some(c) =>
      CarState(Some(c.copy(status = "ready")))
  }
}

object CarState {
  def apply(car: Car): CarState = CarState(Option(car))
}
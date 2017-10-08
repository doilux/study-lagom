/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import akka.NotUsed
import com.lightbend.lagom.javadsl.api.ScalaService.{pathCall, _}
import com.lightbend.lagom.javadsl.api.{Descriptor, Service, ServiceCall}


trait CarRentalService extends Service {

  def getCar(id: String): ServiceCall[NotUsed, Car]

  def addNewCar(): ServiceCall[Car, NotUsed]

  def rentCar(): ServiceCall[CarNumber, NotUsed]

  def returnCar(): ServiceCall[CarNumber, NotUsed]

  override def descriptor(): Descriptor = {
    // @formatter:off
    named("friendservice").withCalls(
      pathCall("/api/car/:id", getCar _),
      namedCall("/api/car", addNewCar _),
      namedCall("/api/car/rent", rentCar _),
      namedCall("/api/car/return", returnCar _)
    ).withAutoAcl(true)
    // @formatter:on
  }
}

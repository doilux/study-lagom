/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import javax.inject.Inject

import akka.{Done, NotUsed}
import com.lightbend.lagom.javadsl.api.ServiceCall
import com.lightbend.lagom.javadsl.api.transport.NotFound
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.javadsl.persistence.cassandra.{CassandraReadSide, CassandraSession}

import scala.concurrent.ExecutionContext

class CarRentalServiceImpl @Inject()(
                                      persistentEntities: PersistentEntityRegistry,
                                      readSide: CassandraReadSide,
                                      db: CassandraSession)(implicit ec: ExecutionContext) extends CarRentalService {

  // Needed to convert some Scala types to Java
  import converter.ServiceCallConverter._

  persistentEntities.register(classOf[CarEntity])
  readSide.register(classOf[CarEventProcessor])


  /*
    ask: PersistentEntitiesにコマンドを送る
   */

  override def getCar(id: String): ServiceCall[NotUsed, Car] = {
    request =>
      carEntityRef(id).ask[GetCarReply, GetCar](GetCar())
        .map(_.car.getOrElse(throw new NotFound(s"car $id not fount")))
  }

  override def addNewCar(): ServiceCall[Car, NotUsed] = {
    request =>
      carEntityRef(request.number).ask[Done, Register](Register(request))
  }

  override def rentCar(): ServiceCall[CarNumber, NotUsed] = {
    request =>
      carEntityRef(request.number).ask[Done, Rent](Rent(request.number))
  }

  override def returnCar(): ServiceCall[CarNumber, NotUsed] = {
    request =>
      carEntityRef(request.number).ask[Done, Return](Return(request.number))
  }


  private def carEntityRef(id: String) =
    persistentEntities.refFor(classOf[CarEntity], id)
}
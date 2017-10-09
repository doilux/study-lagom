/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag
import com.lightbend.lagom.javadsl.persistence.cassandra.{CassandraReadSideProcessor, CassandraSession}

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class CarEventProcessor @Inject()(implicit ec: ExecutionContext) extends CassandraReadSideProcessor[CarEvent] {

  //  // Needed to convert some Scala types to Java
  import converter.CompletionStageConverters._

  //　事前にINSERT文をPreparedStatement化
  @volatile private var writeCar: PreparedStatement = null // initialized in prepare
  @volatile private var writeEvent: PreparedStatement = null // initialized in prepare
  @volatile private var updateCar: PreparedStatement = null // initialized in prepare
  @volatile private var writeOffset: PreparedStatement = null // initialized in prepare

  // 上記のsetter
  // @formatter:off
  private def setWriteCar(stmt: PreparedStatement): Unit = this.writeCar = stmt

  private def setWriteEvent(stmt: PreparedStatement): Unit = this.writeEvent = stmt

  private def setUpdateCar(stmt: PreparedStatement): Unit = this.updateCar = stmt

  private def setWriteOffset(stmt: PreparedStatement): Unit = this.writeOffset = stmt

  // @formatter:on

  override def aggregateTag: AggregateEventTag[CarEvent] = CarEvent.Tag

  // prepareにはテーブルの初期化処理を記述する
  override def prepare(session: CassandraSession) = {
    // @formatter:off
    prepareCreateTables(session).thenCompose(a =>
      prepareWriteCar(session).thenCompose(b =>
        prepareWriteEvent(session).thenCompose(c =>
          prepareWriteOffset(session).thenCompose(d =>
            selectOffset(session)
          ))))
    // @formatter:on
  }


  /*
    オフセットについての詳細
    https://qiita.com/kencharos/items/10fc88c4d3c9956d843c
   */
  private def prepareCreateTables(session: CassandraSession) = {
    // @formatter:off
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS car ("
        + "car_number text, name text, status text,"
        + "PRIMARY KEY (car_number))")
      .thenCompose(a => session.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS car_rent_event ("
          + "car_number text, at timestamp, type text,"
          + "PRIMARY KEY (car_number, at))"))
      .thenCompose(b => session.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS car_offset ("
          + "partition int, offset timeuuid, "
          + "PRIMARY KEY (partition))"))
    // @formatter:on
  }

  private def prepareWriteCar(session: CassandraSession) = {
    val statement = session.prepare("INSERT INTO car (car_number, name, status) VALUES (?, ?, ?)")
    statement.map(ps => {
      setWriteCar(ps)
      Done
    })
  }

  private def prepareWriteEvent(session: CassandraSession) = {
    val statement = session.prepare("INSERT INTO car_rent_event (car_number, at, type) VALUES (?, ?, ?)")
    statement.map(ps => {
      setWriteEvent(ps)
      Done
    })
  }

  private def prepareUpdateCar(session: CassandraSession) = {
    val statement = session.prepare("UPDATE car set status = ? where car_number = ?")
    statement.map((ps) => {
      setUpdateCar(ps)
      Done.getInstance
    })
  }

  private def prepareWriteOffset(session: CassandraSession) = {
    val statement = session.prepare("INSERT INTO car_offset (partition, offset) VALUES (1, ?)")
    statement.map(ps => {
      setWriteEvent(ps)
      Done
    })
  }

  private def selectOffset(session: CassandraSession) = {
    val select = session.selectOne("SELECT offset FROM car_offset")
    select.map { maybeRow => maybeRow.map[UUID](_.getUUID("offset")) }
  }

  override def defineEventHandlers(builder: EventHandlersBuilder): EventHandlers = {
    builder.setEventHandler(classOf[CarRented], processCarRented)
    builder.setEventHandler(classOf[CarReturned], processCarReturned)
    builder.build()
  }

  private def processCarRegistered(event: CarRegistered, offset: UUID) = {
    val bindWriteCar = writeCar.bind
    bindWriteCar.setString("car_number", event.number)
    bindWriteCar.setString("name", event.name)
    bindWriteCar.setString("status", "rent")
    val bindWriteOffset = writeOffset.bind(offset)
    completedStatements(Seq(bindWriteCar, bindWriteOffset).asJava)
  }

  private def processCarRented(event: CarRented, offset: UUID) = {
    val bindWriteFollowers = writeEvent.bind()
    bindWriteFollowers.setString("car_number", event.number)
    bindWriteFollowers.setTimestamp("at", toTimestamp(offset))
    bindWriteFollowers.setString("type", "rent")
    val bindWriteOffset = writeOffset.bind(offset)
    val bindUpdateCar = updateCar.bind
    bindUpdateCar.setString("type", "ready")
    bindUpdateCar.setString("car_number", event.number)
    completedStatements(Seq(bindWriteFollowers, bindWriteOffset, bindUpdateCar).asJava)
  }

  private def processCarReturned(event: CarReturned, offset: UUID) = {
    val bindWriteFollowers = writeEvent.bind()
    bindWriteFollowers.setString("car_number", event.number)
    bindWriteFollowers.setTimestamp("at", toTimestamp(offset))
    bindWriteFollowers.setString("type", "ready")
    val bindWriteOffset = writeOffset.bind(offset)
    val bindUpdateCar = updateCar.bind
    bindUpdateCar.setString("type", "ready")
    bindUpdateCar.setString("car_number", event.number)
    completedStatements(Seq(bindWriteFollowers, bindWriteOffset, bindUpdateCar).asJava)
  }


  import java.util.UUID

  /** UUID type1 timestampe to unix epoch Timestamp */
  private def toTimestamp(type1UUID: UUID) = {
    val NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L
    new Timestamp((type1UUID.timestamp - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000L)
  }

}

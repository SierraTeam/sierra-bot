package com.inno.sierra.bot

import java.util.Date

import akka.actor.ActorRef
import com.inno.sierra.model.DbSchema

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration


class NotifierService ()(implicit ec: ExecutionContext) {

  def sendMessages(sender: ActorRef, timeframe: FiniteDuration): Future[Unit] = Future  {
    val tillDate = new Date(new Date().getTime + timeframe.toMillis)
    val events = DbSchema.getAllEventsTillDate(tillDate)
    println("events analyzed till " + tillDate + ": " + events) //TODO: change to log
    events.foreach(e => {
      sender ! e
    })
  }(ec)
}

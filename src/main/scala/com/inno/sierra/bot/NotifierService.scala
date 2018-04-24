package com.inno.sierra.bot

import java.util.Date

import akka.actor.ActorRef
import com.inno.sierra.model.DbSchema
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration


class NotifierService ()(implicit ec: ExecutionContext) extends LazyLogging {

  /**
    * This method is scheduled to execute each 10 seconds. It will collect events from DB and send this events to notification actor
    * @param sender reference to actor that will send messages
    * @param timeframe time period in which events to notify will be collected from db
    */
  def sendMessages(sender: ActorRef, timeframe: FiniteDuration): Future[Unit] = Future  {
    val tillDate = new Date(new Date().getTime + timeframe.toMillis)
    val events = DbSchema.getAllEventsTillDate(tillDate)

    //logger.debug("events analyzed till " + tillDate + ": " + events)
    events.foreach(e => {
      sender ! e
    })
  }(ec)
}

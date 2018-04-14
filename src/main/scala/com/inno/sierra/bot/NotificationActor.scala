package com.inno.sierra.bot

import akka.actor.Actor
import com.inno.sierra.model.{DbSchema, Event}
import com.typesafe.scalalogging.LazyLogging

class NotificationActor(bot: SierraBot) extends Actor with LazyLogging {
  /*lazy val token = ConfigFactory.load().getString("bot.token")*/

  private def sendNotification(event: Event): Unit = {
    val chatSession = DbSchema.getChatSessionByEventId(event.id)

    logger.debug("Notifying about " + event.name + ", chatsessionid: " + chatSession.csid)

    // TODO: handle the group in a different way
    val notification = "I want to remind you that you have an event '" +
      event.name + "' at " + event.beginDate

    bot.sendMessage(chatSession.csid, notification)

    event.isNotified = true
    DbSchema.update(event)
  }

  override def receive: Receive = {
    case x: Event ⇒ sendNotification(x)
    case _ ⇒ logger.debug("received unknown message")
  }
}

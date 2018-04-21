package com.inno.sierra.bot

import akka.actor.Actor
import com.inno.sierra.model.{ChatSession, DbSchema, Event}
import com.typesafe.scalalogging.LazyLogging

class NotificationActor(bot: SierraBot) extends Actor with LazyLogging {

  private def sendNotification(event: Event): Unit = {
    val chatSession = DbSchema.getChatSessionByEventId(event.id)

    logger.debug("Notifying about " + event.name + ", chatsessionid: " + chatSession.csid)

    val baseNotification = MessagesText.NOTIFICATION
      .format(event.name, event.beginDate.toLocalDateTime.format(Utils.datePattern))

    val notification =
      if (!chatSession.isGroup) {
        baseNotification
      } else {
        val members = ChatSession.getMembersOfGroup(chatSession.csid)
        val sb = new StringBuffer()
        members.foreach(m => sb.append("@" + m.alias + " "))
        sb.toString + baseNotification
      }

    bot.sendMessage(chatSession.csid, notification)

    event.isNotified = true
    DbSchema.update(event)
  }

  override def receive: Receive = {
    case x: Event ⇒ sendNotification(x)
    case _ ⇒ logger.debug("received unknown message")
  }
}

package com.inno.sierra.bot

import com.inno.sierra.model.{ChatSession, DbSchema, Event}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.api.{BotBase, RequestHandler, TelegramBot}
import info.mukel.telegrambot4s.methods.SendMessage

import scala.concurrent.{ExecutionContext, Future}

class RunnableNotification(event: Event, bot: SierraBot) {
  /*lazy val token = ConfigFactory.load().getString("bot.token")*/

  def sendNotification()(implicit ec:ExecutionContext): Future[Unit] = Future {
    val chatSession = DbSchema.getChatSessionByEventId(event.id)
    // TODO: change to log
    println("Notifying about " + event.name + ", chatsessionid: " + chatSession.csid)

    // TODO: handle the group in a different way
    val notification = "I want to remind you that you have an event '" +
      event.name + "' at " + event.beginDate

    bot.sendMessage(chatSession.csid, notification)

    event.isNotified = true
    DbSchema.update(event)
  }
}

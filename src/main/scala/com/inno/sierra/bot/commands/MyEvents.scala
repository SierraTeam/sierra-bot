package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.DbSchema
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.Message

object MyEvents extends LazyLogging {
  def execute(msg: Message): String = {
    val events = DbSchema.getAllUpcomingEventsForUser(msg.chat.id)
    if (events.isEmpty) {
      MessagesText.NO_EVENTS_FOUND
    } else {
      events.map(e => {
        e.beginDate + " -- " + e.endDate + " " + e.name
      }).reduce(_ + "\n" + _)
    }
  }
}
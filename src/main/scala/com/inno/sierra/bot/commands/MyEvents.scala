package com.inno.sierra.bot.commands

import com.inno.sierra.bot.{MessagesText, Utils}
import com.inno.sierra.model.DbSchema
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.Message

/**
  * Shows the list of events for the current chatsession.
  */
object MyEvents extends LazyLogging {

  /**
    * Executes the command.
    * @param msg  the message to process
    * @return response to the user
    */
  def execute(msg: Message): String = {
    val events = DbSchema.getAllUpcomingEventsForUser(msg.chat.id)
    if (events.isEmpty) {
      MessagesText.NO_EVENTS_FOUND
    } else {
      events.map(e => {
        e.id + ": " + e.beginDate.toLocalDateTime.format(Utils.datePattern) +
          " — " + e.endDate.toLocalDateTime.format(Utils.timePattern) + " " + e.name
      }).reduce(_ + "\n" + _)
    }
  }
}
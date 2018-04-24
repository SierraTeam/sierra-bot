package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.Message

/**
  * Shows the available commands.
  */
object Info extends LazyLogging {

  /**
    * Executes the command.
    * @param msg  the message to process
    * @return response to the user
    */
  def execute(msg: Message): String = {
    MessagesText.INFO
  }
}

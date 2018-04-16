package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import info.mukel.telegrambot4s.models.Message

object Info {
  def execute(msg: Message): String = {
    MessagesText.INFO
  }
}

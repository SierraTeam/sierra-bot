package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.ChatSession
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.{ChatType, Message}

object Subscribe extends LazyLogging {
  def execute(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (!chat.`type`.equals(ChatType.Private)) {
      ChatSession.addUserToGroup(chat.id, user.id, user.username.get)
      println(ChatSession.getAll(None))
      MessagesText.SUBSCRIBE_DONE
    } else {
      MessagesText.SUBSCRIBE_ALREADY
    }
  }
}

package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.{ChatSession, ChatState}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.{ChatType, Message}

object Start extends LazyLogging {
  def execute(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (!ChatSession.exists(chat.id)) {
      ChatSession.create(chat.id, user.username.get,
        !chat.`type`.equals(ChatType.Private), ChatState.Started)
      MessagesText.START_FIRST_TIME.format(user.firstName)

    } else {
      MessagesText.START_AGAIN.format(user.firstName)
    }
  }
}

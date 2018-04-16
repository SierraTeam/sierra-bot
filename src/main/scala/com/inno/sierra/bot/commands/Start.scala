package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.{ChatSession, ChatState}
import info.mukel.telegrambot4s.models.{ChatType, Message}

object Start {
  def execute(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (!ChatSession.exists(chat.id)) {
      ChatSession.create(chat.id, user.username.get,
        !chat.`type`.equals(ChatType.Private), ChatState.Start)
      MessagesText.START_FIRST_TIME.format(user.firstName)

    } else {
      MessagesText.START_AGAIN.format(user.firstName)
    }
  }
}

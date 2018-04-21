package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.{ChatSession, ChatState}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.{ChatType, Message}

object Start extends LazyLogging {
  def execute(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (chat.`type`.equals(ChatType.Private)) {
      createIfNotExist(chat.id, user.username.getOrElse(""), false, user.firstName)
    } else {
      createIfNotExist(chat.id, "", true, "")
      createIfNotExist(user.id, user.username.getOrElse(""), false, user.firstName)
    }
  }

  private def createIfNotExist(csid: Long, alias: String,
                       isGroup: Boolean, firstName: String) = {
    if (!ChatSession.exists(csid)) {
      ChatSession.create(csid, alias, isGroup, ChatState.Started)
      MessagesText.START_FIRST_TIME.format(firstName)

    } else {
      MessagesText.START_AGAIN.format(firstName)
    }
  }
}

package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.{ChatSession, ChatState}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.{ChatType, Message}

/**
  * Memorizes the user in the database.
  */
object Start extends LazyLogging {

  /**
    * Executes the command.
    * @param msg  the message to process
    * @return response to the user
    */
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

  /**
    * Helper method. Creates user or group chatsession
    * if it does not exist.
    * @param csid ChatSession id
    * @param alias  user's alias
    * @param isGroup  is it a group or a private chat
    * @param firstName  first name of the user if applicable
    * @return
    */
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

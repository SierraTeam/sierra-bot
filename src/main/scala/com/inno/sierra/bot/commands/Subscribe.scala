package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.ChatSession
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.{ChatType, Message}

/**
  * IN-GROPUP only.
  * Subscribes the user in order to keep track of him/her
  * for the group events (during organization and notifications).
  */
object Subscribe extends LazyLogging {

  /**
    * Executes the command.
    * @param msg  the message to process
    * @return response to the user
    */
  def execute(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    val members = ChatSession.getMembersOfGroup(chat.id)
    val isInGroup = members.exists(_.csid == user.id)

    if (chat.`type`.equals(ChatType.Private) || isInGroup) {
      MessagesText.SUBSCRIBE_ALREADY
    } else {
      ChatSession.addUserToGroup(chat.id, user.id, user.username.get)
      println(ChatSession.getMembersOfGroup(chat.id))
      MessagesText.SUBSCRIBE_DONE
    }
  }
}

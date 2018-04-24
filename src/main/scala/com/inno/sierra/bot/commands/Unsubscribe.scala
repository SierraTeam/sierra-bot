package com.inno.sierra.bot.commands

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.ChatSession
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.models.{ChatType, Message}

/**
  * Unsubscribes the user from participation in
  * group events (the schedule of this user will not be
  * used when planning the event, notifications will not be sent
  * to him/her).
  */
object Unsubscribe extends LazyLogging {

  /**
    * Executes the command.
    * @param msg  the message to process
    * @return response to the user
    */
  def execute(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (!chat.`type`.equals(ChatType.Private)) {
      ChatSession.removeUserFromGroup(chat.id, user.id)
      println(ChatSession.getMembersOfGroup(chat.id))
      MessagesText.UNSUBSCRIBE_DONE
    } else {
      MessagesText.UNSUBSCRIBE_CANNOT
    }
  }
}

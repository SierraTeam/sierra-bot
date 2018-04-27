package com.inno.sierra.bot.commands

import java.util.Calendar

import com.inno.sierra.bot.{MessagesText, Utils}
import com.inno.sierra.model.{ChatSession, ChatState, Event}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.api.Extractors
import info.mukel.telegrambot4s.models.{ChatType, Message}

import scala.collection.mutable.ListBuffer

/**
  * Suggests time, when the events can be conducted.
  * In groups - will show the available time slots for all the
  * subscribed users in the group.
  * In private chats - will show the available time for the user.
  */
object SuggestTime extends LazyLogging {

  /**
    * Executes the command.
    * @param msg  the message to process
    * @return response to the user
    */
  def execute(msg: Message): String = {
    val args = Extractors.commandArguments(msg).get
    val day =
      if (args.isEmpty) {
        Utils.simpleDateTimeFormat
          .parse(Utils.simpleDateTimeFormat.format(Calendar.getInstance().getTime))
      } else {
        Utils.simpleDateFormat.parse(args.head)
      }

    val user = msg.from.get
    val chat = msg.chat

    Start.execute(msg)

    val curChatSession = ChatSession.getByChatId(chat.id).get
    val chatSessions: List[ChatSession] =
      if (chat.`type`.equals(ChatType.Private)) {
        List[ChatSession](curChatSession)
      } else {
        curChatSession :: ChatSession.getMembersOfGroup(chat.id)
      }

    logger.debug(chatSessions.toString)

    val events = ListBuffer[Event]()
    chatSessions.foreach(cs =>
      {
        logger.debug("chatSession: " + cs)
        events.appendAll(ChatSession.getEventsForDay(cs.csid, day))
      })
    if (events.isEmpty) {
      MessagesText.SUGGESTTIME_DAY_FREE
    } else {
      val slots = Event.countFreeTimeSlots(events.toList, day)
      val sb = new StringBuffer(MessagesText.SUGGESTTIME_DONE)
      slots.foreach(s => sb.append(s.beginDate.toLocalDateTime.format(Utils.datePattern) +
        " - " + s.endDate.toLocalDateTime.format(Utils.datePattern) + "\n"))
      val result = sb.toString
      result.substring(0, result.length - 1)
    }
  }
}

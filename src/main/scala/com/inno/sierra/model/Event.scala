package com.inno.sierra.model

import java.sql.Timestamp

import org.squeryl.KeyedEntity
import java.util.Date

import com.inno.sierra.bot.Utils

case class Event private (
            var id: Long,
            var beginDate: Timestamp,
            var name: String,
            var endDate: Timestamp,
            var isNotified: Boolean = false) extends KeyedEntity[Long] {

  override def toString: String = {
    beginDate.toLocalDateTime.format(Utils.datePattern) + " - " +
      endDate.toLocalDateTime.format(Utils.datePattern) + ": " + name
  }
}

object Event {
  def create(chatId: Long, beginDate: Date, name: String, endDate: Date): Event = {
    val begin = new Timestamp(beginDate.getTime)
    val end = new Timestamp(endDate.getTime)
    val event = DbSchema.insert(new Event(0, begin, name, end))
    assignEventTo(event.id, chatId)
    event
  }

  def update(event: Event): Unit = {
    DbSchema.update(event)
  }

  def cancel(event: Event): Unit = {
    DbSchema.delete(event.id)
  }

  def assignEventTo(eventId: Long, chatId: Long): Unit = {
    val chatSession = DbSchema.getChatSessionByChatId(chatId).get
    val cse = ChatSessionEvents(eventId, chatSession.id)
    DbSchema.insert(cse)
  }

  def get(ids: Option[List[Long]]) = {
    DbSchema.getAll[Event](ids)
  }

  def getEarliest(tillDate: Date) = {
    DbSchema.getAllEventsTillDate(tillDate)
  }
}
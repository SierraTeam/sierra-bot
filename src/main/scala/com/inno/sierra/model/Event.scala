package com.inno.sierra.model

import java.sql.Timestamp

import org.squeryl.KeyedEntity
import java.util.Date

import scala.collection.mutable

case class Event private (
            var id: Long,
            var beginDate: Timestamp,
            var name: String,
            var endDate: Timestamp,
            var isNotified: Boolean = false) extends KeyedEntity[Long] {

  // TODO: make a better format
  override def toString: String = {
    beginDate + " - " + endDate + ": " + name
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

  def assignEventTo(eventId: Long, chatId: Long): Unit = {
    val chatSession = DbSchema.getChatSessionIdByChatId(chatId).get
    val cse = ChatSessionEvents(eventId, chatSession.id)
    DbSchema.insert(cse)
  }

  def get(ids: Option[mutable.Set[Long]]): mutable.Set[Event] = {
    DbSchema.getAllEvents(ids)
  }

  def getEarliest(tillDate: Date): mutable.Set[Event] = {
    DbSchema.getAllEventsTillDate(tillDate)
  }
}
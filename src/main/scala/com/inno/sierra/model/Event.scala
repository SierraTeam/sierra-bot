package com.inno.sierra.model

import org.squeryl.KeyedEntity
import java.util.Date
import scala.collection.mutable

case class Event private (
            var id: Long,
            var beginDate: Date,
            var name: String,
            var endDate: Date,
            var isNotified: Boolean = false) extends KeyedEntity[Long] {

}

object Event {
  def create(id: Long, beginDate: Date,
             name: String, endDate: Date): Event = {

    DbSchema.insert(new Event(id, beginDate, name, endDate))
  }

  def assignEventTo(eventId: Long, chatSessionId: Long): Unit = {
    val cse = new ChatSessionEvents(eventId, chatSessionId)
    DbSchema.insert(cse)
  }

  def get(ids: Option[mutable.Set[Long]]): mutable.Set[Event] = {
    DbSchema.getAllEvents(ids)
  }
}
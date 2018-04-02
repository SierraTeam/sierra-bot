package com.inno.sierra.model

import org.squeryl.KeyedEntity
import java.util.Date
import scala.collection.mutable

case class Event private (
            var id: Long,
            var time: Date,
            var name: String,
            var duration: Long
            ) extends KeyedEntity[Long] {

}

object Event {
  def create(id: Long, time: Date,
             name: String, duration: Long): Event = {

    DbSchema.insert(new Event(id, time, name, duration))
  }

  def assignEventTo(eventId: Long, chatSessionId: Long): Unit = {
    val cse = new ChatSessionEvents(eventId, chatSessionId)
    DbSchema.insert(cse)
  }

  def get(ids: Option[mutable.Set[Long]]): mutable.Set[Event] = {
    DbSchema.getAllEvents(ids)
  }
}
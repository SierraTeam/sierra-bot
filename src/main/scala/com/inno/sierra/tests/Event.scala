package com.inno.sierra.tests

import org.squeryl.KeyedEntity
import java.util.Date

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
}
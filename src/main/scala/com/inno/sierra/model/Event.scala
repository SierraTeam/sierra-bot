package com.inno.sierra.model

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

    new Event(id, time, name, duration)
  }
}
package com.inno.sierra.model

import java.sql.Timestamp

import org.squeryl.KeyedEntity
import java.util.{Calendar, Date}

import com.inno.sierra.bot.Utils

import scala.collection.mutable.ListBuffer

/**
  * Represents the scheduled event.
  * @param id
  * @param beginDate
  * @param name
  * @param endDate
  * @param isNotified
  */
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

/**
  * Represents a simple timeslot.
  * @param beginDate  begin date
  * @param endDate  end date
  */
case class TimeSlot(beginDate: Timestamp, endDate: Timestamp)

/**
  * Factory object for communication with the database.
  */
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

  def countFreeTimeSlots(events: List[Event], day: Date): List[TimeSlot] = {
    var slots = List[TimeSlot](getTimeSlotForDay(day))
    events.foreach(ev => {
      slots = countSlots(slots, ev)
      println("Slots at event: " + slots)
      slots
    })
    slots
  }

  private def countSlots(slots: List[TimeSlot], event: Event) = {
    val res = slots.flatMap(sl => splitSlot(sl, event))
    //println("phase new slots: " + res)
    res
  }

  private def splitSlot(slot: TimeSlot, event: Event) = {
    val newSlots = ListBuffer[TimeSlot]()
    var flag = false

    if (event.beginDate.after(slot.beginDate)
      && event.beginDate.before(slot.endDate)) {
      newSlots.append(TimeSlot(slot.beginDate, event.beginDate))
      flag = true
    }

    if (event.endDate.after(slot.beginDate)
      && event.endDate.before(slot.endDate)) {
      newSlots.append(TimeSlot(event.endDate, slot.endDate))
      flag = true
    }

    if (!flag) newSlots.append(slot)
    newSlots.toList
  }

  private def getTimeSlotForDay(day: Date) = {
    val c = Calendar.getInstance()
    c.setTimeInMillis(day.getTime)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val beginDate = new Timestamp(c.getTimeInMillis)

    c.set(Calendar.HOUR_OF_DAY, 23)
    c.set(Calendar.MINUTE, 59)
    c.set(Calendar.SECOND, 59)
    c.set(Calendar.MILLISECOND, 0)
    val endDate = new Timestamp(c.getTimeInMillis)

    TimeSlot(beginDate, endDate)
  }
}
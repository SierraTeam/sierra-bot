import java.sql.Timestamp
import java.util.{Calendar, Date}

import com.inno.sierra.bot.Utils
import com.inno.sierra.model.{ChatSession, ChatState, DbSchema, Event}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

object EventTest extends FlatSpec with MockFactory with Matchers {

  behavior of "The database"

  def main(args: Array[String]): Unit = {
    getFreeTimeSlots(events, Utils.simpleDateTimeFormat.parse("26.04.2018 00:00"))

  }

  case class Event(beginDate: Timestamp, endDate: Timestamp) {
    def timeSlot(): TimeSlot = {
      TimeSlot(beginDate, endDate)
    }
  }
  case class TimeSlot(beginDate: Timestamp, endDate: Timestamp)

  def ev(begin: String, end: String): Event = {
    val bDate = Utils.simpleDateTimeFormat.parse(begin).getTime
    val eDate = Utils.simpleDateTimeFormat.parse(end).getTime
    Event(new Timestamp(bDate), new Timestamp(eDate))
  }

  val events = List(
    ev("26.04.2018 10:00", "26.04.2018 12:00"),
    ev("26.04.2018 11:00", "26.04.2018 17:00"),
  )

  def getFreeTimeSlots(events: List[Event], day: Date) = {
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

    println("Search for the boundary: " + beginDate + " - " + endDate)
    println("Events are: " + events)

    var slots = List[TimeSlot](TimeSlot(beginDate, endDate))

    events.foreach(ev => {
      slots = countSlots(slots, ev)
      println("Slots at event: " + slots)
      slots
    })

    println("New slots: " + countSlots(slots, events.head))
  }

  def countSlots(slots: List[TimeSlot], event: Event) = {
    val res = slots.flatMap(sl => splitSlot(sl, event))
    //println("phase new slots: " + res)
    res
  }

  def splitSlot(slot: TimeSlot, event: Event) = {
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
}

package com.inno.sierra.bot.commands

import java.util.{Calendar, Date, GregorianCalendar}

import com.inno.sierra.bot.{MessagesText, MonthCalendar, SierraBot}
import com.inno.sierra.model.{ChatSession, ChatState, Event}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.api.Extractors
import info.mukel.telegrambot4s.methods.{DeleteMessage, EditMessageReplyMarkup, SendMessage}
import info.mukel.telegrambot4s.models._

import scala.concurrent.ExecutionContext

object KeepInMind2 extends LazyLogging {

  def execute(bot: SierraBot)(implicit msg: Message): Unit = {
    Start.execute(msg)

    val chatSession = ChatSession.getByChatId(msg.chat.id).get
    chatSession.chatState = ChatState.CreatingEventInputtingName
    chatSession.resetInputs()
    chatSession.save()

    bot.reply("Alright, a new event. How are we going to call it? Please choose a name for an event.")
  }

  def onMessage(bot: SierraBot)(implicit msg: Message, ec: ExecutionContext): Unit = {

    for {
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      if chatSession.chatState == ChatState.CreatingEventInputtingName
      _ <- msg.text
      command <- Extractors.textTokens(msg).map(_.head)
      if command != "/keepinmind2"
    } /* do */ {
      chatSession.inputEventName = msg.text
      chatSession.chatState = ChatState.CreatingEventInputtingParams

      val now = new GregorianCalendar
      val year = now.get(Calendar.YEAR)
      val month = now.get(Calendar.MONTH) + 1
      val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)

      chatSession.inputEventYear = Some(year)
      chatSession.inputEventMonth = Some(month)
      chatSession.inputEventDay = Some(dayOfMonth)
      chatSession.save()

      val calendar = createCalendar(year, month)

      bot.reply(
        "Good. Now let's choose a date for your event. Use the calendar widget to enter the planned date.",
        replyMarkup = Some(calendar)
      ).map { msg =>
        chatSession.inputCalendarMessageId = Some(msg.messageId)
        chatSession.save()
        msg
      }
    }

  }

  val CALENDAR_DAY_TAG = "calendar-day-"
  def calendarDayTag(s: String): String = CALENDAR_DAY_TAG + s

  // scalastyle:off method.length
  def createCalendar(year: Int, month: Int): InlineKeyboardMarkup = {
    val daysInWeek = Calendar.SATURDAY
    val daysInCalendarMax = 42
    val calendar: MonthCalendar = MonthCalendar(month, year)

    val header = Seq(
      InlineKeyboardButton.callbackData(
        s"${calendar.monthName} $year",
        "ignore"
      )
    )
    val daysOfWeek = List("M", "T", "W", "R", "F", "S", "U").map { dayOfWeek =>
      InlineKeyboardButton.callbackData(
        dayOfWeek,
        "ignore"
      )
    }
    val daySlotsInMonth: Seq[String] = Seq().padTo(calendar.dayOfWeek, "  ") ++
      calendar.daysInMonth.map(_.toString)
    val daySlotsInMonthPadded: Seq[String] = daySlotsInMonth match {
      case _ if daySlotsInMonth.lengthCompare(daysInCalendarMax - 7) > 0 =>  daySlotsInMonth.padTo(daysInCalendarMax, " ")
      case _ => daySlotsInMonth.padTo(daysInCalendarMax - 7, " ")
    }

    val body = daySlotsInMonthPadded.grouped(daysInWeek).map { weekSlots =>
      weekSlots.map { dayOfMonth =>
        InlineKeyboardButton.callbackData(
          dayOfMonth,
          dayOfMonth match {
            case " " => "ignore"
            case _ => calendarDayTag(dayOfMonth)
          }
        )
      }
    }.toSeq
    val footer = Seq(
      InlineKeyboardButton.callbackData("<", "month-previous"),
      InlineKeyboardButton.callbackData(" ", "ignore"),
      InlineKeyboardButton.callbackData(">", "month-next")
    )

    InlineKeyboardMarkup(
      Seq(
        header,
        daysOfWeek
      )
        ++
        body
        ++
        Seq(
          footer
        )
    )
  }
  // scalastyle:on method.length

  def onCallbackWithTagIgnore(bot: SierraBot)(implicit cbq: CallbackQuery): Unit = {
    bot.ackCallback()
  }

  def onCallbackWithTagMonth(bot: SierraBot)(implicit cbq: CallbackQuery): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
    } /* do */ {

      val year = chatSession.inputEventYear.get
      val month = chatSession.inputEventMonth.get

      val (yearNew: Int, monthNew: Int) = data match {
        case "next" =>
          month + 1 match {
            case monthPlusOne if monthPlusOne > 12 => (year + 1, 1)
            case _ => (year, month + 1)
          }
        case "previous" =>
          month - 1 match {
            case monthMinusOne if monthMinusOne < 1 => (year - 1, 12)
            case _ => (year, month - 1)
          }
      }

      chatSession.inputEventYear = Some(yearNew)
      chatSession.inputEventMonth = Some(monthNew)
      chatSession.save()

      val calendarMarkup = createCalendar(yearNew, monthNew)

      bot.request(
        EditMessageReplyMarkup(
          Some(ChatId(msg.source)), // msg.chat.id
          Some(msg.messageId),
          replyMarkup = Some(calendarMarkup)))
    }
  }

  def onCallbackWithTagCalendarDay(bot: SierraBot)(implicit cbq: CallbackQuery, ec: ExecutionContext): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
      Extractors.Int(dayOfMonth) = data
    } /* do */ {

      chatSession.inputEventYear = Some(chatSession.inputEventYear match {
        case Some(year) => year
        case None =>
          val calendar = Calendar.getInstance
          calendar.get(Calendar.YEAR)
      })
      chatSession.inputEventMonth = Some(chatSession.inputEventMonth match {
        case Some(month) => month
        case None =>
          val calendar = Calendar.getInstance
          calendar.get(Calendar.MONTH)
      })
      chatSession.inputEventDay = Some(dayOfMonth)
      chatSession.save()

      bot.request(
        SendMessage(
          ChatId(msg.source),
          chatSession.getEventDate
        )
      ).flatMap { msg =>

        val timepicker = createTimepicker()

        bot.request(
          SendMessage(
            ChatId(msg.source),
            "Please choose the time for the event",
            replyMarkup = Some(timepicker)
          )
        )

      }.map { msg =>
        chatSession.inputTimepickerMessageId = Some(msg.messageId)
        chatSession.save()
        msg
      }

    }
  }

  def createTimepicker(): InlineKeyboardMarkup = {

    // scalastyle:off magic.number
    val timepickerButtons = (0 to 23 flatMap { hh: Int =>
      List(0, 30) map { mm: Int =>
        InlineKeyboardButton.callbackData(
          "%02d:%02d".format(hh, mm),
          s"timepicker-$hh-$mm"
        )
      }
    }).toSeq.grouped(6).toSeq
    // scalastyle:on magic.number

    InlineKeyboardMarkup(timepickerButtons)
  }

  def onCallbackWithTagTimepicker(bot: SierraBot)(implicit cbq: CallbackQuery, ec: ExecutionContext): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
    } /* do */ {

      val time = data.split('-').map(_.toInt)
      val hour = time(0)
      val minutes = time(1)

      chatSession.inputEventHour = Some(hour)
      chatSession.inputEventMinutes = Some(minutes)
      chatSession.save()

      bot.request(
        SendMessage(
          ChatId(msg.source),
          chatSession.getEventTime
        )
      ).flatMap { msg =>

        val durationpicker = createDurationpicker()

        bot.request(
          SendMessage(
            ChatId(msg.source),
            "Please choose the time for the event",
            replyMarkup = Some(durationpicker)
          )
        )

      }.map { msg =>
        chatSession.inputDurationpickerMessageId = Some(msg.messageId)
        chatSession.save()
        msg
      }

    }
  }

  val DURATIONS = List(
    5 -> "5 min",
    10 -> "10 min",
    15 -> "15 min",
    30 -> "30 min",
    45 -> "45 min",
    60 -> "1 hour",
    90 -> "1.5 hours",
    120 -> "2.0 hours",
    180 -> "3.0 hours",
    666 -> "are you sadist?"
  )

  def createDurationpicker(): InlineKeyboardMarkup = {

    // scalastyle:off magic.number
    val durationpickerButtons = DURATIONS.map { duration =>
      InlineKeyboardButton.callbackData(
        duration._2,
        s"duration-${duration._1}"
      )
    }.toSeq.grouped(5).toSeq
    // scalastyle:on magic.number

    InlineKeyboardMarkup(durationpickerButtons)
  }

  // scalastyle:off method.length
  def onCallbackWithTagDuration(bot: SierraBot)(implicit cbq: CallbackQuery): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
      Extractors.Int(durationInMinutes) = data
    } /* do */ {

      for {
        durationInMinutesTuple <- DURATIONS.find(_._1 == durationInMinutes)
      } /* do */ {
        bot.request(
          SendMessage(
            ChatId(msg.source),
            durationInMinutesTuple._2
          )
        )
      }

      chatSession.inputEventDurationInMinutes = Some(durationInMinutes)
      chatSession.save()

      val calendar = Calendar.getInstance
      calendar.set(
        chatSession.inputEventYear.get,
        chatSession.inputEventMonth.get,
        chatSession.inputEventDay.get,
        chatSession.inputEventHour.get,
        chatSession.inputEventMinutes.get
      )
      val beginDate: Date = calendar.getTime

      val ONE_MINUTE_IN_MILLIS = 60000
      val duration = durationInMinutes * ONE_MINUTE_IN_MILLIS
      val endDate = new Date(beginDate.getTime + duration)

      val intersectedEvents = ChatSession.hasIntersections(
        msg.chat.id, beginDate, endDate)
      logger.debug(intersectedEvents.toString)

      val answer = if (intersectedEvents.isEmpty) {
        val event = Event.create(msg.chat.id, beginDate, chatSession.inputEventName.get, endDate)

        for {
          inputCalendarMessageId <- chatSession.inputCalendarMessageId
        } /* do */ {
          bot.request(
            DeleteMessage(
              ChatId(msg.source), // msg.chat.id
              inputCalendarMessageId
            )
          )
        }

        for {
          inputTimepickerMessageId <- chatSession.inputTimepickerMessageId
        } /* do */ {
          bot.request(
            DeleteMessage(
              ChatId(msg.source), // msg.chat.id
              inputTimepickerMessageId
            )
          )
        }

        for {
          inputDurationpickerMessageId <- chatSession.inputDurationpickerMessageId
        } /* do */ {
          bot.request(
            DeleteMessage(
              ChatId(msg.source), // msg.chat.id
              inputDurationpickerMessageId
            )
          )
        }

        chatSession.chatState = ChatState.Started
        chatSession.resetInputs()
        chatSession.save()

        "The event " + event + " is recorded. I will remind you ;)"
      } else {
        val stringBuilder = new StringBuilder(
          "I'm sorry but this event intersects with another ones:\n ")
        intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
        stringBuilder.toString()
      }

      bot.request(
        SendMessage(
          ChatId(msg.source),
          answer
        )
      )

    }
  }
  // scalastyle:on method.length

}

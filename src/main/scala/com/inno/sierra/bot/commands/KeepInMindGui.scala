package com.inno.sierra.bot.commands

import java.util.{Calendar, Date, GregorianCalendar}

import com.inno.sierra.bot.{MessagesText, MonthCalendar, SierraBot}
import com.inno.sierra.model.{ChatSession, ChatState, Event}
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegrambot4s.api.Extractors
import info.mukel.telegrambot4s.methods.{DeleteMessage, EditMessageReplyMarkup, SendMessage}
import info.mukel.telegrambot4s.models._

import scala.concurrent.ExecutionContext

/**
  * Appointing an event in step-by-step mode using
  * telegram inline buttons as user interface.
  */
object KeepInMindGui extends LazyLogging {

  /**
    * Configuration for duration picker widget.
    */
  val DURATIONS = List(
    5 -> "5 min",
    10 -> "10 min",
    15 -> "15 min",
    30 -> "30 min",
    45 -> "45 min",
    60 -> "1 hour",
    90 -> "1.5 hours",
    120 -> "2.0 hours",
    180 -> "3.0 hours"
  )

  /**
    * Handler for /keepinmind command when there are no
    * additional parameters are passed with the comand.
    * @param bot Bot for sending a reply
    * @param msg Message received
    */
  def execute(bot: SierraBot)(implicit msg: Message): Unit = {
    Start.execute(msg)

    // Change state to the first step - entering the name for an event
    val chatSession = ChatSession.getByChatId(msg.chat.id).get
    chatSession.chatState = ChatState.CreatingEventInputtingName
    chatSession.resetInputs()
    chatSession.save()

    // Show a prompting message
    bot.reply(MessagesText.KEEPINMINDGUI_EVENT_NAME)
  }

  /**
    * Handler for regular text message.
    * Depending on current state process message by
    * certain message handler.
    * @param bot Bot for sending a reply
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  def onMessage(bot: SierraBot)(implicit msg: Message, ec: ExecutionContext): Unit = {
    for {
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      _ <- msg.text
      command <- Extractors.textTokens(msg).map(_.head)
      if command != "/keepinmind"
    } /* do */ {
      chatSession.chatState match {
        case ChatState.CreatingEventInputtingName =>
          onMessageEventName(bot, chatSession)
        case ChatState.CreatingEventInputtingDate =>
          onMessageEventDate(bot, chatSession)
        case ChatState.CreatingEventInputtingTime =>
          onMessageEventTime(bot, chatSession)
        case ChatState.CreatingEventInputtingDuration =>
          onMessageEventDuration(bot, chatSession)
        case _ =>
          logger.debug("not applicable to keepinmindgui")
          Unit // do nothing
      }
    }
  }

  /**
    * Handler for message containing event name.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  def onMessageEventName(bot: SierraBot, chatSession: ChatSession)
                        (implicit msg: Message, ec: ExecutionContext): Unit = {

    // Event name will be stored in ChatSession temporarly until step-by-step process is completed.
    chatSession.inputEventName = msg.text

    // Use current date as default value for the event
    val now = new GregorianCalendar
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH) + 1
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    chatSession.inputEventYear = Some(year)
    chatSession.inputEventMonth = Some(month)
    chatSession.inputEventDay = Some(dayOfMonth)

    // Change state to the second step - entering the date for the event
    chatSession.chatState = ChatState.CreatingEventInputtingDate
    chatSession.save()

    // Create calendar widget
    val calendar = createCalendar(year, month)

    // Send a reply with timepicker widget and wait until it is sent
    bot.reply(
      MessagesText.KEEPINMINDGUI_EVENT_DATE,
      replyMarkup = Some(calendar)
    ).map { msg =>
      // Then save calendar widget message ID so we can remove the widget later
      chatSession.inputCalendarMessageId = Some(msg.messageId)
      chatSession.save()
      msg
    }

  }

  /**
    * Prefix (tag) for inline button callback used for handling
    * pressing specific day in calendar widget.
    */
  val CALENDAR_DAY_TAG = "calendar-day-"
  def calendarDayTag(s: String): String = CALENDAR_DAY_TAG + s

  /**
    * Create calendar widget for specific year and month.
    * @param year Year to show in calendar
    * @param month Month to show in calendar
    */
  // scalastyle:off method.length
  private def createCalendar(year: Int, month: Int): InlineKeyboardMarkup = {
    val daysInWeek = Calendar.SATURDAY
    val daysInCalendarMax = 42
    val calendar: MonthCalendar = MonthCalendar(month, year)

    // Top header shows current month and year
    val header = Seq(
      InlineKeyboardButton.callbackData(
        s"${calendar.monthName} $year",
        "ignore"
      )
    )

    // Second header shows days of week
    val daysOfWeek = List("M", "T", "W", "R", "F", "S", "U").map { dayOfWeek =>
      InlineKeyboardButton.callbackData(
        dayOfWeek,
        "ignore"
      )
    }

    // Prepare data about days in the selected month
    val daySlotsInMonth: Seq[String] = Seq().padTo(calendar.dayOfWeek, "  ") ++
      calendar.daysInMonth.map(_.toString)
    val daySlotsInMonthPadded: Seq[String] = daySlotsInMonth match {
      case _ if daySlotsInMonth.lengthCompare(daysInCalendarMax - 7) > 0 =>
        daySlotsInMonth.padTo(daysInCalendarMax, " ")
      case _ => daySlotsInMonth.padTo(daysInCalendarMax - 7, " ")
    }

    // Body shows day of the selected month
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

    // Footer shows controls for navigating to previous or next month
    val footer = Seq(
      InlineKeyboardButton.callbackData("<", "month-previous"),
      InlineKeyboardButton.callbackData(" ", "ignore"),
      InlineKeyboardButton.callbackData(">", "month-next")
    )

    // Consolidate all parts into single widget
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

  /**
    * Dummy handler for disabled buttons.
    * @param bot Bot for sending a reply
    * @param cbq Inline button callback query
    */
  def onCallbackWithTagIgnore(bot: SierraBot)
                             (implicit cbq: CallbackQuery): Unit = {
    bot.ackCallback()
  }

  /**
    * Handler for two buttons: previous and next month.
    * @param bot Bot for sending a reply
    * @param cbq Inline button callback query
    */
  def onCallbackWithTagMonth(bot: SierraBot)
                            (implicit cbq: CallbackQuery): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
    } /* do */ {

      // Get previously selected year and month
      val year = chatSession.inputEventYear.get
      val month = chatSession.inputEventMonth.get

      // Move to next or previous month
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

      // Save newly selected year and month
      chatSession.inputEventYear = Some(yearNew)
      chatSession.inputEventMonth = Some(monthNew)
      chatSession.save()

      // Create calendar widget for newly selected year and month
      val calendarMarkup = createCalendar(yearNew, monthNew)

      // Send a reply and wait until it is sent
      bot.request(
        EditMessageReplyMarkup(
          Some(ChatId(msg.source)), // msg.chat.id
          Some(msg.messageId),
          replyMarkup = Some(calendarMarkup)))
    }
  }

  /**
    * Handler for pressing a button in calendar widget.
    * @param bot Bot for sending a reply
    * @param cbq Inline button callback query
    * @param ec Thread pool in which bot operates
    */
  // scalastyle:off method.length
  def onCallbackWithTagCalendarDay
    (bot: SierraBot)
    (implicit cbq: CallbackQuery, ec: ExecutionContext): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
      Extractors.Int(dayOfMonth) = data
    } /* do */ {

      // Save selected day of month
      chatSession.inputEventDay = Some(dayOfMonth)
      chatSession.save()

      // Proceed to the next step - entering the time for the event
      implicit val message: Message = msg
      proceedToTimepicker(bot, chatSession)

    }
  }
  // scalastyle:on method.length

  /**
    * Handler for message containing event date.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  def onMessageEventDate(bot: SierraBot, chatSession: ChatSession)
                        (implicit msg: Message, ec: ExecutionContext): Unit = {
    // TODO: here could be preprocessing logic from /keepinmind
    val text = msg.text.get.split('.')
    chatSession.inputEventDay = Some(text(0).toInt)
    chatSession.inputEventMonth = Some(text(1).toInt)
    chatSession.inputEventYear = Some(text(2).toInt)
    chatSession.save()

    // Proceed to the next step - entering the time for the event
    proceedToTimepicker(bot, chatSession)

  }

  /**
    * Proceed to the step of selecting time.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  private def proceedToTimepicker(bot: SierraBot, chatSession: ChatSession)
                         (implicit msg: Message, ec: ExecutionContext): Unit = {

    // Prevent showing timepicker widget more than once
    if (chatSession.chatState != ChatState.CreatingEventInputtingDate) {
      return
    }

    // Change state to the second step - entering the time for the event
    chatSession.chatState = ChatState.CreatingEventInputtingTime
    chatSession.save()

    // Show that entered date was accepted and wait until this message is sent
    bot.request(
      SendMessage(
        ChatId(msg.source),
        chatSession.getEventDate
      )
    ).flatMap { msg =>

      // Create timepicker widget
      val timepicker = createTimepicker()

      // Send a reply with timepicker widget and wait until it is sent
      bot.request(
        SendMessage(
          ChatId(msg.source),
          "Please choose the time for the event",
          replyMarkup = Some(timepicker)
        )
      )

    }.map { msg =>

      // Then save timepicker widget message ID so we can remove the widget later
      chatSession.inputTimepickerMessageId = Some(msg.messageId)
      chatSession.save()
      msg
    }

  }

  /**
    * Create timepicker widget.
    */
  private def createTimepicker(): InlineKeyboardMarkup = {

    // Grid for each half an hour in a day.
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

  /**
    * Handler for pressing a button with selected time.
    * @param bot Bot for sending a reply
    * @param ec Thread pool in which bot operates
    */
  def onCallbackWithTagTimepicker(bot: SierraBot)
                                 (implicit cbq: CallbackQuery, ec: ExecutionContext): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
    } /* do */ {

      // Data has format of "$hh-$mm"
      val time = data.split('-').map(_.toInt)
      val hour = time(0)
      val minutes = time(1)

      // Save selected time for an event
      chatSession.inputEventHour = Some(hour)
      chatSession.inputEventMinutes = Some(minutes)
      chatSession.save()

      // Proceed to the next step - entering the duration for the event
      implicit val message: Message = msg
      proceedToDurationpicker(bot, chatSession)

    }
  }

  /**
    * Handler for message containing event time.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  def onMessageEventTime(bot: SierraBot, chatSession: ChatSession)
                        (implicit msg: Message, ec: ExecutionContext): Unit = {
    // TODO: here could be preprocessing logic from /keepinmind
    val text = msg.text.get.split(':')
    chatSession.inputEventHour = Some(text(0).toInt)
    chatSession.inputEventMinutes = Some(text(1).toInt)
    chatSession.save()

    // Proceed to the next step - entering the duration for the event
    proceedToDurationpicker(bot, chatSession)
  }

  /**
    * Proceed to the step of selecting duration.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  private def proceedToDurationpicker(bot: SierraBot, chatSession: ChatSession)
                             (implicit msg: Message, ec: ExecutionContext): Unit = {
    if (chatSession.chatState != ChatState.CreatingEventInputtingTime) {
      return
    }

    // Change state to the second step - entering the duration for the event
    chatSession.chatState = ChatState.CreatingEventInputtingDuration
    chatSession.save()

    // Show that entered time was accepted and wait until this message is sent
    bot.request(
      SendMessage(
        ChatId(msg.source),
        chatSession.getEventTime
      )
    ).flatMap { msg =>

      // Create duration picker widget
      val durationpicker = createDurationpicker()

      // Send a reply with durationpicker widget and wait until it is sent
      bot.request(
        SendMessage(
          ChatId(msg.source),
          MessagesText.KEEPINMINDGUI_EVENT_TIME,
          replyMarkup = Some(durationpicker)
        )
      )

    }.map { msg =>
      // Then save duration widget message ID so we can remove the widget later
      chatSession.inputDurationpickerMessageId = Some(msg.messageId)
      chatSession.save()
      msg
    }

  }

  /**
    * Create duration picker widget.
    */
  private def createDurationpicker(): InlineKeyboardMarkup = {

    // Take durations from constant defined above
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

  /**
    * Handler for pressing a button with selected duration.
    * @param bot Bot for sending a reply
    * @param ec Thread pool in which bot operates
    */
  def onCallbackWithTagDuration(bot: SierraBot)
                               (implicit cbq: CallbackQuery, ec: ExecutionContext): Unit = {
    // Notification only shown to the user who pressed the button.
    bot.ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
      Extractors.Int(durationInMinutes) = data
    } /* do */ {

      // Map selected duration from string back to integer representing duration in minutes
      for {
        durationInMinutesTuple <- DURATIONS.find(_._1 == durationInMinutes)
      } /* do */ {
        // Show that entered duration was accepted
        bot.request(
          SendMessage(
            ChatId(msg.source),
            durationInMinutesTuple._2
          )
        )
      }

      // Save selected duration
      chatSession.inputEventDurationInMinutes = Some(durationInMinutes)
      chatSession.save()

      // Proceed to final step of this step-by-step process
      implicit val message: Message = msg
      createEvent(bot, chatSession)

    }
  }

  /**
    * Handler for message containing event duration.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  def onMessageEventDuration(bot: SierraBot, chatSession: ChatSession)
                            (implicit msg: Message, ec: ExecutionContext): Unit = {
    chatSession.inputEventDurationInMinutes = Some(msg.text.get.toInt)
    chatSession.save()

    // Proceed to final step of this step-by-step process
    createEvent(bot, chatSession)
  }

  /**
    * Create timepicker widget.
    * @param bot Bot for sending a reply
    * @param chatSession Chat session with the current user
    * @param msg Message received
    * @param ec Thread pool in which bot operates
    */
  // scalastyle:off method.length
  def createEvent(bot: SierraBot, chatSession: ChatSession)
                 (implicit msg: Message, ec: ExecutionContext): Unit = {

    // Begin datetime of the event
    val calendar = Calendar.getInstance
    calendar.set(
      chatSession.inputEventYear.get,
      chatSession.inputEventMonth.get - 1,
      chatSession.inputEventDay.get,
      chatSession.inputEventHour.get,
      chatSession.inputEventMinutes.get
    )
    val beginDate: Date = calendar.getTime

    // End datetime of the event
    val ONE_MINUTE_IN_MILLIS = 60000
    val duration = chatSession.inputEventDurationInMinutes.get * ONE_MINUTE_IN_MILLIS
    val endDate = new Date(beginDate.getTime + duration)

    // Check intersection with another events
    val intersectedEvents = ChatSession.hasIntersections(
      msg.chat.id, beginDate, endDate)
    logger.debug(intersectedEvents.toString)

    // Depending on has intersection or not
    val answer = if (intersectedEvents.isEmpty) {

      // If no intersection store the event in database
      val event = Event.create(msg.chat.id, beginDate, chatSession.inputEventName.get, endDate)

      // Delete calendar widget to unclutter chat
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

      // Delete timepicker widget to unclutter chat
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

      // Delete durationpicker widget to unclutter chat
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

      // Reset parameters related to KeepInMindGui
      chatSession.chatState = ChatState.Started
      chatSession.resetInputs()
      chatSession.save()

      // Show that event was successfully created
      MessagesText.KEEPINMIND_DONE.format(event)

    } else {

      // Show error message that event has intersection with another events
      val stringBuilder = new StringBuilder(
        MessagesText.KEEPINMIND_INTERSECTIONS)
      intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
      stringBuilder.toString()

    }

    // Send the answer
    bot.request(
      SendMessage(
        ChatId(msg.source),
        answer
      )
    )

  }
  // scalastyle:on method.length

}

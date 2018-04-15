package com.inno.sierra.bot

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar, TimeZone}

import com.inno.sierra.model.{ChatSession, ChatState, Event}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.methods.{DeleteMessage, EditMessageReplyMarkup, GetMe, SendMessage}
import info.mukel.telegrambot4s.models._
import java.util.concurrent.Executors

import akka.actor.{ActorSystem, Cancellable, Props}

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.MutableList
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

abstract class SierraBot extends TelegramBot with Commands with Callbacks {
//  lazy val botName = ConfigFactory.load().getString("bot.name")
  val botName: String =
    Await.result(request(GetMe).map(_.firstName), 10.seconds)
  lazy val token: String = ConfigFactory.load().getString("bot.token")

  val NUM_OF_THREADS = 10
  var notifier: Cancellable = _

  onCommand("/start") {
    println("start command")
    implicit msg => {
      println("msg is: " + msg)
      reply(start(msg))
    }
  }

  onCommand("/subscribe") {
    implicit msg => {reply(subscribe(msg))
    }
  }

  onCommand("/keepinmind") {
    implicit msg => reply(keepInMind(msg))
  }

   onCommand("/info") {
    implicit msg => reply(info())
  }
  
  /**
    * Handling the communication within the group is implemented here.
    * @param message message instance
    */
  override def receiveMessage(message: Message): Unit = {
    logger.debug("recieved message '" + message.text + "' from " + message.chat)
    for (text <- message.text) {
      // If it is a group chat
      if (message.chat.`type` == ChatType.Group) {
        if (text.startsWith(botName)) {
          if (text.contains("/info")) {
            request(SendMessage(message.source, info))
          } else if (text.contains("/start")) {
            request(SendMessage(message.source, start(message)))
          } else if (text.contains("/keepinmind")) {
            request(SendMessage(message.source, keepInMind(message)))
          } else {
            request(SendMessage(message.source, "I'm sorry, it seems I can't understand you -_- " +
              "Let me explain what I can do"))
            request(SendMessage(message.source, info))
          }
        }
      } else {
        super.receiveMessage(message)
      }

    }
  }


  val actorSystem = ActorSystem("telegramNotification")

  override def run(): Unit = {
    super.run()
    val ns = new NotifierService()(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
    val notificationSendingActor = actorSystem.actorOf(Props(classOf[NotificationActor], this), "notificationSendingActor")
    val timeframe = (10 seconds)  //each x seconds bot will lookup for new events and send them
    notifier = actorSystem.scheduler.schedule(0 seconds, timeframe){
      ns.sendMessages(notificationSendingActor, timeframe) onComplete {
        case Success(_) =>
        case Failure(e) => logger.error("Notifier error: ", e)
      }
    }
  }

  override def shutdown(): Future[Unit] = {
    notifier.cancel()
    super.shutdown()
  }


  def start(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (!ChatSession.exists(chat.id)) {
      ChatSession.create(chat.id, user.username.get,
        !chat.`type`.equals(ChatType.Private), ChatState.Started)
      MessagesText.START_FIRST_TIME.format(user.firstName)

    } else {
      MessagesText.START_AGAIN.format(user.firstName)
    }
  }

  def subscribe(msg: Message): String = {
    val user = msg.from.get
    val chat = msg.chat

    if (!chat.`type`.equals(ChatType.Private)) {
      ChatSession.addUserToGroup(chat.id, user.id, user.username.get)
      MessagesText.SUBSCRIBE_DONE
    } else {
      MessagesText.SUBSCRIBE_ALREADY
    }
  }

  def info(): String = MessagesText.INFO

  def keepInMind(implicit msg: Message): String = {
    start(msg)

    withArgs {
      // TODo: maybe we need to check that the event planned is in the future
      val now = Calendar.getInstance().getTime()
      args => {
        val dateRegex = """([0-9]{2}.[0-9]{2}.[0-9]{4})"""
        val timeRegex = """([0-9]{2}:[0-9]{2})"""
        var nameRex = """([A-Za-z0-9]{1,30})"""
        val duration = """([0-9]{0,4})"""
        val DateOnly = dateRegex.r
        val TimeOnly = timeRegex.r
        val NameMeeting = nameRex.r
        val TimeDuration = duration.r

        val parameter = MutableList[String]()
        val regex = MutableList[String]()

        var i = 0;
        var listRegex = List(DateOnly,TimeOnly,TimeDuration,NameMeeting)
        for (arg <- args) {


          if (!arg.isEmpty && !arg.startsWith("/")) {

            def verifyParameter(x: Any,y:scala.util.matching.Regex) = x match {
            //  case s: String =>  println(" - param match string : "+arg)
            //  case TimeDuration(d) =>  println(" - duration only : "+d+arg)
            //  case DateOnly(d) =>  println(" - date only : "+d+arg)
            //  case TimeOnly(d) =>  println(" - time only : "+d+arg)
              case
                test:String if y.findFirstMatchIn(test).nonEmpty => println("Checked correct parameter: "+arg)
              case _  =>   println("Wrong parameter: "+arg)
            }
            verifyParameter(arg,listRegex(i));
          //  val unMactchParamerter = verifyParameter(arg,listRegex(i));
          //  if(unMactchParamerter == true){
           //   return "Wrong format of parameter"
         //   }


            i+= 1
          logger.trace(arg)
          if (!arg.isEmpty && !arg.startsWith("/")) {
            logger.trace(" - param is added")
            parametro += arg

          }
        }

        if (parameter.length != 4) {
          return "Create a meeting requires 4 parameters date, hour, name and duration(min)"
        } else {
          var date_meeting = parameter(0).concat(" ").concat(parameter(1))
          val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
          val simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm")
          val beginDate: Date = simpleDateFormat.parse(date_meeting)

          val ONE_MINUTE_IN_MILLIS = 60000
          val duration = Integer.parseInt(parameter(3)) * ONE_MINUTE_IN_MILLIS
          val endDate = new Date(beginDate.getTime() + duration)

          val intersectedEvents = ChatSession.hasIntersections(
            msg.chat.id, beginDate, endDate)
          logger.debug(intersectedEvents.toString)

          if (intersectedEvents.isEmpty) {
            val event = Event.create(msg.chat.id, beginDate, parameter(2), endDate)
            return "The event " + event + " is recorded. I will remind you ;)" // TODO: phrases in different file!
          } else {
            val stringBuilder = new StringBuilder(
              "I'm sorry but this event intersects with another ones:\n ")
            intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
            return stringBuilder.toString()
          }
          for (name <- args) {
            //   reply(name)
          }
        }
      }
    }
    return MessagesText.ERROR_UNEXPTECTED
  }

  def sendMessage(csid: Long, text: String): Unit = synchronized {
    request(SendMessage(csid, text)) onComplete {
      case Success(_) =>
      case Failure(e) => logger.error("Message sending error: ", e)
    }
  }

  val CALENDAR_DAY_TAG = "calendar-day-"
  def calendarDayTag(s: String): String = prefixTag(CALENDAR_DAY_TAG)(s)

  onCallbackWithTag("ignore") { implicit cbq =>
    ackCallback()
  }

  onCallbackWithTag(CALENDAR_DAY_TAG) { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
      Extractors.Int(dayOfMonth) = data
    } /* do */ {

      val (year: Int, month: Int) = chatSession.inputEventDatetime match {
        case Some(date) =>
          val calendar = Calendar.getInstance
          calendar.setTime(date)
          (calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
        case None =>
          throw new NoSuchElementException()
      }

      val inputEventDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
      inputEventDate.set(year, month, dayOfMonth)
      chatSession.inputEventDatetime = Some(new Timestamp(inputEventDate.getTime.getTime))
      chatSession.save()

      val dateStr = "%02d.%02d.%04d".format(dayOfMonth, month, year)

      request(
        SendMessage(
          ChatId(msg.source),
          dateStr
        )
      )

      val timepicker = createTimepicker()

      request(
        SendMessage(
          ChatId(msg.source),
          "Please choose the time for the event",
          replyMarkup = Some(timepicker)
        )
      ).map { msg =>
        chatSession.inputTimepickerMessageId = Some(msg.messageId)
        chatSession.save()
        msg
      }

    }
  }

  onCallbackWithTag("month-") { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
    } /* do */ {

      val (year: Int, month: Int) = chatSession.inputEventDatetime match {
        case Some(date: Date) =>
          val calendar = Calendar.getInstance
          calendar.setTime(date)
          (calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
        case None =>
          throw new NoSuchElementException()
      }

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

      val calendar = Calendar.getInstance
      val eventDatetime = chatSession.inputEventDatetime.getOrElse(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime)
      calendar.setTime(eventDatetime)
      calendar.set(Calendar.YEAR, yearNew)
      calendar.set(Calendar.MONTH, monthNew)
      chatSession.inputEventDatetime = Some(new Timestamp(calendar.getTime.getTime))
      chatSession.save()

      val calendarMarkup = createCalendar(yearNew, monthNew)

      request(
        EditMessageReplyMarkup(
          Some(ChatId(msg.source)), // msg.chat.id
          Some(msg.messageId),
          replyMarkup = Some(calendarMarkup)))
    }
  }

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

  onCallbackWithTag("timepicker-") { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
    } /* do */ {

      val time = data.split('-').map(_.toInt)
      val hour = time(0)
      val minutes = time(1)

      val calendar = Calendar.getInstance
      val date = chatSession.inputEventDatetime.getOrElse(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime)
      calendar.setTime(date)
      calendar.set(Calendar.HOUR, hour)
      calendar.set(Calendar.MINUTE, minutes)
      calendar.set(Calendar.SECOND, 0)
      chatSession.inputEventDatetime = Some(new Timestamp(calendar.getTime.getTime))
      chatSession.save()

      val timeStr = "%02d:%02d".format(hour, minutes)

      request(
        SendMessage(
          ChatId(msg.source),
          timeStr
        )
      )

      val durationpicker = createDurationpicker()

      request(
        SendMessage(
          ChatId(msg.source),
          "Please choose the time for the event",
          replyMarkup = Some(durationpicker)
        )
      ).map { msg =>
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

  onCallbackWithTag("duration-") { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()

    for {
      msg <- cbq.message
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      data <- cbq.data
      Extractors.Int(durationInMinutes) = data
    } /* do */ {

      for {
        durationInMinutesTuple <- DURATIONS.find(_._1 == durationInMinutes)
      } /* do */ {
        request(
          SendMessage(
            ChatId(msg.source),
            durationInMinutesTuple._2
          )
        )
      }

      chatSession.inputEventDurationInMinutes = Some(durationInMinutes)
      chatSession.save()

      val calendar = Calendar.getInstance
      calendar.setTimeInMillis(chatSession.inputEventDatetime.get.getTime)
      val beginDate: Date = calendar.getTime

      val ONE_MINUTE_IN_MILLIS = 60000
      val duration = durationInMinutes * ONE_MINUTE_IN_MILLIS
      val endDate = new Date(beginDate.getTime() + duration)

      val intersectedEvents = ChatSession.hasIntersections(
        msg.chat.id, beginDate, endDate)
      logger.debug(intersectedEvents.toString)

      val answer = if (intersectedEvents.isEmpty) {
        val event = Event.create(msg.chat.id, beginDate, chatSession.inputEventName.get, endDate)

        for {
          inputCalendarMessageId <- chatSession.inputCalendarMessageId
        } /* do */ {
          request(
            DeleteMessage(
              ChatId(msg.source), // msg.chat.id
              inputCalendarMessageId
            )
          )
        }

        for {
          inputTimepickerMessageId <- chatSession.inputTimepickerMessageId
        } /* do */ {
          request(
            DeleteMessage(
              ChatId(msg.source), // msg.chat.id
              inputTimepickerMessageId
            )
          )
        }

        for {
          inputDurationpickerMessageId <- chatSession.inputDurationpickerMessageId
        } /* do */ {
          request(
            DeleteMessage(
              ChatId(msg.source), // msg.chat.id
              inputDurationpickerMessageId
            )
          )
        }

        chatSession.chatState = ChatState.Started
        chatSession.inputEventDatetime = None
        chatSession.inputEventName = None
        chatSession.inputEventDurationInMinutes = None
        chatSession.inputCalendarMessageId = None
        chatSession.inputTimepickerMessageId = None
        chatSession.inputDurationpickerMessageId = None
        chatSession.save()

        "The event " + event + " is recorded. I will remind you ;)"
      } else {
        val stringBuilder = new StringBuilder(
          "I'm sorry but this event intersects with another ones:\n ")
        intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
        stringBuilder.toString()
      }

      request(
        SendMessage(
          ChatId(msg.source),
          answer
        )
      )

    }
  }

  onCommand("/keepinmind2") { implicit msg =>
    start(msg)

    val chatSession = ChatSession.getByChatId(msg.chat.id).get
    chatSession.chatState = ChatState.CreatingEventInputtingName
    chatSession.inputEventDatetime = None
    chatSession.inputEventName = None
    chatSession.inputEventDurationInMinutes = None
    chatSession.save()

    reply("Alright, a new event. How are we going to call it? Please choose a name for an event.")
  }

  onMessage { implicit  msg =>

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
      var dayOfMonth = now.get(Calendar.DAY_OF_MONTH)

      val inputEventDate = Calendar.getInstance()
      inputEventDate.set(year, month, dayOfMonth)

      chatSession.inputEventDatetime = Some(new Timestamp(inputEventDate.getTime.getTime))
      chatSession.save()

      val calendar = createCalendar(year, month)

      reply("Good. Now let's choose a date for your event. Use the calendar widget to enter the planned date.", replyMarkup = Some(calendar)).map { msg =>
        chatSession.inputCalendarMessageId = Some(msg.messageId)
        chatSession.save()
        msg
      }
    }

  }

}

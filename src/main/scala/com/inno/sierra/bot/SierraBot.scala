package com.inno.sierra.bot

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar}

import com.inno.sierra.model.{ChatSession, ChatState, Event}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.methods.{GetMe, SendMessage}
import info.mukel.telegrambot4s.models._
import java.util.concurrent.Executors

import akka.actor.{ActorSystem, Cancellable, Props}
import info.mukel.telegrambot4s.methods.EditMessageReplyMarkup

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
        !chat.`type`.equals(ChatType.Private), ChatState.Start)
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

  /**
    * Chat message ID represents a reference to concrete message in some chat,
    * so it is kinda unique identifier for a message.
    *
    * @param chatId
    * @param messageId
    */
  case class ChatMessageId(chatId: Long, messageId: Int)

  val currentShownDates: scala.collection.mutable.Map[ChatMessageId, (Int, Int)] = scala.collection.mutable.Map.empty[ChatMessageId, (Int, Int)]

  val CALENDAR_DAY_TAG = "calendar-day"
  def calendarDayTag(s: String): String = prefixTag(CALENDAR_DAY_TAG)(s)

  onCallbackWithTag("ignore") { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()
  }

  onCallbackWithTag(CALENDAR_DAY_TAG) { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()

    for {
      data <- cbq.data
      Extractors.Int(dayOfMonth) = data
      msg <- cbq.message
    } /* do */ {

      val (year: Int, month: Int) = currentShownDates(ChatMessageId(msg.source, msg.messageId))

      val dateStr = "%02d.%02d.%04d".format(dayOfMonth, month, year)

      request(
        SendMessage(
          ChatId(msg.source),
          dateStr
        )
      )
    }
  }

  onCallbackWithTag("month-") { implicit cbq =>
    // Notification only shown to the user who pressed the button.
    ackCallback()

    for {
      data <- cbq.data
      msg <- cbq.message
    } /* do */ {

      val (year: Int, month: Int) = currentShownDates(ChatMessageId(msg.source, msg.messageId))

      val (yearNew: Int, monthNew: Int) = data match {
        case "next" =>
          month + 1 match {
            case monthPlusOne if monthPlusOne > 12 => (year + 1, 1)
            case _ => (year, month + 1)
          }
        case "previous" =>
          month - 1 match {
            case monthMinusOne if monthMinusOne < 1 => (year - 1, 1)
            case _ => (year, month - 1)
          }
      }

      currentShownDates(ChatMessageId(msg.chat.id, msg.messageId)) = (yearNew, monthNew)

      val calendar = createCalendar(yearNew, monthNew)

      request(
        EditMessageReplyMarkup(
          Some(ChatId(msg.source)), // msg.chat.id
          Some(msg.messageId),
          replyMarkup = Some(calendar)))
    }
  }

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
    val daySlotsInMonth: Seq[String] = (Seq().padTo(calendar.dayOfWeek, "  ") ++
      calendar.daysInMonth.map(_.toString))
      .padTo(daysInCalendarMax, "  ")
    val body = daySlotsInMonth.grouped(daysInWeek).map { weekSlots =>
      weekSlots.map { dayOfMonth =>
        InlineKeyboardButton.callbackData(
          dayOfMonth,
          dayOfMonth match {
            case " " => "ignore"
            case _ => calendarDayTag(dayOfMonth)
          }
        )
      }
    } toSeq
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

  onCommand("/calendar") { implicit msg =>
    val now = new GregorianCalendar
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH)
    val calendar = createCalendar(year, month)

    reply("Please choose the date to conduct an event on", replyMarkup = Some(calendar)).map { msg =>
      currentShownDates(ChatMessageId(msg.chat.id, msg.messageId)) = (year, month)
      msg
    }
  }

}

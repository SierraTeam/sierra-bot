package com.inno.sierra.bot

import java.util.concurrent.Executors

import akka.actor.{ActorSystem, Cancellable, Props}
import com.inno.sierra.bot.commands._
import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.methods.SendMessage

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


abstract class SierraBot extends TelegramBot with Commands with Callbacks {
  lazy val token: String = ConfigFactory.load().getString("bot.token")

  val NUM_OF_THREADS = 10
  var notifier: Cancellable = _

  onCommand("/start") {
    implicit msg => reply(Start.execute(msg))
  }

  onCommand("/subscribe") {
    implicit msg => reply(Subscribe.execute(msg))
  }

  onCommand("/unsubscribe") {
    implicit msg => reply(Unsubscribe.execute(msg))
  }

  onCommand("/keepinmind") {
    implicit msg => {
      val args = Extractors.commandArguments(msg)
      if (args.isEmpty || args.get.length != 4) {
        KeepInMind2.execute(this)
      } else reply(KeepInMind.execute(msg))
    }
  }

  onCommand("/info") {
    implicit msg => reply(Info.execute(msg))
  }

  onCommand("/myevents") {
    implicit msg => reply(MyEvents.execute(msg))
  }

  onCommand("/cancelevent") {
    implicit msg => CancelEvent.execute(this)
  }

  onMessage { implicit msg =>
    KeepInMind2.onMessage(this)
  }

  onCallbackWithTag("ignore") { implicit cbq =>
    KeepInMind2.onCallbackWithTagIgnore(this)
  }

  onCallbackWithTag(KeepInMind2.CALENDAR_DAY_TAG) { implicit cbq =>
    KeepInMind2.onCallbackWithTagCalendarDay(this)
  }

  onCallbackWithTag("month-") { implicit cbq =>
    KeepInMind2.onCallbackWithTagMonth(this)
  }

  onCallbackWithTag("timepicker-") { implicit cbq =>
    KeepInMind2.onCallbackWithTagTimepicker(this)
  }

  onCallbackWithTag("duration-") { implicit cbq =>
    KeepInMind2.onCallbackWithTagDuration(this)
  }

  onCallbackWithTag("event-") { implicit cbq =>
    CancelEvent.onCallbackWithTagEvent(this)
  }

  /**
    * Handling the communication within the group is implemented here.
    *
    * @param message message instance
    */
  /*override def receiveMessage(message: Message): Unit = {
    logger.debug("recieved message '" + message.text + "' from " + message.chat)
    for (text <- message.text) {
      // If it is a group chat
      if (message.chat.`type` == ChatType.Group) {
        if (text.startsWith(botName)) {
          if (text.contains("/info")) {
            request(SendMessage(message.source, Info.execute(message)))
          } else if (text.contains("/start")) {
            request(SendMessage(message.source, Start.execute(message)))
          } else if (text.contains("/keepinmind")) {
            request(SendMessage(message.source, KeepInMind.execute(message)))
          } else {
            request(SendMessage(message.source, "I'm sorry, it seems I can't understand you -_- " +
              "Let me explain what I can do"))
            request(SendMessage(message.source, Info.execute(message)))
          }
        }
      } else {
        super.receiveMessage(message)
      }

    }
  }*/


  val actorSystem = ActorSystem("telegramNotification")

  override def run(): Unit = {
    super.run()
    val ns = new NotifierService()(
      ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
    val notificationSendingActor =
      actorSystem.actorOf(Props(classOf[NotificationActor], this), "notificationSendingActor")
    val timeframe = (10 seconds) //each x seconds bot will lookup for new events and send them
    notifier = actorSystem.scheduler.schedule(0 seconds, timeframe) {
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

  def sendMessage(csid: Long, text: String): Unit = {
    request(SendMessage(csid, text)) onComplete {
      case Success(_) =>
      case Failure(e) => logger.error("Message sending error: ", e)
    }
  }
}

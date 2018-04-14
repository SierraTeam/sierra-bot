package com.inno.sierra.bot

import java.text.SimpleDateFormat
import java.util.Date

import com.inno.sierra.model.{ChatSession, ChatState, DbSchema, Event}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.{GetMe, SendMessage}
import info.mukel.telegrambot4s.models._
import java.util.Calendar

import akka.actor.Cancellable

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.MutableList
import scala.concurrent.{Await, Future}

abstract class SierraBot extends TelegramBot with Commands {
//  lazy val botName = ConfigFactory.load().getString("bot.name")
  val botName: String =
    Await.result(request(GetMe).map(_.firstName), 10.seconds)
  lazy val token = ConfigFactory.load().getString("bot.token")

  val NUM_OF_THREADS = 10
  var notifier: Cancellable = _

  onCommand("/start") {
    implicit msg => reply(start(msg))
  }

  onCommand("/keepinmind") {
    implicit msg => reply(keepInMind(msg))
  }

   onCommand("/info") {
    implicit msg => reply(info())
  }
  
  /**
    * Handling the communication within the group is implemented here.
    * @param message
    */
  override def receiveMessage(message: Message): Unit = {
    println("recieved message '" + message.text + "' from " + message.chat)
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

  override def run(): Unit = {
    super.run()
    val ns = new NotifierService(NUM_OF_THREADS, this)
    notifier = system.scheduler.schedule(0 seconds, 10 seconds){
      ns.sendMessages()
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
      ChatSession.create(
        chat.id, user.username.get, ChatState.Start)
      "Nice to meet you, " + user.firstName + "! I can help" +
        " you to plan your activities. I'll try to be useful for you :)"

    } else {
      "Welcome back, " + user.firstName + "! I'm glad to see you again :) " +
        "How can I help you?"
    }
  }

  def info(): String = {
    "Telegram bot created with Scala. This bot is a simple Assistant that provides " +
      "the following functionality:\n" +
      "/start: Starts this bot.\n" +
      "/keepinmind: Creates an Event to Keep in Mind.\n" +
      "/info:  Displays description (this text).\n" +
      "/exit:  TODO.\n"
  }

  def keepInMind(implicit msg: Message): String = {
    withArgs {
      // TODo: maybe we need to check that the event planned is in the future
      val now = Calendar.getInstance().getTime()
      args => {
        val parameter = MutableList[String]()
        for (arg <- args) {
          print(arg) // TODO: change to log
          if (!arg.isEmpty && !arg.startsWith("/")) {
            println(" - param is added")
            parameter += arg
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
          println(intersectedEvents) // TODO: to log

          if (intersectedEvents.isEmpty) {
            val event = Event.create(msg.chat.id, beginDate, parameter(2), endDate)
            return "The event " + event + " is remembered. I will remind you ;)" // TODO: phrases in different file!
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
    return "I'm so sorry! It seems something went wrong, try again, please."
  }

  def sendMessage(csid: Long, text: String) = synchronized {
    request(SendMessage(csid, text))
  }
}

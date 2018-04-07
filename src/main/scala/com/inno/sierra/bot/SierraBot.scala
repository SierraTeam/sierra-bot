package com.inno.sierra.bot

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

import com.inno.sierra.model.{ChatSession, ChatState, DbSchema, Event}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models._
import java.util.Calendar

import akka.actor.Cancellable

import scala.concurrent.duration._
import java.time.format.DateTimeFormatter

import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.api.BotBase

import scala.concurrent.Future

abstract class SierraBot extends TelegramBot with Commands {
  val botName = "@sierraTest1bot"

  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
//  lazy val token = scala.util.Properties.
//    .envOrNone("BOT_TOKEN")
//    .getOrElse(Source.fromFile("bot.token").getLines().mkString)
  lazy val token = ConfigFactory.load().getString("bot.token")

  val NUM_OF_THREADS = 10
  var notifier: Cancellable = _

  onCommand("/start") {
    implicit msg => reply(start(msg))
  }

  onCommand("/keepinmind") {
    val today = Calendar.getInstance().getTime()
    var taskName : String = "midterm"

    var  i:Int  =0;

    implicit msg =>  withArgs {

      args =>  //doubleMatch(1,20170101,args.mkString(" "),37);
      //            print(args);
        //reply(args.mkString(" "))
        val parametro =  Array("","","","")
        var indice = 0;
        var size =0;
        for (arg <- args){ println(arg)
          parametro(indice) = arg;
          if(arg !=""){
            size+= 1;
          }
          indice+= 1;
        }

        if(size != 4){
          reply("Create a meeting requires 4 parameters date, hour, name and duration(min)")
        }else{

          var date_meeting = parametro(0).concat(" ").concat(parametro(1))
          val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
          var  simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
          var  date:Date = simpleDateFormat.parse(date_meeting);

//          Event.create(2,date,parametro(2),parametro(3));
          reply("Create task "+parametro(2)+" successfull")
          for (name <- args){
         //   reply(name)
          }

        }
    }

      //
  }

   onCommand("/info") {
    implicit msg => reply(info())
  }

  /**
    * Handling the communication within the group is implemented here.
    * @param message
    */
  override def receiveMessage(message: Message): Unit = {
    for (text <- message.text) {
      // If there is a group chat
      if (message.chat.username.isEmpty) {
        if (text.startsWith(botName)) {
          if (text.contains("/info")){
            request(SendMessage(message.source, info))
          } else if (text.contains("/start")) {
            request(SendMessage(message.source, start(message)))
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
    val ns = new NotifierService(NUM_OF_THREADS)
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
}

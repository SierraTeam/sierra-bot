package com.inno.sierra.bot

import java.util.Date

import com.inno.sierra.model.{ChatSession, ChatState, DbSchema, Event}
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models._
import java.util.Calendar

import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.api.BotBase

abstract class SierraBot extends TelegramBot with Commands {

  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
//  lazy val token = scala.util.Properties.
//    .envOrNone("BOT_TOKEN")
//    .getOrElse(Source.fromFile("bot.token").getLines().mkString)
  lazy val token = ConfigFactory.load().getString("bot.token")


  val event = Event

  /**
    * COMMAND /start
    * Present the bot.
    */
  onCommand("/start") {implicit msg =>
    {
      val user = msg.from.get
      val chat = msg.chat
      if (!ChatSession.exists(chat.id)) {
        ChatSession.create(
          chat.id, user.username.get, ChatState.Start)
        reply("Nice to meet you, " + user.firstName + "! I can help" +
          " you to plan your activities. I'll try to be useful for you :)")
      } else {
        reply("Welcome back, " + user.firstName + "! I'm glad to see you again :)")
      }
    }
  }
  //def doubleMatch(foo: Any, bar: Any,foo2: Any, bar2: Any) = (foo, bar,foo2,bar2) match {
   // case ('a', 'b','c',_) => "a and b"
  //  case (a:Long,b:Date,c:String,d:Long) => "oi"

  //}

  onCommand("/keepinmind") {

    val today = Calendar.getInstance().getTime()
    var taskName : String = "midterm"
    Event.create(2,today,taskName,30);
    //id: Long, time: Date,e
    //name: String, duration: Long

    implicit msg =>  withArgs {

      args =>  //doubleMatch(1,20170101,args.mkString(" "),37);
                  print(args);
        //reply(args.mkString(" "))


   //     for (name <- args){
    //      reply(name)
    //    }
       // nameValuePairs: Array[java.lang.String] = Array(oauth_token=FOO, oauth_token_secret=BAR, oauth_expires_in=3600)

    }

      //reply("Create task "+taskName+" successfull")
  }
  
   onCommand("/info") {
    implicit msg => reply("Telegram bot created with Scala. This bot is a simple Assistant that provides " +
      "the following functionality:\n" +
      "/start: Starts this bot.\n" +
      "/keepinmind: Creates an Event to Keep in Mind.\n" +
      "/info:  Displays description (this text).\n" +
      "/exit:  TODO.\n")
  }

  // TODO: Remove later, it's for test of notifications
/*  onCommand("/test") {
    /*request(
      SendMessage()
    )*/
  }*/

  // TODO: Remove, just an example
  /**
    * COMMAND /coin
    * COMMAND /flip
    * Flip a coin.
    */
  val rng = new scala.util.Random(System.currentTimeMillis())
  onCommand("coin", "flip") {
    implicit msg => reply(if (rng.nextBoolean()) "Head!" else "Tail!")
  }

  /**
    * MESSAGE <any>
    * Echo message back.
    * @param message
    */
  override def receiveMessage(message: Message): Unit = {
    for (text <- message.text) {
      if (text(0) != '/') {
        request(SendMessage(message.source, message.chat.id + ": " + text.reverse))
      } else {
        super.receiveMessage(message)
      }
    }
  }
}

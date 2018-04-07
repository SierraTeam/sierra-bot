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
  val botName = "@sierraTest1bot"

  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
//  lazy val token = scala.util.Properties.
//    .envOrNone("BOT_TOKEN")
//    .getOrElse(Source.fromFile("bot.token").getLines().mkString)
  lazy val token = ConfigFactory.load().getString("bot.token")


  onCommand("/start") {
    implicit msg => reply(start(msg))
  }

  onCommand("/keepinmind") {
    val today = Calendar.getInstance().getTime()
    var taskName : String = "midterm"
    Event.create(2,today,taskName,today)

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
    implicit msg => reply(info())
  }

  /**
    * Handling the communication within the group is implemented here.
    * @param message
    */
  override def receiveMessage(message: Message): Unit = {
    for (text <- message.text) {
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
    }
  }

  override def run(): Unit = {
    super.run()
    new Thread(new NotifierService(10)).run()
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

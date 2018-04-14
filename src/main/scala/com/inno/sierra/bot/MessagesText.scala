package com.inno.sierra.bot

object MessagesText {

  val INFO = "Telegram bot created with Scala. This bot is a simple Assistant that provides " +
    "the following functionality:\n" +
    "/start: Starts this bot.\n" +
    "/keepinmind: Creates an Event to Keep in Mind.\n" +
    "/info:  Displays description (this text).\n" +
    "/exit:  TODO.\n"


  val START_FIRST_TIME = "Nice to meet you, %s! I can help" +
    " you to plan your activities. I'll try to be useful for you :)"


  val START_AGAIN = "Welcome back, %s! I'm glad to see you again :) " +
    "How can I help you?"


  val ERROR_UNEXPTECTED = "I'm so sorry! It seems something went wrong, try again, please."
}

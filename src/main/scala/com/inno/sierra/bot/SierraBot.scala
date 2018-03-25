package com.inno.sierra.bot

import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models._

import scala.io.Source

object SierraBot extends TelegramBot with Polling with Commands {

  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
  lazy val token = scala.util.Properties
    .envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromFile("bot.token").getLines().mkString)

  /**
    * COMMAND /start
    *
    * Present the bot.
    */
  onCommand("/start") { implicit msg => reply("Description of bot. Description of commands.") }

  /**
    * COMMAND /coin
    * COMMAND /flip
    *
    * Flip a coin.
    */
  val rng = new scala.util.Random(System.currentTimeMillis())
  onCommand("coin", "flip") { implicit msg => reply(if (rng.nextBoolean()) "Head!" else "Tail!") }

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

package com.inno.sierra

import scala.io.StdIn

import com.inno.sierra.bot.{SierraPollingBot, SierraWebhookBot}
import com.inno.sierra.model.DbSchema
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]) {
    DbSchema.init()

    // Mode is polling or webhook
    val sierraBot = ConfigFactory.load().getBoolean("bot.polling") match {
      case false => new SierraWebhookBot()
      case _ => new SierraPollingBot()
    }

    // Run bot
    sierraBot.run()

    // Wait for Enter keypress then shutdown
    StdIn.readLine()
    sierraBot.shutdown()

  }
}

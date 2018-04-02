package com.inno.sierra

import scala.io.StdIn

import com.inno.sierra.bot.SierraBot
import com.inno.sierra.model.DbSchema

object Main {

  def main(args: Array[String]) {
    // TODO: delete before production
    DbSchema.init()

    // Run bot
    SierraBot.run()

    // Wait for Enter keypress then shutdown
    StdIn.readLine()
    SierraBot.shutdown()

  }
}

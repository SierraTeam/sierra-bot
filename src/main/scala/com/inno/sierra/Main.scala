package com.inno.sierra

import com.inno.sierra.bot.SierraBot
import com.inno.sierra.model.DbSchema

object Main {

  def main(args: Array[String]) {
    // TODO: delete before production
    DbSchema.init()

    SierraBot.run()
  }
}

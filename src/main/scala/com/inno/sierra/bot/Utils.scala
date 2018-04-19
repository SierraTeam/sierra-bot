package com.inno.sierra.bot

import java.time.format.DateTimeFormatter

object Utils {
  val datePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  val timePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

}

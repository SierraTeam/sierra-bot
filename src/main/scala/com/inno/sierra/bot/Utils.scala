package com.inno.sierra.bot

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

object Utils {
  val datePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
  val timePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  val simpleDateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm")
  val simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy")
}

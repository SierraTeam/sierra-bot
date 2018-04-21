package com.inno.sierra.bot.commands

import java.text.SimpleDateFormat

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar

object SyncGoogle extends com.inno.sierra.Quickstart{

  def init(): Unit ={
    val service: Calendar = getCalendarService
  }

  def checkFree(): Boolean ={
    init()
    val inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm")

    val date = "19/04/2018 17:00"
    val ONE_HOUR_IN_MILLIS = 60000*60
    val minTime:DateTime = new DateTime(System.currentTimeMillis())
    val endTime:DateTime = new DateTime(System.currentTimeMillis() + ONE_HOUR_IN_MILLIS)

    val amIFree: Boolean = getEventsWindow(minTime, endTime)
    amIFree
  }


}

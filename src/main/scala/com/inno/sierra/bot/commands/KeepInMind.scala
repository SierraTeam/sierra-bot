package com.inno.sierra.bot.commands

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import com.inno.sierra.bot.MessagesText
import com.inno.sierra.model.{ChatSession, Event}
import info.mukel.telegrambot4s.api.Extractors
import info.mukel.telegrambot4s.models.Message

import scala.collection.mutable
import scala.collection.mutable.MutableList
import com.typesafe.scalalogging.LazyLogging


object KeepInMind extends LazyLogging {
  val dateRegex = """([0-9]{2}.[0-9]{2}.[0-9]{4})"""
  val timeRegex = """([0-9]{2}:[0-9]{2})"""
  var nameRex = """([A-Za-z0-9]{1,30})"""
  val duration = """([0-9]{0,4})"""
  val DateOnly = dateRegex.r
  val TimeOnly = timeRegex.r
  val NameMeeting = nameRex.r
  val TimeDuration = duration.r


  def verifyParameter(x: Any, y: scala.util.matching.Regex) = x match {
    //  case s: String =>  println(" - param match string : "+arg)
    //  case TimeDuration(d) =>  println(" - duration only : "+d+arg)
    //  case DateOnly(d) =>  println(" - date only : "+d+arg)
    //  case TimeOnly(d) =>  println(" - time only : "+d+arg)
    case
      test: String if y.findFirstMatchIn(test).nonEmpty => logger.debug("Checked correct parameter: " + x)
    case _ => logger.debug("Wrong parameter: " + x)
  }

  def execute(msg: Message): String = {
    val args = Extractors.commandArguments(msg).getOrElse(
      return MessagesText.KEEPINMIND_NOT_ENOUGH_PARAMS
    )
    val regex = MutableList[String]()

    var i = 0
    var listRegex = List(DateOnly,TimeOnly,TimeDuration,NameMeeting)
    for (arg <- args) {
      logger.trace(arg)
      if (!arg.isEmpty && !arg.startsWith("/")) {
        verifyParameter(arg, listRegex(i))
        //  val unMactchParamerter = verifyParameter(arg,listRegex(i));
        //  if(unMactchParamerter == true){
        //   return "Wrong format of parameter"
        //   }
        i += 1
        logger.trace(arg)
      }
    }

    if (args.length != 4) {
      MessagesText.KEEPINMIND_NOT_ENOUGH_PARAMS

    } else {
      Start.execute(msg)

      var date_meeting = args(0).concat(" ").concat(args(1))
      val simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm")
      val beginDate: Date = simpleDateFormat.parse(date_meeting)

      val ONE_MINUTE_IN_MILLIS = 60000
      val duration = Integer.parseInt(args(3)) * ONE_MINUTE_IN_MILLIS
      val endDate = new Date(beginDate.getTime() + duration)

      val intersectedEvents = ChatSession.hasIntersections(
        msg.chat.id, beginDate, endDate)
      logger.debug(intersectedEvents.toString)

      if (intersectedEvents.isEmpty) {
        val event = Event.create(msg.chat.id, beginDate, args(2), endDate)
        MessagesText.KEEPINMIND_DONE.format(event)
      } else {
        val stringBuilder = new StringBuilder(
          MessagesText.KEEPINMIND_INTERSECTIONS)
        intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
        stringBuilder.toString()
      }
    }
  }
}

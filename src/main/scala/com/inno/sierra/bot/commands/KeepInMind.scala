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
import java.util.Date


object KeepInMind extends LazyLogging {
  val dateRegex = """([0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4})"""
  val timeRegex = """([0-9]{2}[:][0-9]{2})"""
  var nameRex = """([A-Za-z0-9]{1,30})"""
  val duration = """([0-9]{1,4})"""
  val DateOnly = dateRegex.r
  val TimeOnly = timeRegex.r
  val NameMeeting = nameRex.r
  val TimeDuration = duration.r
  var regexError = 0;


  def verifyParameter(x: Any, y: scala.util.matching.Regex,z: Integer) = x match {
    //  case s: String =>  println(" - param match string : "+arg)
    //  case TimeDuration(d) =>  println(" - duration only : "+d+arg)
    //  case DateOnly(d) =>  println(" - date only : "+d+arg)
    //  case TimeOnly(d) =>  println(" - time only : "+d+arg)
    case
      test: String if y.findAllMatchIn(test).nonEmpty => logger.debug("Checked correct parameter: " +test)
    case _ => regexError = z;
  }
//MessagesText.KEEPINMIND_WRONG_FORMAT_PARAMS
  def execute(msg: Message): String = {
    val args = Extractors.commandArguments(msg).getOrElse(
      return MessagesText.KEEPINMIND_NOT_ENOUGH_PARAMS
    )
    val regex = MutableList[String]()

    var i = 0
    var indexError = 0;
    var noError = 0;
    var listRegex = List(DateOnly,TimeOnly,NameMeeting,TimeDuration)
    for (arg <- args) {
      logger.trace(arg)
      if (!arg.isEmpty && !arg.startsWith("/")) {
        logger.trace(listRegex(i).toString());
        indexError = i+1
        verifyParameter(arg, listRegex(i),indexError)
        i += 1
        logger.trace(arg)
      }
    }


   val resultMatch =  regexError match {
      case 1 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM1
      case 2 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM2
      case 3 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM3
      case 4 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM4;
      case _ => noError = 1; "";
    }


    if (resultMatch != ""){
      resultMatch.toString
    } else
     if (args.length != 4) {
      MessagesText.KEEPINMIND_NOT_ENOUGH_PARAMS;

    }else {


        val now = Calendar.getInstance().getTime()
        val simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm")
        val today = simpleDateFormat.format(now);
        val dateToday = simpleDateFormat.parse(today)
        var date_meeting = args(0).concat(" ").concat(args(1))

        val beginDate: Date = simpleDateFormat.parse(date_meeting)

        Start.execute(msg)
        if (dateToday.before(beginDate)){

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
      }else {
        logger.trace(" data antiga");
        MessagesText.EVENT_ALREADY_PASS
      }
    }
  }
}

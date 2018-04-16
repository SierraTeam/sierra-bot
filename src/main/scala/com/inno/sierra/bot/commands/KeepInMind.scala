package com.inno.sierra.bot.commands

import java.util.Calendar

import com.inno.sierra.bot.MessagesText
import info.mukel.telegrambot4s.api.Extractors
import info.mukel.telegrambot4s.models.Message

import scala.collection.mutable

object KeepInMind {
  val dateRegex = """([0-9]{2}.[0-9]{2}.[0-9]{4})"""
  val timeRegex = """([0-9]{2}:[0-9]{2})"""
  var nameRex = """([A-Za-z0-9]{1,30})"""
  val duration = """([0-9]{0,4})"""
  val DateOnly = dateRegex.r
  val TimeOnly = timeRegex.r
  val NameMeeting = nameRex.r
  val TimeDuration = duration.r


  def execute(msg: Message): String = {
    val args = Extractors.commandArguments(msg)

    Start.execute(msg)

    // TODo: maybe we need to check that the event planned is in the future
    val now = Calendar.getInstance().getTime()

    val parameter = mutable.MutableList[String]()
            val regex = mutable.MutableList[String]()

            var i = 0;
            var listRegex = List(DateOnly, TimeOnly, TimeDuration, NameMeeting)
            for (arg <- args) {


              if (!arg.isEmpty && !arg.startsWith("/")) {

                def verifyParameter(x: Any, y: scala.util.matching.Regex) = x match {
                  //  case s: String =>  println(" - param match string : "+arg)
                  //  case TimeDuration(d) =>  println(" - duration only : "+d+arg)
                  //  case DateOnly(d) =>  println(" - date only : "+d+arg)
                  //  case TimeOnly(d) =>  println(" - time only : "+d+arg)
                  case
                    test: String if y.findFirstMatchIn(test).nonEmpty => println("Checked correct parameter: " + arg)
                  case _ => println("Wrong parameter: " + arg)
                }

                verifyParameter(arg, listRegex(i));
                //  val unMactchParamerter = verifyParameter(arg,listRegex(i));
                //  if(unMactchParamerter == true){
                //   return "Wrong format of parameter"
                //   }


                i += 1
                logger.trace(arg)
                if (!arg.isEmpty && !arg.startsWith("/")) {
                  logger.trace(" - param is added")
                  //parametro += arg

                }
              }

              if (parameter.length != 4) {
                return "Create a meeting requires 4 parameters date, hour, name and duration(min)"
              } else {
                var date_meeting = parameter(0).concat(" ").concat(parameter(1))
                val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
                val simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm")
                val beginDate: Date = simpleDateFormat.parse(date_meeting)

                val ONE_MINUTE_IN_MILLIS = 60000
                val duration = Integer.parseInt(parameter(3)) * ONE_MINUTE_IN_MILLIS
                val endDate = new Date(beginDate.getTime() + duration)

                val intersectedEvents = ChatSession.hasIntersections(
                  msg.chat.id, beginDate, endDate)
                logger.debug(intersectedEvents.toString)

                if (intersectedEvents.isEmpty) {
                  val event = Event.create(msg.chat.id, beginDate, parameter(2), endDate)
                  return "The event " + event + " is recorded. I will remind you ;)" // TODO: phrases in different file!
                } else {
                  val stringBuilder = new StringBuilder(
                    "I'm sorry but this event intersects with another ones:\n ")
                  intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
                  return stringBuilder.toString()
                }
              }
            }
          }*/
    return MessagesText.ERROR_UNEXPTECTED
    //    }
  }
}

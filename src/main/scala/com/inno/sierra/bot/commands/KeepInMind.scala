package com.inno.sierra.bot.commands

import java.util.Calendar

import com.inno.sierra.bot.{MessagesText, SierraBot, Utils}
import com.inno.sierra.model.{ChatSession, Event}
import info.mukel.telegrambot4s.api.{Extractors, declarative}
import info.mukel.telegrambot4s.models.{ChatType, Message}
import com.typesafe.scalalogging.LazyLogging
import java.util.Date

import scala.collection.mutable.ListBuffer


object KeepInMind extends LazyLogging {
  private val dateRegex = """([0-9]{1,2}[.][0-9]{1,2}[.][0-9]{4})"""
  private val timeRegex = """([0-9]{2}[:][0-9]{2})"""
  private var nameRex = """([A-Za-z0-9]{1,30})"""
  private val duration = """([0-9]{1,4})"""
  private val DateOnly = dateRegex.r
  private val TimeOnly = timeRegex.r
  private val NameMeeting = nameRex.r
  private val TimeDuration = duration.r
  private var regexError = 0
  private final val ONE_MINUTE_IN_MILLIS = 60000


  def verifyParameters(args: declarative.Args): Option[String] = {
    var listRegex = List(DateOnly,TimeOnly,NameMeeting,TimeDuration)
    var i = 0
    for (arg <- args) {
      logger.debug(arg)
      logger.debug(listRegex(i).toString())
      if (listRegex(i).findAllMatchIn(arg).isEmpty) {
        val result = i match {
          case 0 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM1
          case 1 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM2
          case 2 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM3
          case 3 => MessagesText.KEEPINMIND_WRONG_FORMAT_PARAM4
        }
        return Some(result)
      }
      i += 1
      logger.debug(arg)
    }
    None
  }

  def execute(bot: SierraBot)(implicit msg: Message): String = {

    val args = Extractors.commandArguments(msg).getOrElse(
      return MessagesText.KEEPINMIND_NOT_ENOUGH_PARAMS
    )

    val resultMatch = verifyParameters(args)

    if (resultMatch.nonEmpty){
      resultMatch.get
    } else if (args.length != 4) {
      MessagesText.KEEPINMIND_NOT_ENOUGH_PARAMS

    } else {
      Start.execute(msg)

      val dateToday = Utils.simpleDateTimeFormat
        .parse(Utils.simpleDateTimeFormat.format(Calendar.getInstance().getTime))
      val beginDate = Utils.simpleDateTimeFormat
        .parse(args(0).concat(" ").concat(args(1)))

      if (dateToday.before(beginDate)){
        val duration = Integer.parseInt(args(3)) * ONE_MINUTE_IN_MILLIS
        val endDate = new Date(beginDate.getTime + duration)

        if (msg.chat.`type`.equals(ChatType.Private)) {
          val intersectedEvents = ChatSession.hasIntersections(
            msg.chat.id, beginDate, endDate)

          logger.debug(msg.chat.id.toString)
          logger.debug(beginDate + " - " + endDate + ": " + intersectedEvents)

          if (intersectedEvents.isEmpty) {
            val event = Event.create(msg.chat.id, beginDate, args(2), endDate)
            MessagesText.KEEPINMIND_DONE.format(event)
          } else {
            val stringBuilder = new StringBuilder(
              MessagesText.KEEPINMIND_INTERSECTIONS)
            intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
            stringBuilder.toString()
          }

        } else {
          val membersIds = ChatSession
            .getMembersOfGroup(msg.chat.id)
          val intersectedMembers = ListBuffer[String]()

          logger.debug(membersIds.toString())

          membersIds.foreach {m =>
            val intersectedEvents = ChatSession
              .hasIntersections(m.csid, beginDate, endDate)

            logger.debug(m.toString)
            logger.debug(intersectedEvents.toString)

            if (intersectedEvents.nonEmpty) {
              val stringBuilder = new StringBuilder(
                MessagesText.KEEPINMIND_INTERSECTIONS_PRIVATE_GROUP.format(msg.chat.title))
              intersectedEvents.foreach(stringBuilder.append(_).append("\n"))
              bot.sendMessage(m.csid, stringBuilder.toString())
              intersectedMembers.append(m.alias)
            }
          }
          if (intersectedMembers.isEmpty) {
            val event = Event.create(msg.chat.id, beginDate, args(2), endDate)
            MessagesText.KEEPINMIND_DONE.format(event)
          } else {
            val stringBuilder = new StringBuilder(
              MessagesText.KEEPINMIND_INTERSECTIONS_GROUP)
            intersectedMembers.foreach(m => stringBuilder.append(m + " "))
            stringBuilder.toString()
          }
        }

      }else {
        MessagesText.EVENT_ALREADY_PASS
      }
    }
  }
}

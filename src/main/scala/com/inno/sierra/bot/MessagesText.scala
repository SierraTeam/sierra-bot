package com.inno.sierra.bot

object MessagesText {

  val INFO: String =
    """Telegram bot created with Scala.
      |This bot is a simple Assistant that provides the following functionality:
      |/start: Starts this bot.
      |/keepinmind: Creates an Event to Keep in Mind.
      |Example: 24.04.2018 10:23 EventName 60.
      |Parameters are date, time, name of the event and its duration in minutes.
      |/myevents:  Displays list of all events scheduled for you.
      |/cancelevent:  Cancels the event. Id of event can be passed as a parameter.
      |/info:  Displays description (this text).
      |/exit:  TODO.
      """.stripMargin

  val START_FIRST_TIME: String =
    """Nice to meet you, %s! I can help
      |you to plan your activities. I'll try to be useful for you :)
    """.stripMargin

  val START_AGAIN: String =
    """Welcome back, %s! I'm glad to see you again :)
      |How can I help you?
    """.stripMargin

  val SUBSCRIBE_DONE: String =
    "Now you will be informed about the events in this group :)"

  val SUBSCRIBE_ALREADY: String =
    "You are already subscribed to the events in this chat ;)"

  val UNSUBSCRIBE_DONE: String =
    "Now you will NOT be informed about the events in this group :)"

  val UNSUBSCRIBE_CANNOT: String =
    "You cannot unsubscribe in private chats. " +
      "You need to do better to get rid of me ;)"

  val KEEPINMIND_DONE: String =
    "The event %s is recorded. I will remind you ;)"

  val KEEPINMIND_INTERSECTIONS: String =
    "I'm sorry but this event intersects with another ones:\n "

  val KEEPINMIND_NOT_ENOUGH_PARAMS: String =
    "Create an event requires 4 parameters date, hour, name and duration(min). " +
      "Or you can just use /keepinmind command and I will ask you explicitly all I need ;)"

  val KEEPINMIND_CORRECT_PARAMS: String =
    "Try again with correct parameters"

  val KEEPINMIND_WRONG_FORMAT_PARAM1: String =
    "The date, first parameter, should follow the format dd.mm.YYYY"

  val KEEPINMIND_WRONG_FORMAT_PARAM2: String =
    "The time, second parameter, should follow the format HH:MM"

  val KEEPINMIND_WRONG_FORMAT_PARAM3: String =
    "The name, third parameter, should follow the format letters and/or numbers"

  val KEEPINMIND_WRONG_FORMAT_PARAM4: String =
    "The duration, fourth parameter, should be a number"

  val KEEPINMIND2_EVENT_NAME: String =
    "Alright, a new event. How are we going to call it? " +
      "Please choose a name for an event."

  val KEEPINMIND2_EVENT_DATE: String =
    "Good. Now let's choose a date for your event. " +
      "Use the calendar widget to enter the planned date."

  val KEEPINMIND2_EVENT_TIME: String =
    "Please choose the time for the event"

  val KEEPINMIND2_EVENT_DURATION: String =
    ""

  val CANCEL_MISSING_PARAM: String =
    "Please specify number of event you want to cancel"

  val NOTIFICATION: String =
    "I want to remind you that you have an event '%s' at %s"

  val ERROR_UNEXPTECTED: String =
    "I'm so sorry! It seems something went wrong, try again, please."

  val NO_EVENTS_FOUND: String =
    "There is no upcoming events recorded for you."

  val EVENT_ALREADY_PASS: String =
    "The event must be in the future, to be schedule."

  val CANCEL_DONE: String =
    "Event was canceled"

  val CANCEL_FAILED: String =
    "Error: Unable to chancel this event"
}

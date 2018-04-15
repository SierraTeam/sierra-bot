package com.inno.sierra.bot

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

import scala.collection.mutable

/**
  * Code is based on
  * https://rosettacode.org/wiki/Calendar#Scala_Version_2
  *
  * @param monthInYear
  * @param year
  */
case class MonthCalendar(monthInYear: Int, year: Int) {
  private val javaCalendar = makeJavaCalendar(year, monthInYear)
  private def makeJavaCalendar(year: Int, monthInYear: Int): Calendar = {
    val calendar = new GregorianCalendar()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, monthInYear - 1)
    calendar
  }
  val monthName: String = javaCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault)
  val dayOfWeek: Int = javaCalendar.get(Calendar.DAY_OF_WEEK)
  val daysInMonth: Seq[Int] = {
    val tempCal = makeJavaCalendar(year, monthInYear)
    val dayNumbers = mutable.Buffer[Int]()
    while (tempCal.get(Calendar.MONTH) == monthInYear - 1) {
      dayNumbers += tempCal.get(Calendar.DAY_OF_MONTH)
      tempCal.add(Calendar.DATE, 1)
    }
    dayNumbers
  }
}

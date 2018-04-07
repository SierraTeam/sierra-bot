package com.inno.sierra.bot

import java.util.Date

import com.inno.sierra.model.Event
import info.mukel.telegrambot4s.methods.SendMessage

class RunnableNotification(event:Event) extends Runnable{
  override def run() {
//    Thread.sleep(event.time.getTime - new Date().getTime)
    //SendMessage(event.name+ "is now")
    //TODO actually send notification instead printing to console
    System.out.println("Notifying about " + event.name)
  }
}

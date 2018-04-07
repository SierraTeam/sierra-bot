package com.inno.sierra.bot

import java.util.Date
import java.util.concurrent.{ExecutorService, Executors}

import com.inno.sierra.model.DbSchema


class NotifierService (poolSize: Int) extends Runnable{
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

  override def run(): Unit = {
    try {
      while (true) {
        val tillDate = new Date(new Date().getTime + 60000) //10 min after now
        val events = DbSchema.getAllEventsTillDate(tillDate)
        events.foreach(e=>{
          val runnableNotification = new RunnableNotification(e)
          pool.execute(runnableNotification)
        })
        Thread.sleep(60000)
      }
    } finally {
      pool.shutdown()
    }
  }
}

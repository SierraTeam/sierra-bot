package com.inno.sierra.bot

import java.util.Date
import java.util.concurrent.{ExecutorService, Executors}

import com.inno.sierra.model.DbSchema

import scala.concurrent.{ExecutionContext, Future}


class NotifierService (poolSize: Int){
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

  implicit val ec = ExecutionContext.fromExecutorService(pool)

  def sendMessages(): Future[Unit] = Future {
    val tillDate = new Date(new Date().getTime + 10000)
    val events = DbSchema.getAllEventsTillDate(tillDate)
    events.foreach(e=>{
      val runnableNotification = new RunnableNotification(e)
      runnableNotification.sendNotification()
    })
  }
}

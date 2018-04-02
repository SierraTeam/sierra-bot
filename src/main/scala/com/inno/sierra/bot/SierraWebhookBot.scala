package com.inno.sierra.bot

import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.api.Webhook

class SierraWebhookBot extends SierraBot with Webhook {

  override val port = (scala.util.Properties.envOrNone("http.port") match {
    case Some(value) => value
    case None => ConfigFactory.load().getString("http.port")
  }).toInt
  override val webhookUrl = scala.util.Properties.envOrNone("bot.webhookurl")  match {
    case Some(value) => value
    case None => ConfigFactory.load().getString("bot.webhookurl")
  }

}

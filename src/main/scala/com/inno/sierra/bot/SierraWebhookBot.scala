package com.inno.sierra.bot

import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.api.Webhook

class SierraWebhookBot extends SierraBot with Webhook {

  override val port = ConfigFactory.load().getString("http.port").toInt
  override val webhookUrl = ConfigFactory.load().getString("bot.webhookurl")

}

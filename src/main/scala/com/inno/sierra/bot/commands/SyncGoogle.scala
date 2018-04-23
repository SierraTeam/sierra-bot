package com.inno.sierra.bot.commands

import java.text.SimpleDateFormat

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import java.io.{File, IOException, InputStreamReader}
import java.util

import scala.collection.JavaConversions._
import com.google.api.client.auth.oauth2.{AuthorizationCodeRequestUrl, Credential, TokenResponse}
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleAuthorizationCodeRequestUrl, GoogleClientSecrets}
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.api.services.calendar.model.{Event, Events}
import com.inno.sierra.bot.SierraBot
import com.inno.sierra.model.{ChatSession, ChatState}
import info.mukel.telegrambot4s.api.Extractors
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message, ReplyMarkup}

import scala.concurrent.ExecutionContext

object SyncGoogle {

  /** Application name. */
  private val APPLICATION_NAME: String = "Google Calendar API Java Quickstart"

  /** Directory to store user credentials for this application. */
  private val DATA_STORE_DIR: File = new File(System.getProperty("user.home"), ".credentials/calendar-java-quickstart")

  /** Global instance of the {@link FileDataStoreFactory}. */
  private var DATA_STORE_FACTORY: FileDataStoreFactory = null

  /** Global instance of the JSON factory. */
  private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance

  /** Global instance of the HTTP transport. */
  private var HTTP_TRANSPORT: HttpTransport = null

  /** Global instance of the scopes required by this quickstart.
    *
    * If modifying these scopes, delete your previously saved credentials
    * at ~/.credentials/calendar-java-quickstart
    */
  private val SCOPES: util.List[String] = util.Arrays.asList(CalendarScopes.CALENDAR_READONLY)

  private lazy val clientSecrets: GoogleClientSecrets = {
    // Load client secrets.
    val in = getClass.getResourceAsStream("/client_secret.json")
    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
  }

  try {
    HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      System.exit(1)
  }

  /**
    *
    * @return isFree
    * @throws IOException
    */
  @throws[IOException]
  def getEventsWindow(service: Calendar, timeMin: DateTime, timeMax: DateTime): Boolean = {
    var isFree: Boolean = true
    val events: Events = service
      .events
      .list("primary")
      .setMaxResults(10) //todo create a environment variable to make it easily modifiable
      .setTimeMin(timeMin)
      .setTimeMax(timeMax)
      .setOrderBy("startTime")
      .setSingleEvents(true)
      .execute
    val items: util.List[Event] = events.getItems
    if (items.isEmpty) {
      System.out.println("No upcoming events for your time window. Proceed")
    }
    else {
      isFree = false
      System.out.println("No free time for slot. Upcoming events are:")
      for (event <- items) {
        System.out.println("Event    : " + event.getSummary)
        System.out.println("Starts at " + event.getStart.getDateTime + "and ends at " + event.getEnd.getDateTime)
      }
    }
    isFree
  }

  def createFlow(): GoogleAuthorizationCodeFlow  = {
    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(DATA_STORE_FACTORY)
      .setAccessType("offline")
      .build
    flow
  }

  /**
    * Creates an authorized Credential object.
    *
    * @return an authorized Credential object.
    * @throws IOException
    * previously static
    */
  @throws[IOException]
  protected def authorize(chatId: Long): Either[Credential, GoogleAuthorizationCodeRequestUrl] = {
    val userId = chatId.toString
    val flow = createFlow()
    val credential = flow.loadCredential(userId)
    if (credential != null && (credential.getRefreshToken != null || credential.getExpiresInSeconds == null || credential.getExpiresInSeconds > 60)) {
      Left(credential)
    } else {
      val redirectUri = clientSecrets.getInstalled.getRedirectUris.get(0)
      val authorizationUrl = flow
        .newAuthorizationUrl
        .setRedirectUri(redirectUri)
      Right(authorizationUrl)
    }
  }

  /**
    * Creates an authorized Credential object.
    *
    * @return an authorized Credential object.
    * @throws IOException
    * previously static
    */
  @throws[IOException]
  protected def getCredential(chatId: Long, code: String): Credential = {
    val userId = chatId.toString
    val flow = createFlow()

    val redirectUri = clientSecrets.getInstalled.getRedirectUris.get(0)

    // receive authorization code and exchange it for an access token
    val response = flow
      .newTokenRequest(code)
      .setRedirectUri(redirectUri)
      .execute

    // store credential and return it
    val credential = flow.createAndStoreCredential(response, userId)
    //        Credential credential = new AuthorizationCodeInstalledApp(
    //                flow, new LocalServerReceiver()).authorize("user");
    System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath)
    credential
  }

  /** s
    * Build and return an authorized Calendar client service.
    *
    * @return an authorized Calendar client service
    * @throws IOException
    * previously static
    */
  @throws[IOException]
  def getCalendarService(credential: Credential): Calendar = {
    new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build
  }

  def checkFree(bot: SierraBot)(implicit msg: Message, ec: ExecutionContext): Unit = {
    Start.execute(msg)

    val (responseText: String, responseMarkup: Option[InlineKeyboardMarkup]) = authorize(msg.chat.id) match {
      case Right(authorizationCodeRequestUrl) => (
        "You must first authorize me! Open the following link and send me the activation code: \n" + authorizationCodeRequestUrl.build(),
        Some(
          InlineKeyboardMarkup.singleButton(
            InlineKeyboardButton.url("Authorize access to your Google Calendar", authorizationCodeRequestUrl.build())
          )
        )
      )
      case Left(credential) =>
        val calendarService = getCalendarService(credential)
        val inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm")

        val date = "19/04/2018 17:00"
        val ONE_HOUR_IN_MILLIS = 60000*60
        val minTime:DateTime = new DateTime(System.currentTimeMillis())
        val endTime:DateTime = new DateTime(System.currentTimeMillis() + ONE_HOUR_IN_MILLIS)

        val amIFree: Boolean = getEventsWindow(calendarService, minTime, endTime)
        (amIFree.toString, None)
    }

    bot.reply(
      responseText,
      replyMarkup = responseMarkup
    ).map { implicit msg =>

      for {
        chatSession <- ChatSession.getByChatId(msg.chat.id)
      } /* do */ {
        chatSession.chatState = ChatState.AuthorizingGoogleCalendar
        chatSession.save()
      }

    }
  }

  def onMessage(bot: SierraBot)(implicit msg: Message, ec: ExecutionContext): Unit = {

    for {
      chatSession <- ChatSession.getByChatId(msg.chat.id)
      if chatSession.chatState == ChatState.AuthorizingGoogleCalendar
      code <- msg.text
      command <- Extractors.textTokens(msg).map(_.head)
      if command != "/sync"
    } /* do */ {

        chatSession.chatState = ChatState.Started
        chatSession.save()

        val credential = getCredential(msg.chat.id, code)

        bot.reply(
          "Successfully authorized! I will now check your events and prevent intersection when appointing time for new events." + credential.getAccessToken
        )

    }

  }

}

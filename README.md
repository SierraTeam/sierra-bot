# Sierra Bot #

The bot is deployed at Heroku and available by its alias @InnoSierraBot.

## User manual - Functionality ##

This bot is a simple assistant that provides the following functionality:
* /start: Starts this bot.
* /keepinmind: Creates an Event to Keep in Mind (bot's mind).  
Example: 24.04.2018 10:23 EventName 60.  
Parameters are date, time, name of the event and its duration in minutes.
* /myevents:  Displays list of all events scheduled for you.
* /cancelevent:  Cancels the event. Id of event can be passed as a parameter.
* /info:  Displays description (this text).
* /subscribe: Subscribes to the group events.
* /unsubscribe: Unsubscribes from the group events.
* /suggesttime: Suggests the time suitable for all the subscribed users for a specified day.
Example: /suggesttime 24.04.2018

First of all, start the bot using /start  

Then, you can create the reminder using /keepinmind 24.04.2018 15:30 GoToExam 60
It will remind you on April 24 at 15:30 to go to exam, which lasts for 60 minutes.  
Also, you can just type /keepinmind and follow the instructions.  

To see the list of created events type /myevents. It will display all events scheduled for you, starting with event number.

To cancel the event use /cancelevent. It will display all the events you can cancel.  
Also you can use /cancelevent 1 to cancel the event number 1.  

To see the the instructions type /info.

In order to participate in group events (be accounted when the event is 
been created and been notified about), you need to /subscribe in the group. Later, if you changed 
your mind, you can /unsubscribe.

In order to facilitate the process of finding the suitable time for everybody, you can use 
/suggesttime command with the date provided (without the date it will show the result for the current day). The result of the command is the time slots available for all the subscribed users in the current group chat.

Example: /suggesttime 24.04.2018.

(PROTOTYPE - branch "feature/google-calendar", bot does not remember the token yet) You can authorize your Google Calendar account with the bot using command /sync, so that it could
check your schedule in Google Calendar (not implemented yet) in order to find suitable time for you. 

## Build & Run ##

In order to run the bot you need to create the file with the configuration:
```sh
/src/main/resources/application.conf
```
The content should be as follows (you need to replace the settings with yours):
```sh
bot{
  token = "9999999YOUR-BOT-TOKEN99999999"
}

db{
  connection = "jdbc:h2:~/sierrabot"
  username = "sa"
  password = ""
}

time{
  coeficient = 0
}
```

time.coeficient parameter is used to adjust the time at server, should be zero at local machine.

Reference configuration file is used for deployment at Heroku.
```sh
/src/main/resources/reference.conf
```

To run the bot use sbt (you need to be outside Russia for bot to work):
```sh
$ cd sierra-bot
$ sbt
> compile
> run
```

## Additional Information ##
There is a code analyser set up, the settings are in the following file:
```sh
/project/scalastyle-config.xml
```

## Deploy on Heroku ##

First set up some config vars in Heroku, they will be passed as environment variables to the bot:
```
BOT_POLLING=false
BOT_TOKEN=<token>
BOT_WEBHOOKURL=<https_heroku_app_url>
DB_CONNECTION=jdbc:<jdbc_connection>
DB_USERNAME=sa
DB_PASSWORD=
```

To deploy from local machine use `sbt stage deployHeroku` or in more detailed:
```sh
$ cd sierra-bot
$ sbt
> stage
> deployHeroku
```

## Test ##

To run tests:
```sh
$ sbt test
```

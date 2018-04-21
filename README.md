# Sierra Bot #

## User manual ##

This bot is a simple assistant that provides the following functionality:
* /start: Starts this bot.
* /keepinmind: Creates an Event to Keep in Mind.  
Example: 24.04.2018 10:23 EventName 60.  
Parameters are date, time, name of the event and its duration in minutes.
* /myevents:  Displays list of all events scheduled for you.
* /cancelevent:  Cancels the event. Id of event can be passed as a parameter.
* /info:  Displays description (this text).
* /exit:  TODO.

First of all, start the bot using /start  

Then, you can create the reminder using /keepinmind 24.04.2018 15:30 GoToExam 60
It will remind you on April 24 at 15:30 to go to exam, which lasts for 60 minutes.  
Also, you can just type /keepinmind and follow the instructions.  

To see the list of created events type /myevents. It will display all events scheduled for you, starting with event number.

To cancel the event use /cancelevent. It will display all the events you can cancel.  
Also you can use /cancelevent 1 to cancel the event number 1.  

To see the the instructions type /info

## Build & Run ##

Make a copy of reference settings and then configure application.conf for your needs:

```sh
cp ./src/main/resources/reference.conf ./src/main/resources/application.conf
```

To run the bot use sbt:

```sh
$ cd sierra-bot
$ sbt
> compile
> run
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

## Test (TODO) ##

To run tests:

```sh
$ sbt test
```

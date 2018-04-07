# Sierra Bot #

A telegram bot to manage schedule:

* Functionality 1
* Functionality 2
* Functionality 3
* etc.

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

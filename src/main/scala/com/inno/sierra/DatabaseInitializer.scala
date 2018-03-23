package com.inno.sierra

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.{H2Adapter, MySQLAdapter}
import org.squeryl.Session
import org.squeryl.SessionFactory
import org.slf4j.LoggerFactory

object DatabaseInitializer {
  val logger = LoggerFactory.getLogger(getClass)

  val databaseUsername = "sa"
  val databasePassword = ""
  val databaseConnection = "jdbc:h2:~/sierrabot"

  val maxPoolSize = 50

  var cpds = new ComboPooledDataSource

  def configureDb() {
    cpds.setDriverClass("org.h2.Driver")
    cpds.setJdbcUrl(databaseConnection)
    cpds.setUser(databaseUsername)
    cpds.setPassword(databasePassword)

    cpds.setMinPoolSize(1)
    cpds.setAcquireIncrement(1)
    cpds.setMaxPoolSize(maxPoolSize)

    SessionFactory.concreteFactory = Some(() => connection)

    def connection = {
      logger.info("Creating connection with c3po connection pool")
      Session.create(cpds.getConnection, new H2Adapter)
    }
  }

  def closeDbConnection() {
    logger.info("Closing c3po connection pool")
    cpds.close()
  }
}

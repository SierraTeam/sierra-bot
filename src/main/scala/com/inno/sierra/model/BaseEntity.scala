package com.inno.sierra.model

import org.squeryl.KeyedEntity

/**
  * Every model object has an id
  */
class BaseEntity extends KeyedEntity[Long] {
  var id: Long = 0
}

package com.inno.sierra.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

case class GroupMembers (
                               var groupId: Long,
                               var memberId: Long)
  extends KeyedEntity[CompositeKey2[Long,Long]] {

  def id = CompositeKey2(groupId, memberId)
}

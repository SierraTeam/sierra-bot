package com.inno.sierra.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

/**
  * Represents the relation between group and its
  * members in the database.
  * @param groupId  id of the group
  * @param memberId id of the member
  */
case class GroupMembers (
                               var groupId: Long,
                               var memberId: Long)
  extends KeyedEntity[CompositeKey2[Long,Long]] {

  def id = CompositeKey2(groupId, memberId)
}

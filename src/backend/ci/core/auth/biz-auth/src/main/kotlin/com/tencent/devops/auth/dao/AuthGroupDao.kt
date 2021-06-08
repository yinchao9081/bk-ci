/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.entity.GroupCreateInfo
import com.tencent.devops.model.auth.tables.TAuthGroup
import com.tencent.devops.model.auth.tables.records.TAuthGroupRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthGroupDao {

    fun createGroup(dslContext: DSLContext, groupCreateInfo: GroupCreateInfo) {
        with(TAuthGroup.T_AUTH_GROUP) {
            dslContext.insertInto(
                this,
                GROUP_NAME,
                GROUP_CODE,
                GROUP_TYPE,
                RELATION_ID,
                DISPLAY_NAME,
                PROJECT_CODE,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME
            ).values(
                groupCreateInfo.groupName,
                groupCreateInfo.groupCode,
                groupCreateInfo.groupType,
                groupCreateInfo.relationId,
                groupCreateInfo.displayName,
                groupCreateInfo.projectCode,
                groupCreateInfo.user,
                LocalDateTime.now(),
                null,
                null
            ).execute()
        }
        return
    }

    fun getGroup(dslContext: DSLContext, projectCode: String, groupCode: String): TAuthGroupRecord? {
        with(TAuthGroup.T_AUTH_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode).and(GROUP_CODE.eq(groupCode).and(IS_DELETE.eq(false)))).fetchAny()
        }
    }
    
    fun getGroupByCodes(
        dslContext: DSLContext,
        projectCode: String,
        groupCodes: List<String>
    ): Result<TAuthGroupRecord?> {
        with(TAuthGroup.T_AUTH_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode).and(GROUP_CODE.`in`(groupCodes).and(IS_DELETE.eq(false)))).fetch()
        }
    }

    fun getGroupById(dslContext: DSLContext, groupId: Int): TAuthGroupRecord? {
        with(TAuthGroup.T_AUTH_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId)).fetchOne()
        }
    }
    
    fun batchCreateGroups(dslContext: DSLContext, groups: List<GroupCreateInfo>) {
        if (groups.isEmpty()) {
            return
        }
        dslContext.batch(groups.map {
            with(TAuthGroup.T_AUTH_GROUP) {
                dslContext.insertInto(
                    this,
                    GROUP_NAME,
                    GROUP_CODE,
                    GROUP_TYPE,
                    RELATION_ID,
                    DISPLAY_NAME,
                    PROJECT_CODE,
                    CREATE_USER,
                    CREATE_TIME,
                    UPDATE_USER,
                    UPDATE_TIME
                ).values(
                    it.groupName,
                    it.groupCode,
                    it.groupType,
                    it.relationId,
                    it.displayName,
                    it.projectCode,
                    it.user,
                    LocalDateTime.now(),
                    null,
                    null
                )
            }
        }).execute()
    }
}

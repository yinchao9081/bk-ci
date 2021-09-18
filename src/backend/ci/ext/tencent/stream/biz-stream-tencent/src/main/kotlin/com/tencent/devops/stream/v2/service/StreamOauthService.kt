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

package com.tencent.devops.stream.v2.service

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.stream.v2.dao.GitCIBasicSettingDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import org.jooq.DSLContext

@Service
class StreamOauthService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCIBasicSettingDao: GitCIBasicSettingDao
) {

    fun getAndCheckOauthToken(
        userId: String
    ): GitToken {
        return client.get(ServiceOauthResource::class).gitGet(userId).data ?: throw OauthForbiddenException(
            message = "用户$userId 无OAuth权限"
        )
    }

    fun getOauthToken(
        userId: String
    ): GitToken? {
        return client.get(ServiceOauthResource::class).gitGet(userId).data
    }

    fun getOauthTokenNotNull(
        userId: String
    ): GitToken {
        return client.get(ServiceOauthResource::class).gitGet(userId).data
            ?: throw RuntimeException("用户${userId}未进行OAuth授权")
    }

    fun getGitCIEnableToken(
        gitProjectId: Long
    ): GitToken {
        val userId = gitCIBasicSettingDao.getSetting(dslContext, gitProjectId)?.enableUserId
            ?: throw RuntimeException("工蜂项目${gitProjectId}未开启Stream")
        return try {
            client.get(ServiceOauthResource::class).gitGet(userId).data!!
        } catch (e: Exception) {
            throw RuntimeException("用户${userId}未进行OAuth授权")
        }
    }
}

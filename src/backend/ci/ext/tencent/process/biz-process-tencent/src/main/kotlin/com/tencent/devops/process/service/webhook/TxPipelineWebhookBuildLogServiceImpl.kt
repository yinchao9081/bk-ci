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

package com.tencent.devops.process.service.webhook

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineWebhookBuildLogDetailDao
import com.tencent.devops.process.engine.service.PipelineWebhookBuildLogService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.webhook.PipelineWebhookBuildLog
import com.tencent.devops.process.pojo.webhook.PipelineWebhookBuildLogDetail
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TxPipelineWebhookBuildLogServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineWebhookBuildLogDetailDao: PipelineWebhookBuildLogDetailDao,
    private val pipelinePermissionService: PipelinePermissionService
) : PipelineWebhookBuildLogService {

    override fun saveWebhookBuildLog(webhookBuildLog: PipelineWebhookBuildLog) {
        pipelineWebhookBuildLogDetailDao.save(
            dslContext = dslContext,
            logId = 0L,
            webhookBuildLogDetails = webhookBuildLog.detail
        )
    }

    override fun listWebhookBuildLogDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        repoName: String?,
        commitId: String?,
        page: Int?,
        pageSize: Int?
    ): SQLPage<PipelineWebhookBuildLogDetail> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: DEFAULT_PAGE_SIZE
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
        val count = pipelineWebhookBuildLogDetailDao.countByPage(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            repoName = repoName,
            commitId = commitId
        )
        val records = pipelineWebhookBuildLogDetailDao.listByPage(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            repoName = repoName,
            commitId = commitId,
            offset = limit.offset,
            limit = limit.limit
        )
        return SQLPage(count = count, records = records)
    }
}

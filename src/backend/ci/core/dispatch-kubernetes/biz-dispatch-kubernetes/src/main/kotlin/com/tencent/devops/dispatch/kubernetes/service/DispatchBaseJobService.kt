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

package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobLogResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchEnumType
import com.tencent.devops.dispatch.kubernetes.utils.JobRedisUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DispatchBaseJobService @Autowired constructor(
    private val dispatchFactory: DispatchTypeServiceFactory,
    private val jobRedisUtils: JobRedisUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchBaseJobService::class.java)
    }

    fun createJob(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchType: DispatchEnumType,
        jobReq: DispatchJobReq
    ): DispatchTaskResp {
        logger.info("projectId: $projectId, buildId: $buildId create $dispatchType jobContainer. userId: $userId")

        // 检查job数量是否超出限制
        if (jobRedisUtils.getJobCount(dispatchType, buildId, jobReq.podNameSelector) > 10) {
            throw ErrorCodeException(
                statusCode = 500,
                errorCode = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.formatErrorMessage
            )
        }
        jobRedisUtils.setJobCount(dispatchType, buildId, jobReq.podNameSelector)

        return dispatchFactory.load(dispatchType).createJob(userId, jobReq)
    }

    fun getJobStatus(userId: String, dispatchType: DispatchEnumType, jobName: String): DispatchBuildStatusResp {
        return dispatchFactory.load(dispatchType).getJobStatus(userId, jobName)
    }

    fun getJobLogs(
        userId: String,
        dispatchType: DispatchEnumType,
        jobName: String,
        sinceTime: Int?
    ): DispatchJobLogResp {
        return dispatchFactory.load(dispatchType).getJobLogs(userId, jobName, sinceTime)
    }

    fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchType: DispatchEnumType,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp {
        return dispatchFactory.load(dispatchType)
            .buildAndPushImage(userId, projectId, buildId, dispatchBuildImageReq)
    }
}

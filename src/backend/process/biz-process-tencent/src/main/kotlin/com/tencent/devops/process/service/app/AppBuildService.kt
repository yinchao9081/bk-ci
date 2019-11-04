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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.app

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.pipeline.AppModelDetail
import com.tencent.devops.process.service.label.PipelineGroupService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AppBuildService @Autowired constructor(
    private val buildService: PipelineBuildService,
    private val pipelineService: PipelineService,
    private val pipelineGroupService: PipelineGroupService,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AppBuildService::class.java)
    }

    fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): AppModelDetail {
        // 查web端数据
        var beginTime = System.currentTimeMillis()
        val modelDetail =
            buildService.getBuildDetail(userId, projectId, pipelineId, buildId, channelCode, checkPermission)
        logger.info("查web端数据: ${System.currentTimeMillis() - beginTime} ms")
        beginTime = System.currentTimeMillis()

        // 文件个数、版本
        val files = client.get(ServiceArtifactoryResource::class)
            .search(projectId, null, null, listOf(Property("pipelineId", pipelineId), Property("buildId", buildId)))
            .data
        val packageVersion = StringBuilder()
        files?.records?.forEach {
            val singlePackageVersion =
                client.get(ServiceArtifactoryResource::class).show(projectId, it.artifactoryType, it.path)
                    .data?.meta?.get(ARCHIVE_PROPS_APP_VERSION)
            if (!singlePackageVersion.isNullOrBlank()) packageVersion.append(singlePackageVersion).append(";")
        }
        logger.info("查文件个数、版本: ${System.currentTimeMillis() - beginTime} ms")
        beginTime = System.currentTimeMillis()

        // 查流水线信息
        val (name, version) = pipelineService.getPipelineNameVersion(pipelineId)
        logger.info("查流水线信息: ${System.currentTimeMillis() - beginTime} ms")
        beginTime = System.currentTimeMillis()

        // 查询收藏的流水线
        val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
        logger.info("查询收藏的流水线: ${System.currentTimeMillis() - beginTime} ms")

        return AppModelDetail(
            modelDetail.id,
            modelDetail.userId,
            modelDetail.trigger,
            modelDetail.startTime,
            modelDetail.endTime,
            modelDetail.status,
            modelDetail.currentTimestamp,
            modelDetail.buildNum,
            modelDetail.cancelUserId,
            files?.records?.size ?: 0,
            packageVersion.toString().removeSuffix(";"),
            pipelineId,
            version,
            name,
            projectId,
            favorPipelines.contains(pipelineId),
            modelDetail.model
        )
    }
}

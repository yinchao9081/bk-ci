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

package com.tencent.devops.gitci.listener

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIBuildFinishListener @Autowired constructor(
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitCISettingDao: GitCISettingDao,
    private val scmClient: ScmClient,
    private val dslContext: DSLContext
) {

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = MQ.QUEUE_PIPELINE_BUILD_FINISH_GITCI, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineBuildFinishBroadCastEvent(buildFinishEvent: PipelineBuildFinishBroadCastEvent) {
        try {
            val record = gitRequestEventBuildDao.getEventByBuildId(dslContext, buildFinishEvent.buildId)
            if (record != null) {
                val normalizerYaml = record["NORMALIZED_YAML"] as String
                logger.info("listenPipelineBuildFinishBroadCastEvent , normalizerYaml : $normalizerYaml")
                val yamlObject = YamlUtil.getObjectMapper().readValue(normalizerYaml, CIBuildYaml::class.java)

                val objectKind = record["OBJECT_KIND"] as String

                // 推送结束构建消息,当人工触发时不推送构建消息
                if (objectKind != OBJECT_KIND_MANUAL) {
                    val commitId = record["COMMIT_ID"] as String
                    val gitProjectId = record["GIT_PROJECT_ID"] as Long
                    var mergeRequestId = 0L
                    if (record["MERGE_REQUEST_ID"] != null) {
                        mergeRequestId = record["MERGE_REQUEST_ID"] as Long
                    }
                    val description = record["DESCRIPTION"] as String

                    val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId)
                        ?: throw OperationException("git ci projectCode not exist")

                    // 检测状态
                    val state = if (BuildStatus.isFailure(BuildStatus.valueOf(buildFinishEvent.status))) {
                        "failure"
                    } else {
                        "success"
                    }

                    scmClient.pushCommitCheck(
                        commitId,
                        description,
                        mergeRequestId,
                        buildFinishEvent.buildId,
                        buildFinishEvent.userId,
                        state,
                        yamlObject.name ?: "",
                        gitProjectConf
                    )
                }
            }
        } catch (e: Throwable) {
            logger.error("Fail to push commit check build(${buildFinishEvent.buildId})", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBuildFinishListener::class.java)
    }
}

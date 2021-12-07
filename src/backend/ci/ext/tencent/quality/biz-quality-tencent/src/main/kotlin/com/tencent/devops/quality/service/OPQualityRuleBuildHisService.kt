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

package com.tencent.devops.quality.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.model.quality.tables.records.TQualityRuleBuildHisRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisOperationDao
import com.tencent.devops.quality.service.v2.QualityRuleBuildHisService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OPQualityRuleBuildHisService @Autowired constructor(
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityRuleBuildHisOperationDao: QualityRuleBuildHisOperationDao,
    private val dslContext: DSLContext,
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(QualityRuleBuildHisService::class.java)

    fun updateRuleBuildHisStatus(): Int {
        var count = 0
        val dateTime = LocalDateTime.now().minusDays(1)
        val ruleCount = qualityRuleBuildHisDao.listTimeOutRuleCount(dslContext, dateTime)
        val timeOutRules = mutableListOf<TQualityRuleBuildHisRecord>()
        if (null != ruleCount && count < ruleCount) {
            timeOutRules.addAll(qualityRuleBuildHisDao.listTimeoutRule(dslContext, dateTime, 50, count))
            count += 50
        }
        logger.info("QUALITY|time_out_rule count is: $ruleCount")
        if (timeOutRules.size > 0) {
            count = qualityRuleBuildHisDao.updateTimeoutRuleStatus(timeOutRules.map { it.id })
            logger.info("QUALITY|update_rule_status_count: $count")
            val processClient = client.get(ServiceBuildResource::class)
            timeOutRules.map { rule ->
                try {
                    val trigger = processClient.qualityTriggerStage(
                        userId = rule.createUser,
                        projectId = rule.projectId,
                        pipelineId = rule.pipelineId,
                        buildId = rule.buildId,
                        stageId = rule.stageId,
                        qualityRequest = StageQualityRequest(
                            position = rule.rulePos,
                            pass = false,
                            checkTimes = 1
                        )
                    ).data ?: false
                    qualityRuleBuildHisOperationDao.create(dslContext, rule.createUser, rule.id, rule.stageId)
                    logger.info("QUALITY|project: ${rule.projectId}, pipelineId: ${rule.pipelineId}, " +
                            "buildId: ${rule.buildId}, trigger: $trigger")
                } catch (e: Exception) {
                    logger.error("QUALITY|project: ${rule.projectId}, pipelineId: ${rule.pipelineId}, " +
                            "buildId: ${rule.buildId} has triggered")
                }
            }
        }
        return count
    }
}

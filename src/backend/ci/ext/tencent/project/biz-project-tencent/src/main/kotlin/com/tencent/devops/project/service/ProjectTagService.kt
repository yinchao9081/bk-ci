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

package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.consul.ConsulConstants.PROJECT_TAG_REDIS_KEY
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectTagDao
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ProjectTagService @Autowired constructor(
    val dslContext: DSLContext,
    val projectTagDao: ProjectTagDao,
    val redisOperation: RedisOperation,
    val projectDao: ProjectDao,
    val objectMapper: ObjectMapper
) {

    private val executePool = Executors.newFixedThreadPool(1)

    @Value("\${system.router}")
    val routerTagList: String = ""

    @Value("\${auto.tag:#{null}}")
    private val autoTag: String? = null

    fun updateTagByProject(
        projectTagUpdateDTO: ProjectTagUpdateDTO
    ): Result<Boolean> {
        logger.info("updateTagByProject: $projectTagUpdateDTO")
        checkRouteTag(projectTagUpdateDTO.routerTag)
        checkProject(projectTagUpdateDTO.projectCodeList)
        projectTagDao.updateProjectTags(
            dslContext = dslContext,
            projectIds = projectTagUpdateDTO.projectCodeList!!,
            routerTag = projectTagUpdateDTO.routerTag
        )
        executePool.submit {
            refreshRouterByProject(
                routerTag = projectTagUpdateDTO.routerTag,
                redisOperation = redisOperation,
                projectCodeIds = projectTagUpdateDTO.projectCodeList!!
            )
        }
        return Result(true)
    }

    fun updateTagByProject(
        projectCode: String,
        tag: String? = null
    ): Boolean {
        val routerTag = if (tag.isNullOrEmpty()) {
            autoTag
        } else {
            tag
        }
        if (autoTag.isNullOrEmpty()) {
            return true
        }
        logger.info("updateTagByProject: $projectCode| $routerTag")
        val projectTagUpdate = ProjectTagUpdateDTO(
            routerTag = routerTag!!,
            projectCodeList = arrayListOf(projectCode),
            bgId = null,
            centerId = null,
            deptId = null,
            channel = null
        )
        updateTagByProject(projectTagUpdate)
        return true
    }

    fun updateTagByOrg(
        projectTagUpdateDTO: ProjectTagUpdateDTO
    ): Result<Boolean> {
        logger.info("updateTagByOrg: $projectTagUpdateDTO")
        checkRouteTag(projectTagUpdateDTO.routerTag)
        checkOrg(projectTagUpdateDTO)
        projectTagDao.updateOrgTags(
            dslContext = dslContext,
            routerTag = projectTagUpdateDTO.routerTag,
            bgId = projectTagUpdateDTO.bgId,
            centerId = projectTagUpdateDTO.centerId,
            deptId = projectTagUpdateDTO.deptId
        )

        val projectCodes = projectDao.listByGroupId(
            dslContext = dslContext,
            bgId = projectTagUpdateDTO.bgId,
            centerId = projectTagUpdateDTO.centerId,
            deptId = projectTagUpdateDTO.deptId
        ).map { it.englishName }

        executePool.submit {
            refreshRouterByProject(
                routerTag = projectTagUpdateDTO.routerTag,
                redisOperation = redisOperation,
                projectCodeIds = projectCodes
            )
        }
        return Result(true)
    }

    fun updateTagByChannel(
        projectTagUpdateDTO: ProjectTagUpdateDTO
    ): Result<Boolean> {
        logger.info("updateTagByChannel: $projectTagUpdateDTO")
        checkRouteTag(projectTagUpdateDTO.routerTag)
        checkChannel(projectTagUpdateDTO.channel)
        projectTagDao.updateChannelTags(
            dslContext = dslContext,
            routerTag = projectTagUpdateDTO.routerTag,
            channel = projectTagUpdateDTO.channel!!
        )

        executePool.submit {
            refreshRouterByChannel(
                routerTag = projectTagUpdateDTO.routerTag,
                redisOperation = redisOperation,
                channel = projectTagUpdateDTO.channel!!,
                dslContext = dslContext
            )
        }
        return Result(true)
    }

    fun updateExtSystemRouterTag(extSystemTag: ProjectExtSystemTagDTO): Result<Boolean> {
        logger.info("updateTagByProject: $extSystemTag")
        checkRouteTag(extSystemTag.routerTag)
        checkProject(extSystemTag.projectCodeList)
        val projectInfos = projectTagDao.getExtSystemRouterTag(dslContext, extSystemTag.projectCodeList)
            ?: return Result(false)
        projectInfos.forEach {
            val extSystemRouter = it.otherRouterTags
            logger.info("project otherRouterTag ${it.otherRouterTags} ${it.englishName}")
            val newRouteMap = mutableMapOf<String, String>()
            // 如果有对应系统的router则替换，否则直接加
            if (extSystemRouter.isNullOrEmpty()) {
                newRouteMap[extSystemTag.system] = extSystemTag.routerTag
            } else {
                val routerMap = objectMapper.readValue<Map<String, String>>(extSystemRouter)
                newRouteMap.putAll(routerMap)
                newRouteMap[extSystemTag.system] = extSystemTag.routerTag
            }
            logger.info("setExtSystemRoute ${it.englishName} ${JsonUtil.toJson(newRouteMap)}")
            projectTagDao.updateExtSystemProjectTags(
                dslContext = dslContext,
                projectCode = it.englishName,
                routerTag = JsonUtil.toJson(newRouteMap)
            )
            if (extSystemTag.system == "codecc") {
                redisOperation.hset(PROJECT_TAG_CODECC_REDIS_KEY, it.englishName, extSystemTag.routerTag)
            } else if (extSystemTag.system == "repo") {
                redisOperation.hset(PROJECT_TAG_REPO_REDIS_KEY, it.englishName, extSystemTag.routerTag)
            }
        }
        return Result(true)
    }

    private fun checkProject(projectIds: List<String>?) {
        if (projectIds == null || projectIds.isEmpty()) {
            throw ParamBlankException("Invalid projectIds")
        }

        val projectInfos = projectDao.listByEnglishName(dslContext,
            projectIds,
            null,
            null,
            null
        ).map { it.englishName }
        if (projectIds.size > projectInfos.size) {
            val notExistProjectList = mutableListOf<String>()
            projectIds.forEach {
                if (!projectInfos.contains(it)) {
                    notExistProjectList.add(it)
                }
            }
            throw ParamBlankException("project $notExistProjectList not exist")
        }
    }

    private fun checkChannel(channel: String?) {
        if (channel == null || channel.isEmpty()) {
            throw ParamBlankException("Invalid projectIds")
        }
    }

    private fun checkOrg(projectTagUpdateDTO: ProjectTagUpdateDTO) {
        if (projectTagUpdateDTO.bgId == null &&
            projectTagUpdateDTO.deptId == null &&
            projectTagUpdateDTO.centerId == null
        ) {
            throw ParamBlankException("Invalid project org")
        }
    }

    fun refreshRouterByProject(
        routerTag: String,
        projectCodeIds: List<String>,
        redisOperation: RedisOperation
    ) {
        val watcher = Watcher("ProjectTagRefresh $routerTag")
        logger.info("ProjectTagRefresh start $routerTag $projectCodeIds")
        projectCodeIds.forEach { projectCode ->
            redisOperation.hset(PROJECT_TAG_REDIS_KEY, projectCode, routerTag)
        }
        logger.info("ProjectTagRefresh success. $routerTag ${projectCodeIds.size}")
        LogUtils.printCostTimeWE(watcher)
    }

    fun refreshRouterByChannel(
        routerTag: String,
        channel: String,
        redisOperation: RedisOperation,
        dslContext: DSLContext
    ) {
        try {
            var offset = 0
            val limit = 500
            do {
                val projectInfos = projectTagDao.listByChannel(dslContext, channel!!, limit, offset)
                projectInfos.forEach {
                    redisOperation.hset(PROJECT_TAG_REDIS_KEY, it.englishName, routerTag)
                }
                offset += limit
            } while (projectInfos.size == limit)
        } finally {
            logger.info("refreshRouterByChannel success")
        }
    }

    private fun checkRouteTag(routerTag: String) {
        if (routerTag.isNullOrBlank()) {
            throw ParamBlankException("routerTag error:empty routerTag")
        }
        if (!routerTagList.contains(routerTag)) {
            throw ParamBlankException("routerTag error:system unkown routerTag")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectTagService::class.java)
        const val PROJECT_TAG_CODECC_REDIS_KEY = "project:setting:tag:codecc:v2"
        const val PROJECT_TAG_REPO_REDIS_KEY = "project:setting:tag:repo:v2"
    }
}

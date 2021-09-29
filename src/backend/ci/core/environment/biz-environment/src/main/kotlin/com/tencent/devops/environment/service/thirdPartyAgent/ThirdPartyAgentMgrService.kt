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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.environment.agent.ThirdPartyAgentHeartbeatUtils
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.environment.client.InfluxdbClient
import com.tencent.devops.environment.client.UsageMetrics
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.EnvShareProjectDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdPartyAgent.AgentPipelineRefDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentEnableProjectsDao
import com.tencent.devops.environment.exception.AgentPermissionUnAuthorizedException
import com.tencent.devops.environment.model.AgentHostInfo
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentTask
import com.tencent.devops.environment.pojo.thirdPartyAgent.HeartbeatResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentAction
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentHeartbeatInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentLink
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStatusWithInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.UpdateAgentRequest
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.service.NodeWebsocketService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getAgentJarFile
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getFileMD5
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.ws.rs.NotFoundException

@Service
@Suppress("ALL")
class ThirdPartyAgentMgrService @Autowired(required = false) constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val thirdPartyAgentEnableProjectsDao: ThirdPartyAgentEnableProjectsDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val envDao: EnvDao,
    private val agentPipelineRefDao: AgentPipelineRefDao,
    @Autowired(required = false)
    private val agentDisconnectNotifyService: IAgentDisconnectNotifyService?,
    private val slaveGatewayService: SlaveGatewayService,
    private val thirdPartyAgentHeartbeatUtils: ThirdPartyAgentHeartbeatUtils,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val influxdbClient: InfluxdbClient,
    private val agentUrlService: AgentUrlService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val upgradeService: UpgradeService,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val websocketService: NodeWebsocketService,
    private val envShareProjectDao: EnvShareProjectDao
) {
    fun getAgentDetail(userId: String, projectId: String, nodeHashId: String): ThirdPartyAgentDetail? {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId, projectId)
            ?: return null
        val nodeRecord = nodeDao.get(
            dslContext,
            projectId,
            agentRecord.nodeId ?: return null
        ) ?: return null

        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        val nodeStringId = NodeStringIdUtils.getNodeStringId(nodeRecord)
        val displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, nodeRecord.displayName)
        val lastHeartbeatTime = thirdPartyAgentHeartbeatUtils.getHeartbeatTime(agentRecord.id, agentRecord.projectId)
        val parallelTaskCount = (agentRecord.parallelTaskCount ?: "").toString()
        val agentHostInfo = try {
            influxdbClient.queryHostInfo(agentHashId)
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            AgentHostInfo("0", "0", "0")
        }
        return ThirdPartyAgentDetail(
            agentId = HashUtil.encodeLongId(agentRecord.id),
            nodeId = nodeHashId,
            displayName = displayName,
            projectId = projectId,
            status = nodeRecord.nodeStatus,
            hostname = agentRecord.hostname,
            os = agentRecord.os,
            osName = agentRecord.detectOs,
            ip = agentRecord.ip,
            createdUser = nodeRecord.createdUser,
            createdTime = if (null == nodeRecord.createdTime) {
                ""
            } else {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(nodeRecord.createdTime)
            },
            agentVersion = agentRecord.masterVersion ?: "",
            slaveVersion = agentRecord.version ?: "",
            agentInstallPath = agentRecord.agentInstallPath ?: "",
            maxParallelTaskCount = MAX_PARALLEL_TASK_COUNT,
            parallelTaskCount = parallelTaskCount,
            startedUser = agentRecord.startedUser ?: "",
            agentUrl = agentUrlService.genAgentUrl(agentRecord),
            agentScript = agentUrlService.genAgentInstallScript(agentRecord),
            lastHeartbeatTime = if (null == lastHeartbeatTime) "" else DateTimeUtil.formatDate(Date(lastHeartbeatTime)),
            nCpus = agentHostInfo.nCpus,
            memTotal = agentHostInfo.memTotal,
            diskTotal = agentHostInfo.diskTotal,
            canEdit = environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.EDIT),
            currentAgentVersion = upgradeService.getAgentVersion(),
            currentWorkerVersion = upgradeService.getWorkerVersion()
        )
    }

    fun saveAgentEnv(userId: String, projectId: String, nodeHashId: String, envs: List<EnvVar>) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        checkEditPermmission(userId, projectId, nodeId)

        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId, projectId)
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )
        thirdPartyAgentDao.saveAgentEnvs(
            dslContext = dslContext,
            agentId = agentRecord.id,
            envStr = objectMapper.writeValueAsString(envs)
        )
    }

    fun getAgentEnv(projectId: String, nodeHashId: String): List<EnvVar> {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId, projectId)
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )

        return if (agentRecord.agentEnvs.isNullOrBlank()) {
            listOf()
        } else {
            objectMapper.readValue(agentRecord.agentEnvs)
        }
    }

    private fun checkEditPermmission(userId: String, projectId: String, nodeId: Long) {
        if (!environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = MessageCodeUtil.getCodeLanMessage(ERROR_NODE_NO_EDIT_PERMISSSION))
        }
    }

    fun setParallelTaskCount(userId: String, projectId: String, nodeHashId: String, parallelTaskCount: Int) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        checkEditPermmission(userId, projectId, nodeId)

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val agentRecord = thirdPartyAgentDao.getAgentByNodeId(context, nodeId, projectId)
                ?: throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                    params = arrayOf(nodeHashId)
                )
            agentRecord.parallelTaskCount = parallelTaskCount
            thirdPartyAgentDao.saveAgent(context, agentRecord)
        }
    }

    fun listAgentBuilds(
        user: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Page<AgentBuildDetail> {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = nodeId,
            projectId = projectId
        )
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        val agentBuildPage = client.get(ServiceAgentResource::class).listAgentBuild(
            agentId = agentHashId,
            page = page,
            pageSize = pageSize
        )

        val heartbeatInfo = thirdPartyAgentHeartbeatUtils.getNewHeartbeat(
            projectId = projectId,
            agentId = agentRecord.id
        )
        val agentTasks = heartbeatInfo?.taskList ?: listOf()
        val taskMap = agentTasks.associate { "${it.projectId}_${it.buildId}_${it.vmSeqId}" to AgentTask("RUNNING") }

        val agentBuildDetails = agentBuildPage.records.map {
            AgentBuildDetail(
                nodeId = nodeHashId,
                agentId = agentHashId,
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                pipelineName = it.pipelineName,
                buildId = it.buildId,
                buildNumber = it.buildNum,
                vmSetId = it.vmSeqId,
                taskName = it.taskName,
                status = it.status,
                createdTime = it.createdTime,
                updatedTime = it.updatedTime,
                workspace = it.workspace,
                agentTask = taskMap["${it.projectId}_${it.buildId}_${it.vmSeqId}"]
            )
        }

        return Page(
            count = agentBuildPage.count,
            page = agentBuildPage.page,
            pageSize = agentBuildPage.pageSize,
            totalPages = agentBuildPage.totalPages,
            records = agentBuildDetails
        )
    }

    fun listAgentActions(
        user: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Page<ThirdPartyAgentAction> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 100
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 100

        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = nodeId,
            projectId = projectId
        )
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)

        val agentActionCount = thirdPartyAgentDao.getAgentActionsCount(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentRecord.id
        )
        val agentActions =
            thirdPartyAgentDao.listAgentActions(
                dslContext = dslContext,
                projectId = projectId,
                agentId = agentRecord.id,
                offset = offset,
                limit = limit
            ).map {
                ThirdPartyAgentAction(
                    agentId = agentHashId,
                    projectId = it.projectId,
                    action = it.action,
                    actionTime = it.actionTime.timestamp()
                )
            }
        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = agentActionCount, records = agentActions)
    }

    fun queryCpuUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.CPU, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryMemoryUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")
        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.MEMORY, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryDiskioMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.DISK, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun queryNetMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Map<String, List<Map<String, Any>>> {
        val id = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = id,
            projectId = projectId
        ) ?: throw NotFoundException("The agent is not exist")

        return try {
            UsageMetrics.loadMetricsBean(UsageMetrics.MetricsType.NET, OS.valueOf(agentRecord.os))
                ?.loadQuery(
                    agentHashId = HashUtil.encodeLongId(agentRecord.id),
                    timeRange = timeRange
                ) ?: emptyMap()
        } catch (e: Throwable) {
            logger.warn("influx query error: ", e)
            emptyMap()
        }
    }

    fun generateAgent(
        userId: String,
        projectId: String,
        os: OS,
        zoneName: String?
    ): ThirdPartyAgentLink {
        val gateway = slaveGatewayService.getGateway(zoneName)
        val fileGateway = slaveGatewayService.getFileGateway(zoneName)
        logger.info("Generate agent($os) info of project($projectId) with gateway $gateway by user($userId)")
        val unimportAgent = thirdPartyAgentDao.listUnimportAgent(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            os = os
        )
        val agentRecord: TEnvironmentThirdpartyAgentRecord = if (unimportAgent.isEmpty()) {
            val secretKey = generateSecretKey()
            val id = thirdPartyAgentDao.add(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                os = os,
                secretKey = SecurityUtil.encrypt(secretKey),
                gateway = gateway,
                fileGateway = fileGateway
            )
            thirdPartyAgentDao.getAgent(dslContext, id)!!
        } else {
            val agentRecord = unimportAgent[0]
            logger.info("The agent(${agentRecord.id}) exist")
            if (!gateway.isNullOrBlank()) {
                thirdPartyAgentDao.updateGateway(
                    dslContext = dslContext,
                    agentId = agentRecord.id,
                    gateway = gateway!!,
                    fileGateway = fileGateway
                )
            }
            agentRecord.setGateway(gateway!!)
        }

        val agentHashId = HashUtil.encodeLongId(agentRecord.id)

        if (os == OS.WINDOWS) {
            return ThirdPartyAgentLink(
                agentId = agentHashId,
                link = agentUrlService.genAgentUrl(agentRecord)
            )
        }
        return ThirdPartyAgentLink(
            agentId = agentHashId,
            link = agentUrlService.genAgentInstallScript(agentRecord)
        )
    }

    fun getAgentLink(
        userId: String,
        projectId: String,
        nodeId: String
    ): ThirdPartyAgentLink {
        val id = HashUtil.decodeIdToLong(nodeId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, id, projectId)
            ?: throw NotFoundException("The agent is not exist")
        val url = agentUrlService.genAgentInstallUrl(agentRecord)
        val agentId = HashUtil.encodeLongId(agentRecord.id)
        return ThirdPartyAgentLink(
            agentId = agentId,
            link = "curl $url | bash"
        )
    }

    fun listAgents(
        userId: String,
        projectId: String,
        os: OS?
    ): List<ThirdPartyAgentInfo> {
        val agents = thirdPartyAgentDao.listImportAgent(dslContext = dslContext, projectId = projectId, os = os)
        if (agents.isEmpty()) {
            return arrayListOf()
        }

        val nodeIds = agents.filter { it.nodeId != null }.map { it.nodeId }
        val nodes = nodeDao.listByIds(dslContext, projectId, nodeIds)
        if (nodes.isEmpty()) {
            return emptyList()
        }
        val nodeMap = nodes.map { it.nodeId to it }.toMap()

        val canUseNodeIds = environmentPermissionService.listNodeByPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.USE
        )

        if (canUseNodeIds.isEmpty()) {
            return emptyList()
        }

        logger.info("Get the user can use node ids $canUseNodeIds")

        val agentInfo = ArrayList<ThirdPartyAgentInfo>()

        agents.forEach { agent ->
            if (agent.nodeId == null) {
                logger.warn("The agent(${agent.id}) node id is empty")
                return@forEach
            }

            if (!canUseNodeIds.contains(agent.nodeId)) {
                return@forEach
            }
            val node = nodeMap[agent.nodeId]
            if (node == null || node.nodeStatus.isNullOrBlank()) {
                logger.warn("Fail to find the node status of agent(${agent.id})")
                return@forEach
            }

            agentInfo.add(
                ThirdPartyAgentInfo(
                    agentId = HashUtil.encodeLongId(agent.id),
                    projectId = projectId,
                    status = NodeStatus.valueOf(node.nodeStatus!!).statusName,
                    hostname = agent.hostname,
                    ip = agent.ip,
                    displayName = node.displayName,
                    detailName = "${node.displayName}/${agent.ip}/${agent.hostname}/${node.osName}"
                )
            )
        }
        return agentInfo
    }

    fun getAgentByDisplayName(projectId: String, displayName: String): AgentResult<ThirdPartyAgent?> {
        logger.info("[$projectId|$displayName] Get the agent")
        val nodes = nodeDao.getByDisplayName(
            dslContext = dslContext,
            projectId = projectId,
            displayName = displayName,
            nodeType = listOf(NodeType.THIRDPARTY.name, NodeType.DEVCLOUD.name)
        )
        if (nodes.isEmpty()) {
            return AgentResult(0, null, null, null)
        }
        if (nodes.size != 1) {
            logger.warn("[$projectId|$displayName] There are more than one node with the display name - ($nodes)")
            return AgentResult(0, null, null, null)
        }
        val node = nodes[0]
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, node.nodeId, projectId)
        if (agentRecord == null) {
            logger.warn("[$projectId|$displayName|${node.nodeId}] Fail to get the agent")
            return AgentResult(0, null, null, null)
        }
        val status = AgentStatus.fromStatus(agentRecord.status)
        return AgentResult(
            status = status,
            data = ThirdPartyAgent(
                agentId = HashUtil.encodeLongId(agentRecord.id),
                projectId = projectId,
                nodeId = HashUtil.encodeLongId(node.nodeId),
                status = status,
                hostname = agentRecord.hostname,
                os = agentRecord.os,
                ip = agentRecord.ip,
                secretKey = SecurityUtil.decrypt(agentRecord.secretKey),
                createUser = agentRecord.createdUser,
                createTime = agentRecord.createdTime.timestamp(),
                parallelTaskCount = agentRecord.parallelTaskCount
            )
        )
    }

    fun getAgent(
        projectId: String,
        agentId: String
    ): AgentResult<ThirdPartyAgent?> {
        logger.info("Get the agent($agentId) of project($projectId)")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id, projectId = projectId)
            ?: return AgentResult(AgentStatus.DELETE, null)
        val status = AgentStatus.fromStatus(agentRecord.status)
        val nodeId = if (agentRecord.nodeId != null) {
            HashUtil.encodeLongId(agentRecord.nodeId)
        } else {
            null
        }
        return AgentResult(
            status = status,
            data = ThirdPartyAgent(
                agentId = agentId,
                projectId = projectId,
                nodeId = nodeId,
                status = status,
                hostname = agentRecord.hostname,
                os = agentRecord.os,
                ip = agentRecord.ip,
                secretKey = SecurityUtil.decrypt(agentRecord.secretKey),
                createUser = agentRecord.createdUser,
                createTime = agentRecord.createdTime.timestamp(),
                parallelTaskCount = agentRecord.parallelTaskCount
            )
        )
    }

    fun getAgnetByEnvName(projectId: String, envName: String): List<ThirdPartyAgent> {
        logger.info("[$projectId|$envName] Get the agents by env name")
        // get shared project first
        val sharedThridPartyAgentList = getSharedThirdPartyAgentList(projectId, envName)

        val envRecord = envDao.getByEnvName(dslContext = dslContext, projectId = projectId, envName = envName)
        if (envRecord == null && sharedThridPartyAgentList.isEmpty()) {
            logger.warn("[$projectId|$envName] The env is not exist")
            return emptyList()
        }

        return (if (envRecord != null) {
            getAgentByEnvId(projectId = projectId, envHashId = HashUtil.encodeLongId(envRecord.envId))
        } else {
            emptyList()
        }).plus(sharedThridPartyAgentList)
    }

    private fun getSharedThirdPartyAgentList(projectId: String, projectEnvName: String): List<ThirdPartyAgent> {
        val sharedProjEnv = projectEnvName.split("@") // sharedProjId@poolName
        if (sharedProjEnv.size != 2 || sharedProjEnv[0].isNullOrBlank() || sharedProjEnv[1].isNullOrBlank()) {
            return emptyList()
        }
        val sharedProjectId = sharedProjEnv[0]
        val sharedEnvName = sharedProjEnv[1]
        val sharedEnvRecord = envShareProjectDao.get(dslContext, sharedEnvName, projectId, sharedProjectId)
        if (sharedEnvRecord.isEmpty()) {
            logger.info("env name not exists, envName: $sharedEnvName, projectId：$projectId, " +
                "mainProjectId: $sharedProjectId")
            return emptyList()
        }
        logger.info("sharedEnvRecord size: ${sharedEnvRecord.size}")
        val sharedThirdPartyAgents = mutableListOf<ThirdPartyAgent>()
        sharedEnvRecord.forEach {
            val envRecord = envDao.getByEnvName(dslContext, it.mainProjectId, sharedEnvName) ?: return@forEach
            sharedThirdPartyAgents.addAll(getAgentByEnvId(it.mainProjectId, HashUtil.encodeLongId(envRecord.envId)))
        }
        logger.info("sharedThirdPartyAgents size: ${sharedThirdPartyAgents.size}")
        return sharedThirdPartyAgents
    }

    fun getAgentByEnvId(projectId: String, envHashId: String): List<ThirdPartyAgent> {
        logger.info("[$projectId|$envHashId] Get the agents by envId")
        val envId = HashUtil.decodeIdToLong(envHashId)
        val nodes = envNodeDao.list(dslContext = dslContext, projectId = projectId, envIds = listOf(envId))
        if (nodes.isEmpty()) {
            return emptyList()
        }
        val nodeIds = nodes.map {
            it.nodeId
        }.toSet()
        val agents = thirdPartyAgentDao.getAgentsByNodeIds(
            dslContext = dslContext,
            nodeIds = nodeIds,
            projectId = projectId
        )
        if (agents.isEmpty()) {
            return emptyList()
        }
        return agents.map {
            val nodeId = if (it.nodeId != null) {
                HashUtil.encodeLongId(it.nodeId)
            } else {
                null
            }
            ThirdPartyAgent(
                agentId = HashUtil.encodeLongId(it.id),
                projectId = projectId,
                nodeId = nodeId,
                status = AgentStatus.fromStatus(it.status),
                hostname = it.hostname,
                os = it.os,
                ip = it.ip,
                secretKey = SecurityUtil.decrypt(it.secretKey),
                createUser = it.createdUser,
                createTime = it.createdTime.timestamp(),
                parallelTaskCount = it.parallelTaskCount
            )
        }
    }

    fun checkIfCanUpgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        tag: String
    ): AgentResult<Boolean> {
        logger.info("Checking if the agent($agentId) of project($projectId) can upgrade")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id, projectId = projectId)
            ?: return AgentResult(AgentStatus.DELETE, false)
        val status = AgentStatus.fromStatus(agentRecord.status)
        if (status != AgentStatus.IMPORT_OK) {
            return AgentResult(status, false)
        }

        val key = SecurityUtil.decrypt(agentRecord.secretKey)

        if (key != secretKey) {
            logger.warn("The agent($id) of project($projectId)'s secret($secretKey) is not match the expect one($key)")
            return AgentResult(AgentStatus.DELETE, false)
        }

        val jarFile = getAgentJarFile()
        val md5 = getFileMD5(jarFile)
        if (md5 == tag) {
            return AgentResult(status, false)
        }
        logger.info("The agent($id) can upgrade")
        return AgentResult(status, true)
    }

    fun deleteAgent(
        userId: String,
        projectId: String,
        nodeId: String
    ) {
        logger.info("Delete the node($nodeId) of project($projectId) by user($userId)")
        val id = HashUtil.decodeIdToLong(nodeId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val record = thirdPartyAgentDao.getAgentByNodeId(dslContext = context, nodeId = id, projectId = projectId)
            if (record == null) {
                logger.warn("The node($nodeId) is not exist")
                throw NotFoundException("The node is not exist")
            }
            val count = thirdPartyAgentDao.updateStatus(
                dslContext = context,
                id = record.id,
                nodeId = null,
                projectId = projectId,
                status = AgentStatus.DELETE
            )
            if (count != 1) {
                logger.warn("Can't delete the agent($count)")
            }

            nodeDao.updateNodeStatus(dslContext = context, id = id, status = NodeStatus.DELETED)
            if (record.nodeId != null) {
                environmentPermissionService.deleteNode(projectId = projectId, nodeId = record.nodeId)
            }
            webSocketDispatcher.dispatch(
                websocketService.buildDetailMessage(projectId, userId)
            )
        }
    }

    fun getAgentStatusWithInfo(
        userId: String,
        projectId: String,
        agentId: String
    ): ThirdPartyAgentStatusWithInfo {
        val record = thirdPartyAgentDao.getAgent(
            dslContext,
            HashUtil.decodeIdToLong(agentId), projectId
        ) ?: throw NotFoundException("The agent($agentId) is not exist")
        return ThirdPartyAgentStatusWithInfo(
            status = AgentStatus.fromStatus(record.status),
            hostname = record.hostname ?: "",
            ip = record.ip ?: "",
            os = record.detectOs ?: ""
        )
    }

    fun importAgent(
        userId: String,
        projectId: String,
        agentId: String
    ) {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
            ?: throw NotFoundException("The agent($agentId) is not exist")
        if (agentRecord.status == AgentStatus.IMPORT_EXCEPTION.status ||
            agentRecord.status == AgentStatus.UN_IMPORT.status
        ) {
            logger.warn("The agent status(${agentRecord.status}) is NOT OK")
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_AGENT_STATUS_EXCEPTION)
        }

        if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
            throw PermissionForbiddenException(
                message = MessageCodeUtil.getCodeLanMessage(ERROR_NODE_NO_CREATE_PERMISSSION))
        }

        val nodeInfo = nodeDao.listDevCloudNodesByIps(dslContext, projectId, listOf(agentRecord.ip))
        if (nodeInfo.isNotEmpty()) {
            logger.info("Import dev cloud agent, refresh the node status to normal")
            nodeInfo[0].nodeStatus = NodeStatus.NORMAL.name
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                nodeDao.updateNode(context, nodeInfo[0])
                thirdPartyAgentDao.updateStatus(
                    dslContext = context,
                    id = id,
                    nodeId = nodeInfo[0].nodeId,
                    projectId = projectId,
                    status = AgentStatus.IMPORT_OK
                )
                // 不用再写入auth了，因为已经存在了

                webSocketDispatcher.dispatch(
                    websocketService.buildDetailMessage(projectId, userId)
                )
            }
            return
        }

        logger.info("Trying to import the agent($agentId) of project($projectId) by user($userId)")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            val nodeId = nodeDao.addNode(
                dslContext = context,
                projectId = projectId,
                ip = agentRecord.ip,
                name = agentRecord.hostname,
                osName = agentRecord.os.toLowerCase(),
                status = NodeStatus.NORMAL,
                type = NodeType.THIRDPARTY,
                userId = userId
            )

            val maxNodeRecord = nodeDao.getMaxNodeStringId(context, projectId, nodeId)

            val maxNodeRecordId = if (maxNodeRecord == null) {
                0
            } else {
                val nodeStringId = maxNodeRecord.nodeStringId
                if (nodeStringId == null) {
                    0
                } else {
                    val split = nodeStringId.split("_")
                    if (split.size < 3) {
                        logger.warn("Unknown node string id format($nodeStringId)")
                        0
                    } else {
                        split[2].toInt()
                    }
                }
            }

            val nodeStringId = "BUILD_${HashUtil.encodeLongId(nodeId)}_${maxNodeRecordId + 1}"
            nodeDao.insertNodeStringIdAndDisplayName(
                dslContext = context,
                id = nodeId,
                nodeStringId = nodeStringId,
                displayName = nodeStringId,
                userId = userId
            )

            val count = thirdPartyAgentDao.updateStatus(context, id, nodeId, projectId, AgentStatus.IMPORT_OK)
            if (count != 1) {
                logger.warn("Fail to update the agent($id) to OK status")
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                    params = arrayOf(id.toString())
                )
            }
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = nodeId,
                nodeName = "$nodeStringId(${agentRecord.ip})"
            )
            webSocketDispatcher.dispatch(
                websocketService.buildDetailMessage(projectId, userId)
            )
        }
    }

    /**
     * API FROM AGENT
     */
    fun agentStartup(
        projectId: String,
        agentId: String,
        secretKey: String,
        startInfo: ThirdPartyAgentStartInfo
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        logger.info("The agent($id) is start up by ${startInfo.hostIp}")
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId) ?: return AgentStatus.DELETE

        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }

        var status = AgentStatus.fromStatus(agentRecord.status)
        if (status == AgentStatus.UN_IMPORT) {
            status = AgentStatus.UN_IMPORT_OK
        } else if (status == AgentStatus.IMPORT_EXCEPTION) {
            status = AgentStatus.IMPORT_OK
        }
        if (!(AgentStatus.isImportException(status) ||
                AgentStatus.isUnImport(status) ||
                agentRecord.startRemoteIp.isNullOrBlank())) {
            if (startInfo.hostIp != agentRecord.startRemoteIp) {
                return AgentStatus.DELETE
            }
        }
        val updateCount = thirdPartyAgentDao.updateAgentInfo(
            dslContext = dslContext,
            id = id,
            remoteIp = startInfo.hostIp,
            projectId = projectId,
            hostname = startInfo.hostname,
            ip = startInfo.hostIp,
            detectOS = startInfo.detectOS,
            agentVersion = startInfo.version,
            masterVersion = startInfo.masterVersion
        )
        if (updateCount != 1) {
            logger.warn("Fail to update the agent info($updateCount)")
        }

        if (agentRecord.status == AgentStatus.IMPORT_EXCEPTION.status ||
            agentRecord.status == AgentStatus.UN_IMPORT.status) {
            thirdPartyAgentDao.addAgentAction(dslContext, projectId, id, AgentAction.ONLINE.name)
        }

        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            if (agentRecord.nodeId != null) {
                val nodeRecord = nodeDao.get(context, projectId, agentRecord.nodeId)
                if (nodeRecord != null && (nodeRecord.nodeIp != startInfo.hostIp ||
                        nodeRecord.nodeStatus == NodeStatus.ABNORMAL.name)) {
                    nodeRecord.nodeStatus = NodeStatus.NORMAL.name
                    nodeRecord.nodeIp = startInfo.hostIp
                    nodeDao.saveNode(context, nodeRecord)
                    webSocketDispatcher.dispatch(
                        websocketService.buildDetailMessage(projectId, "")
                    )
                }
            }
        }

        return status
    }

    fun agentShutdown(
        projectId: String,
        agentId: String,
        secretKey: String,
        shutdownNormal: Boolean
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        logger.info("The agent($id) shutdown($shutdownNormal)")
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id, projectId = projectId)
            ?: return AgentStatus.DELETE

        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }

        return AgentStatus.fromStatus(agentRecord.status)
    }

    fun getAgentStatus(
        projectId: String,
        agentId: String,
        secretKey: String
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id, projectId = projectId)
            ?: return AgentStatus.DELETE
        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }
        return AgentStatus.fromStatus(agentRecord.status)
    }

    fun newHeartbeat(
        projectId: String,
        agentHashId: String,
        secretKey: String,
        newHeartbeatInfo: NewHeartbeatInfo
    ): HeartbeatResponse {

        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val agentRecord = getAgentRecord(
                context = context,
                projectId = projectId,
                agentId = agentHashId,
                secretKey = secretKey
            )

            if (agentRecord == null) {
                logger.warn("The agent($agentHashId) is not exist")
                return@transactionResult HeartbeatResponse(
                    masterVersion = "",
                    slaveVersion = "",
                    AgentStatus = AgentStatus.DELETE.name,
                    ParallelTaskCount = -1,
                    envs = mapOf()
                )
            }

            var agentChanged = false
            if (newHeartbeatInfo.masterVersion != agentRecord.masterVersion) {
                agentRecord.masterVersion = newHeartbeatInfo.masterVersion
                agentChanged = true
            }
            if (newHeartbeatInfo.slaveVersion != agentRecord.version) {
                var slaveVersion = newHeartbeatInfo.slaveVersion
                if (slaveVersion.length > 128) {
                    slaveVersion = slaveVersion.substring(0, 127)
                }
                agentRecord.version = slaveVersion
                agentChanged = true
            }
            if (newHeartbeatInfo.agentIp != agentRecord.ip) {
                agentRecord.ip = newHeartbeatInfo.agentIp
                agentChanged = true
            }
            if (newHeartbeatInfo.hostName != agentRecord.hostname) {
                agentRecord.hostname = newHeartbeatInfo.hostName
                agentChanged = true
            }
            if (agentRecord.parallelTaskCount == null) {
                agentRecord.parallelTaskCount = newHeartbeatInfo.parallelTaskCount
                agentChanged = true
            }
            if (newHeartbeatInfo.agentInstallPath != agentRecord.agentInstallPath) {
                agentRecord.agentInstallPath = newHeartbeatInfo.agentInstallPath
                agentChanged = true
            }
            if (newHeartbeatInfo.startedUser != agentRecord.startedUser) {
                agentRecord.startedUser = newHeartbeatInfo.startedUser
                agentChanged = true
            }
            if (agentChanged) {
                thirdPartyAgentDao.saveAgent(context, agentRecord)
            }

            val status = AgentStatus.fromStatus(agentRecord.status)
            val agentStatus = when (status) {
                AgentStatus.UN_IMPORT -> {
                    logger.info("update the agent($agentHashId) status to un-import ok")
                    thirdPartyAgentDao.updateStatus(
                        dslContext = context,
                        id = agentRecord.id,
                        nodeId = null,
                        projectId = projectId,
                        status = AgentStatus.UN_IMPORT_OK
                    )
                    AgentStatus.UN_IMPORT_OK
                }
                AgentStatus.UN_IMPORT_OK -> {
                    AgentStatus.UN_IMPORT_OK
                }
                else /* AgentStatus.IMPORT_OK || AgentStatus.IMPORT_EXCEPTION */ -> {
                    if (agentRecord.status == AgentStatus.IMPORT_EXCEPTION.status) {
                        logger.info("update agent($agentHashId) status from exception to ok")
                        thirdPartyAgentDao.updateStatus(
                            dslContext = context,
                            id = agentRecord.id,
                            nodeId = null,
                            projectId = projectId,
                            status = AgentStatus.IMPORT_OK
                        )
                        thirdPartyAgentDao.addAgentAction(
                            dslContext = context,
                            projectId = projectId,
                            agentId = agentRecord.id,
                            action = AgentAction.ONLINE.name
                        )
                    }
                    if (agentRecord.nodeId != null) {
                        val nodeRecord = nodeDao.get(
                            dslContext = context,
                            projectId = projectId,
                            nodeId = agentRecord.nodeId
                        )
                        if (nodeRecord == null) {
                            logger.warn("node not exist")
                            return@transactionResult HeartbeatResponse(
                                masterVersion = "",
                                slaveVersion = "",
                                AgentStatus = AgentStatus.DELETE.name,
                                ParallelTaskCount = -1,
                                envs = mapOf()
                            )
                        }
                        if (nodeRecord.nodeIp != newHeartbeatInfo.agentIp ||
                            nodeRecord.nodeStatus == NodeStatus.ABNORMAL.name) {
                            nodeRecord.nodeStatus = NodeStatus.NORMAL.name
                            nodeRecord.nodeIp = newHeartbeatInfo.agentIp
                            nodeDao.saveNode(dslContext = context, nodeRecord = nodeRecord)
                            webSocketDispatcher.dispatch(
                                websocketService.buildDetailMessage(projectId, "")
                            )
                        }
                    }
                    AgentStatus.IMPORT_OK
                }
            }

            val agentId = HashUtil.decodeIdToLong(agentHashId)
            thirdPartyAgentHeartbeatUtils.saveNewHeartbeat(projectId, agentId, newHeartbeatInfo)
            thirdPartyAgentHeartbeatUtils.heartbeat(projectId, agentHashId)

            HeartbeatResponse(
                masterVersion = upgradeService.getAgentVersion(),
                slaveVersion = upgradeService.getWorkerVersion(),
                AgentStatus = agentStatus.name,
                ParallelTaskCount = agentRecord.parallelTaskCount,
                envs = if (agentRecord.agentEnvs.isNullOrBlank()) {
                    mapOf()
                } else {
                    val envVar: List<EnvVar> = objectMapper.readValue(agentRecord.agentEnvs)
                    envVar.associate { it.name to it.value }
                },
                gateway = agentRecord.gateway,
                fileGateway = agentRecord.fileGateway
            )
        }
    }

    fun heartBeat(
        projectId: String,
        agentId: String,
        secretKey: String,
        heartbeatInfo: ThirdPartyAgentHeartbeatInfo?
    ): AgentStatus {
        logger.info("Agent($agentId) of project($projectId) heartbeat")
        val slaveVersion = if (heartbeatInfo != null) {
            heartbeatInfo.slaveVersion ?: ""
        } else {
            ""
        }
        val masterVersion = if (heartbeatInfo != null) {
            heartbeatInfo.masterVersion ?: ""
        } else {
            ""
        }
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val agentRecord = getAgentRecord(context, projectId, agentId, secretKey)

            if (agentRecord == null) {
                logger.warn("The agent($agentId) is not exist")
                return@transactionResult AgentStatus.DELETE
            }

            logger.info("agent ver: ${agentRecord.masterVersion}|$masterVersion|${agentRecord.version}|$slaveVersion")

            // 心跳上报版本号，且版本跟数据库对不上时，更新数据库
            if (slaveVersion.isNotBlank() && masterVersion.isNotBlank()) {
                if (agentRecord.version != slaveVersion || agentRecord.masterVersion != masterVersion) {
                    thirdPartyAgentDao.updateAgentVersion(
                        dslContext = context,
                        id = agentRecord.id,
                        projectId = projectId,
                        version = slaveVersion,
                        masterVersion = masterVersion
                    )
                }
            }

            val status = AgentStatus.fromStatus(agentRecord.status)
            logger.info("Get the agent($agentId) status $status")

            val agentStatus = when {
                AgentStatus.isUnImport(status) -> {
                    logger.info("Update the agent($agentId) to un-import ok")
                    thirdPartyAgentDao.updateStatus(context, agentRecord.id, null, projectId, AgentStatus.UN_IMPORT_OK)
                    AgentStatus.UN_IMPORT_OK
                }
                AgentStatus.isImportException(status) -> {
                    logger.info("Update the agent($agentId) from exception to ok")
                    agentDisconnectNotifyService?.online(
                        projectId = agentRecord.projectId ?: "",
                        ip = agentRecord.ip ?: "",
                        hostname = agentRecord.hostname ?: "",
                        createUser = agentRecord.createdUser ?: "",
                        os = agentRecord.os ?: ""
                    )
                    thirdPartyAgentDao.updateStatus(context, agentRecord.id, null, projectId, AgentStatus.IMPORT_OK)
                    thirdPartyAgentDao.addAgentAction(context, projectId, agentRecord.id, AgentAction.ONLINE.name)
                    if (agentRecord.nodeId != null) {
                        nodeDao.updateNodeStatus(context, agentRecord.nodeId, NodeStatus.NORMAL)
                        webSocketDispatcher.dispatch(
                            websocketService.buildDetailMessage(projectId, "")
                        )
                    }
                    AgentStatus.IMPORT_OK
                }
                else -> {
                    logger.info("Get the node id(${agentRecord.nodeId}) of agent($agentId)")
                    if (agentRecord.nodeId != null) {
                        val nodeRecord = nodeDao.get(context, projectId, agentRecord.nodeId)
                        if (nodeRecord == null) {
                            logger.warn("The node is not exist")
                            return@transactionResult AgentStatus.DELETE
                        }
                        if (nodeRecord.nodeStatus == NodeStatus.ABNORMAL.name) {
                            val count = nodeDao.updateNodeStatus(context, agentRecord.nodeId, NodeStatus.NORMAL)
                            agentDisconnectNotifyService?.online(
                                projectId = agentRecord.projectId ?: "",
                                ip = agentRecord.ip ?: "",
                                hostname = agentRecord.hostname ?: "",
                                createUser = agentRecord.createdUser ?: "",
                                os = agentRecord.os ?: ""
                            )
                            webSocketDispatcher.dispatch(
                                websocketService.buildDetailMessage(projectId, "")
                            )
                            logger.info("Update the node status - $count of agent $agentId")
                        }
                    }
                    status
                }
            }

            if (status == AgentStatus.IMPORT_OK) {
                // Check if exist in node table
                if (agentRecord.nodeId == null) {
                    return@transactionResult AgentStatus.DELETE
                }
                val nodeRecord = nodeDao.get(context, projectId, agentRecord.nodeId)
                    ?: return@transactionResult AgentStatus.DELETE
                if (nodeRecord.nodeStatus == NodeStatus.DELETED.name) {
                    return@transactionResult AgentStatus.DELETE
                }
            }

            thirdPartyAgentHeartbeatUtils.heartbeat(projectId, agentId)
            agentStatus
        }
    }

    fun getOs(userId: String, projectId: String, agentId: String): String {
        return thirdPartyAgentDao.getAgent(dslContext, HashUtil.decodeIdToLong(agentId), projectId)?.os ?: "LINUX"
    }

    fun enableThirdPartyAgent(projectId: String, enable: Boolean) =
        thirdPartyAgentEnableProjectsDao.enable(dslContext, projectId, enable)

    fun listEnableThirdPartyAgentProjects() =
        thirdPartyAgentEnableProjectsDao.list(dslContext)
            .filter { ByteUtils.byte2Bool(it.enalbe) }
            .map {
                it.projectId
            }

    private fun getAgentRecord(
        context: DSLContext,
        projectId: String,
        agentId: String,
        secretKey: String
    ): TEnvironmentThirdpartyAgentRecord? {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord =
            thirdPartyAgentDao.getAgent(dslContext = context, id = id, projectId = projectId) ?: return null
        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }
        return agentRecord
    }

    fun updateAgentGateway(updateAgentRequest: UpdateAgentRequest) {
        with(updateAgentRequest) {
            thirdPartyAgentDao.updateGateway(
                dslContext = dslContext,
                agentId = HashUtil.decodeIdToLong(agentId),
                gateway = gateway,
                fileGateway = fileGateway
            )
        }
    }

    fun generateSecretKey() = ApiUtil.randomSecretKey()

    fun agentTaskStarted(projectId: String, pipelineId: String, buildId: String, vmSeqId: String, agentId: String) {
        val agentLongId = HashUtil.decodeIdToLong(agentId)
        val agent = thirdPartyAgentDao.getAgent(dslContext, agentLongId)
        if (agent == null) {
            logger.warn("agent no found")
            return
        }
        val now = LocalDateTime.now()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.updateLastBuildTime(context, agent.nodeId, now)
            agentPipelineRefDao.updateLastBuildTime(context, projectId, pipelineId, vmSeqId, agentLongId, now)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentMgrService::class.java)
        private const val MAX_PARALLEL_TASK_COUNT = "10"
    }
}

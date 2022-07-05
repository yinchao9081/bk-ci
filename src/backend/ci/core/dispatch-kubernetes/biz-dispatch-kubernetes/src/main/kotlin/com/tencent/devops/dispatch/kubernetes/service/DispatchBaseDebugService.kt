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
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchDebugResponse
import com.tencent.devops.dispatch.kubernetes.utils.RedisUtils
import com.tencent.devops.dispatch.common.common.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.common.common.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.kubernetes.dao.DispatchKubernetesBuildDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchKubernetesBuildHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchEnumType
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugOperateBuilderType
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugTaskStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DispatchBaseDebugService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchFactory: DispatchTypeServiceFactory,
    private val dispatchKubernetesBuildDao: DispatchKubernetesBuildDao,
    private val dispatchKubernetesBuildHisDao: DispatchKubernetesBuildHisDao,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val redisUtils: RedisUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchBaseDebugService::class.java)
    }

    fun startDebug(
        userId: String,
        dispatchType: DispatchEnumType,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        needCheckPermission: Boolean = true
    ): DispatchDebugResponse {
        logger.info("$userId start debug $dispatchType pipelineId: $pipelineId buildId: $buildId vmSeqId: $vmSeqId")
        // 根据是否传入buildId 查找builderName
        val buildHistory = if (buildId == null) {
            // 查找当前pipeline下的最近一次构建
            dispatchKubernetesBuildHisDao.getLatestBuildHistory(dslContext, dispatchType.value, pipelineId, vmSeqId)
        } else {
            // 精确查找
            dispatchKubernetesBuildHisDao.get(dslContext, dispatchType.value, buildId, vmSeqId)[0]
        }

        val builderName = if (buildHistory != null) {
            buildHistory.containerName
        } else {
            throw ErrorCodeException(
                errorCode = "2103501",
                defaultMessage = "no container is ready to debug",
                params = arrayOf(pipelineId)
            )
        }

        // 检验权限
        if (needCheckPermission) {
            checkPermission(dispatchType, userId, pipelineId, builderName, vmSeqId)
        }

        // 查看当前容器的状态
        val statusResponse = dispatchFactory.load(dispatchType).getDebugBuilderStatus(
            buildId = buildId ?: "",
            vmSeqId = vmSeqId,
            userId = userId,
            builderName = builderName
        )
        if (statusResponse.isOk()) {
            val status = statusResponse.data!!

            when (status) {
                DispatchBuilderDebugStatus.CAN_RESTART -> {
                    // 出于关机状态，开机
                    logger.info("Update container status stop to running, builderName: $builderName")
                    startSleepContainer(
                        dispatchType = dispatchType,
                        userId = userId,
                        projectId = buildHistory.projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        builderName = builderName
                    )
                    dispatchKubernetesBuildDao.updateDebugStatus(
                        dslContext = dslContext,
                        dispatchType = dispatchType.value,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        builderName = builderName,
                        debugStatus = true
                    )
                }
                DispatchBuilderDebugStatus.RUNNING -> {
                    dispatchKubernetesBuildDao.updateDebugStatus(
                        dslContext = dslContext,
                        dispatchType = dispatchType.value,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        builderName = builderName,
                        debugStatus = true
                    )
                }
                DispatchBuilderDebugStatus.STARTING -> {
                    // 容器正在启动中，等待启动成功
                    val status = dispatchFactory.load(dispatchType).waitDebugBuilderRunning(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId ?: "",
                        vmSeqId = vmSeqId,
                        userId = userId,
                        builderName = builderName
                    )
                    if (status != DispatchBuilderDebugStatus.RUNNING) {
                        logger.error("Status exception, builderName: $builderName, status: $status")
                        throw ErrorCodeException(
                            errorCode = "2103502",
                            defaultMessage = "Status exception, please try rebuild the pipeline",
                            params = arrayOf(pipelineId)
                        )
                    }
                }
                else -> {
                    // 异常状态
                    logger.error("Status exception, builderName: $builderName, status: $status")
                    throw ErrorCodeException(
                        errorCode = "2103502",
                        defaultMessage = "Status exception, please try rebuild the pipeline",
                        params = arrayOf(pipelineId)
                    )
                }
            }
        }

        // 设置containerName缓存
        redisUtils.setDebugBuilderName(dispatchType, userId, pipelineId, vmSeqId, builderName)

        return DispatchDebugResponse(
            websocketUrl = dispatchFactory.load(dispatchType).getDebugWebsocketUrl(
                projectId = projectId,
                pipelineId = pipelineId,
                staffName = userId,
                builderName = builderName
            ),
            containerName = builderName
        )
    }

    fun stopDebug(
        userId: String,
        dispatchType: DispatchEnumType,
        pipelineId: String,
        vmSeqId: String,
        builderName: String,
        needCheckPermission: Boolean = true
    ): Boolean {
        val debugBuilderName = builderName.ifBlank {
            redisUtils.getDebugBuilderName(dispatchType, userId, pipelineId, vmSeqId) ?: ""
        }

        logger.info(
            "$userId stop debug ${dispatchType.value} pipelineId: $pipelineId builderName: $debugBuilderName " +
                "vmSeqId: $vmSeqId"
        )

        // 检验权限
        if (needCheckPermission) {
            checkPermission(dispatchType, userId, pipelineId, debugBuilderName, vmSeqId)
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val builder =
                dispatchKubernetesBuildDao.getBuilderStatus(
                    dslContext = transactionContext,
                    dispatchType = dispatchType.value,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    builderName = debugBuilderName
                )
            if (builder != null) {
                // 先更新debug状态
                dispatchKubernetesBuildDao.updateDebugStatus(
                    dslContext = transactionContext,
                    dispatchType = dispatchType.value,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    builderName = builder.containerName,
                    debugStatus = false
                )
                if (builder.status == 0 && builder.debugStatus) {
                    // 关闭容器
                    val taskId = dispatchFactory.load(dispatchType).operateDebugBuilder(
                        buildId = "",
                        vmSeqId = vmSeqId,
                        userId = userId,
                        builderName = debugBuilderName,
                        param = DispatchDebugOperateBuilderParams(DispatchDebugOperateBuilderType.STOP, null)
                    )
                    val opResult = dispatchFactory.load(dispatchType).waitDebugTaskFinish(userId, taskId)
                    if (opResult.status == DispatchDebugTaskStatusEnum.SUCCEEDED) {
                        logger.info("stop debug $debugBuilderName success.")
                    } else {
                        // 停不掉，尝试删除
                        logger.info("stop debug $debugBuilderName failed, msg: ${opResult.errMsg}")
                        logger.info("stop debug $debugBuilderName failed, try to delete it.")
                        dispatchFactory.load(dispatchType).operateDebugBuilder(
                            buildId = "",
                            vmSeqId = vmSeqId,
                            userId = userId,
                            builderName = debugBuilderName,
                            param = DispatchDebugOperateBuilderParams(DispatchDebugOperateBuilderType.DELETE, null)
                        )
                        dispatchKubernetesBuildDao.delete(dslContext, dispatchType.value, pipelineId, vmSeqId, builder.poolNo)
                    }
                } else {
                    logger.info(
                        "stop ${dispatchType.value} debug pipelineId: $pipelineId, vmSeqId: $vmSeqId " +
                            "debugBuilderName:$debugBuilderName 容器没有处于debug或正在占用中"
                    )
                }
            } else {
                logger.info(
                    "stop ${dispatchType.value} debug pipelineId: $pipelineId, vmSeqId: $vmSeqId " +
                        "debugBuilderName:$debugBuilderName 容器已不存在"
                )
            }
        }

        return true
    }

    private fun startSleepContainer(
        dispatchType: DispatchEnumType,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        builderName: String
    ) {
        val taskId = dispatchFactory.load(dispatchType).operateDebugBuilder(
            buildId = buildId ?: "",
            vmSeqId = vmSeqId,
            userId = userId,
            builderName = builderName,
            param = DispatchDebugOperateBuilderParams(
                type = DispatchDebugOperateBuilderType.START_SLEEP,
                env = mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to dispatchFactory.load(dispatchType).slaveEnv
                )
            )
        )

        logger.info("$userId start builder, taskId:($taskId)")
        val startResult = dispatchFactory.load(dispatchType).waitDebugTaskFinish(userId, taskId)
        if (startResult.status == DispatchDebugTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info("$userId start ${dispatchType.value} builder success")
        } else {
            logger.error("$userId start ${dispatchType.value} builder failed, msg: ${startResult.errMsg}")
            throw ErrorCodeException(
                errorCode = "2103503",
                defaultMessage = "构建机启动失败，错误信息:${startResult.errMsg}"
            )
        }
    }

    private fun checkPermission(
        dispatchType: DispatchEnumType,
        userId: String,
        pipelineId: String,
        builderName: String,
        vmSeqId: String
    ) {
        val builderInfo = dispatchKubernetesBuildDao.getBuilderStatus(
            dslContext = dslContext,
            dispatchType = dispatchType.value,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            builderName = builderName
        )!!
        val projectId = builderInfo.projectId
        // 检验权限
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT,
                projectId, pipelineId, AuthPermission.EDIT
            )
        ) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }
    }
}

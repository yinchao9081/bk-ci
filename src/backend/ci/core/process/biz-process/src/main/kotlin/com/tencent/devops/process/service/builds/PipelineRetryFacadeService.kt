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

package com.tencent.devops.process.service.builds

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeLine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.ContainerIdLock
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask.Companion.addRecords
import com.tencent.devops.process.utils.JOB_RETRY_TASK_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ReturnCount", "LongParameterList", "LongMethod")
class PipelineRetryFacadeService @Autowired constructor(
    val dslContext: DSLContext,
    val redisOperation: RedisOperation,
    val pipelineEventDispatcher: PipelineEventDispatcher,
    val pipelineTaskService: PipelineTaskService,
    val pipelineContainerService: PipelineContainerService,
    val pipelineBuildRecordService: PipelineBuildRecordService,
    val containerBuildRecordService: ContainerBuildRecordService,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelinePermissionService: PipelinePermissionService
) {

    /**
     * 对处于运行中的构建[buildId]，对指定的已经失败的插件任务[taskId]进行重试，
     * [skipFailedTask]表示是否在重试时直接设置为跳过，并继续流转执行后续的其他插件任务。
     * 当[taskId]参数不正确或其他原因导致找不到具体的构建任务时，返回false.
     *
     * @throws ErrorCodeException 抛出异常仅当发生以下情况：
     * 1、retry/skip的 task 所在的 Job 必须是结束状态，否则无法被重试或者跳过
     * 2、retry/skip的 task 必须是失败或者被取消的状态，否则无法被重试或者跳过
     * 3、在以上逻辑的基础，在尝试skip 未勾选“手动跳过”的task，但调用者userId又没有【API 特殊操作】权限的，则会失败
     */
    @Throws(ErrorCodeException::class)
    fun runningBuildTaskRetry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        resourceVersion: Int,
        taskId: String? = null,
        skipFailedTask: Boolean? = false
    ): Boolean {
        logger.info("runningBuildTaskRetry $userId|$projectId|$pipelineId|$buildId|$taskId}")
        if (taskId.isNullOrEmpty()) {
            return false
        }
        val targetTask = pipelineTaskService.getBuildTask(
            projectId = projectId, buildId = buildId, taskId = taskId, stepId = null
        ) ?: pipelineTaskService.getBuildTask(
            projectId = projectId,
            buildId = buildId,
            taskId = null,
            stepId = taskId
        ) ?: return false
        // 刷新当前job的开关机以及job状态， container状态， detail数据, record数据
        ContainerIdLock(redisOperation, buildId, targetTask.containerId).use {
            it.lock()

            val targetJob = pipelineContainerService.getContainer(
                projectId = projectId,
                buildId = buildId,
                containerId = targetTask.containerId,
                stageId = targetTask.stageId
            ) ?: return false

            // 校验当前job的关机事件是否有完成以及要重试或者跳过的任务是否已经失败
            preconditionCheck(targetJob, targetTask)

            refreshTaskAndJob(
                userId = userId,
                projectId = projectId,
                buildId = buildId,
                executeCount = executeCount,
                resourceVersion = resourceVersion,
                taskId = taskId,
                containerInfo = targetJob,
                skipFailedTask = skipFailedTask
            )
        }

        // 发送container Refresh事件，重新开始task对应的调度
        sendContainerEvent(targetTask, userId)

        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = if (skipFailedTask == true) {
                "$userId skip the fail task [${targetTask.taskName}]"
            } else {
                "$userId retry fail task [${targetTask.taskName}]"
            },
            tag = targetTask.taskId,
            jobId = targetTask.containerId,
            executeCount = targetTask.executeCount ?: 1,
            stepId = targetTask.stepId
        )
        return true
    }

    // 发送container刷新事件，将重置后的job重新丢入引擎中调度
    private fun sendContainerEvent(taskInfo: PipelineBuildTask, userId: String) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "runningBuildRetry${taskInfo.buildId}|${taskInfo.taskId}",
                containerId = taskInfo.containerId,
                containerHashId = taskInfo.containerHashId,
                stageId = taskInfo.stageId,
                pipelineId = taskInfo.pipelineId,
                buildId = taskInfo.buildId,
                userId = userId,
                projectId = taskInfo.projectId,
                actionType = ActionType.REFRESH,
                executeCount = taskInfo.executeCount,
                containerType = ""
            )
        )
    }

    /**
     * 1、retry/skip的 task 所在的 Job 必须是结束状态，否则无法被重试或者跳过
     * 2、retry/skip的 task 必须是失败状态，否则无法被重试或者跳过
     */
    private fun preconditionCheck(targetJob: PipelineBuildContainer, targetTask: PipelineBuildTask) {
        // 1、retry/skip的 task 所在的 Job 必须是结束状态，否则无法被重试或者跳过
        Preconditions.checkTrue(
            condition = targetJob.status.isFinish(),
            exception = ErrorCodeException(errorCode = ProcessMessageCode.ERROR_JOB_RUNNING)
        )
        // 2、retry/skip的 task 必须是失败或者被取消的状态，否则无法被重试或者跳过
        Preconditions.checkTrue(
            condition = targetTask.status.isFailure() || targetTask.status.isCancel(),
            exception = ErrorCodeException(errorCode = ProcessMessageCode.ERROR_RETRY_STAGE_NOT_FAILED)
        )
    }

    // 刷新要重试task所属job的开关机状态，task状态。 container状态，detail相关信息
    private fun refreshTaskAndJob(
        userId: String,
        projectId: String,
        buildId: String,
        executeCount: Int,
        resourceVersion: Int,
        taskId: String,
        containerInfo: PipelineBuildContainer,
        skipFailedTask: Boolean? = false
    ) {
        val taskBuilds = pipelineTaskService.listContainerBuildTasks(projectId, buildId, containerInfo.containerId)
        val taskRecords = mutableListOf<BuildRecordTask>()
        // 待重试task所属job对应的startVm，stopVm，endTask，对应task状态重置为Queue
        val startAndEndTask = mutableListOf<PipelineBuildTask>()
        taskBuilds.forEach { t ->
            val task = t.copy(executeCount = executeCount)
            if (task.taskId == taskId) {
                // issues_6831: 若设置了手动跳过 且重试时选择了跳过当前插件则不刷新当前失败的插件,直接把task内状态改为SKIP
                if (skipFailedTask == true) {
                    val isSkipTask = isSkipTask(
                        userId = userId,
                        projectId = projectId,
                        manualSkip = task.additionalOptions?.manualSkip
                    )
                    if (isSkipTask) {
                        pipelineTaskService.updateTaskStatus(
                            task = task,
                            userId = userId,
                            buildStatus = BuildStatus.SKIP
                        )
                        return@forEach
                    }
                }
                startAndEndTask.add(task)
                taskRecords.addRecords(mutableListOf(task), resourceVersion)
            } else if (task.taskName.startsWith(VMUtils.getCleanVmLabel()) &&
                task.taskId.startsWith(VMUtils.getStopVmLabel())
            ) {
                startAndEndTask.add(task)
            } else if (task.taskName.startsWith(VMUtils.getPrepareVmLabel()) &&
                task.taskId.startsWith(VMUtils.getStartVmLabel())
            ) {
                startAndEndTask.add(task)
            } else if (task.taskName.startsWith(VMUtils.getWaitLabel()) &&
                task.taskId.startsWith(VMUtils.getEndLabel())
            ) {
                startAndEndTask.add(task)
            } else if (task.status == BuildStatus.UNEXEC) {
                startAndEndTask.add(task)
                taskRecords.addRecords(mutableListOf(task), resourceVersion)
            }
        }
        startAndEndTask.forEach {
            pipelineTaskService.updateTaskStatus(task = it, userId = userId, buildStatus = BuildStatus.QUEUE)
        }

        val lastContainerRecord = containerBuildRecordService.getRecord(
            transactionContext = null,
            projectId = containerInfo.projectId,
            pipelineId = containerInfo.pipelineId,
            buildId = containerInfo.buildId,
            containerId = containerInfo.containerId,
            executeCount = (executeCount - 1).coerceAtLeast(1) // 至少取第一次执行结果
        )
        val containerRecord = if (lastContainerRecord != null) {
            val containerVar = lastContainerRecord.containerVar
            containerVar.remove(Container::timeCost.name)
            containerVar.remove(Container::startVMStatus.name)
            containerVar.remove(Container::startEpoch.name)
            containerVar.remove(BuildRecordTimeLine::class.java.simpleName)
            // 将当前重试 task id 做记录
            containerVar[JOB_RETRY_TASK_ID] = taskId
            lastContainerRecord.copy(
                status = BuildStatus.QUEUE.name, containerVar = containerVar,
                executeCount = executeCount, timestamps = mapOf()
            )
        } else {
            BuildRecordContainer(
                projectId = containerInfo.projectId,
                pipelineId = containerInfo.pipelineId,
                resourceVersion = resourceVersion,
                buildId = containerInfo.buildId,
                stageId = containerInfo.stageId,
                containerId = containerInfo.containerId,
                containerType = containerInfo.containerType,
                executeCount = executeCount,
                containPostTaskFlag = containerInfo.containPostTaskFlag,
                matrixGroupFlag = containerInfo.matrixGroupFlag,
                status = BuildStatus.QUEUE.name,
                containerVar = mutableMapOf(),
                timestamps = mapOf()
            )
        }
        pipelineBuildRecordService.batchSave(
            transactionContext = null, model = null, stageList = null,
            containerList = listOf(containerRecord), taskList = taskRecords
        )
        // 修改容器状态位运行
        pipelineContainerService.updateContainerStatus(
            projectId = containerInfo.projectId,
            buildId = containerInfo.buildId,
            stageId = containerInfo.stageId,
            containerId = containerInfo.containerId,
            buildStatus = BuildStatus.QUEUE
        )
    }

    fun isSkipTask(
        userId: String,
        projectId: String,
        manualSkip: Boolean?
    ): Boolean {
        // 若该task具有手动跳过配置或者用户拥有API特殊操作权限，才允许跳过.
        return manualSkip == true || try {
            pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                authResourceType = AuthResourceType.PROJECT,
                permission = AuthPermission.API_OPERATE
            )
        } catch (ignore: Exception) {
            logger.info("the project has not registered api operation permission:$userId|$projectId|$ignore")
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRetryFacadeService::class.java)
    }
}

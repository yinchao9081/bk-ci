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

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.common.BuildTimeCostUtils.generateTaskTimeCost
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.pojo.PipelineTaskStatusInfo
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.record.PipelineRecordModelService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress(
    "LongParameterList",
    "MagicNumber",
    "ReturnCount",
    "TooManyFunctions",
    "ComplexCondition",
    "ComplexMethod",
    "LongMethod",
    "NestedBlockDepth"
)
@Service
class TaskBuildRecordService(
    private val buildVariableService: BuildVariableService,
    private val dslContext: DSLContext,
    private val recordTaskDao: BuildRecordTaskDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val containerBuildRecordService: ContainerBuildRecordService,
    recordModelService: PipelineRecordModelService,
    pipelineResourceDao: PipelineResourceDao,
    pipelineBuildDao: PipelineBuildDao,
    pipelineResourceVersionDao: PipelineResourceVersionDao,
    stageTagService: StageTagService,
    buildRecordModelDao: BuildRecordModelDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    redisOperation: RedisOperation
) : BaseBuildRecordService(
    dslContext = dslContext,
    buildRecordModelDao = buildRecordModelDao,
    stageTagService = stageTagService,
    pipelineEventDispatcher = pipelineEventDispatcher,
    redisOperation = redisOperation,
    recordModelService = recordModelService,
    pipelineResourceDao = pipelineResourceDao,
    pipelineBuildDao = pipelineBuildDao,
    pipelineResourceVersionDao = pipelineResourceVersionDao
) {

    fun updateTaskStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        operation: String,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ) {
        updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = buildStatus,
            taskVar = emptyMap(),
            timestamps = timestamps,
            operation = operation
        )
    }

    fun taskStart(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ) {
        update(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            executeCount = executeCount, buildStatus = BuildStatus.RUNNING,
            cancelUser = null, operation = "taskStart#$taskId"
        ) {
            val delimiters = ","
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val recordTask = recordTaskDao.getRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount
                ) ?: run {
                    logger.warn(
                        "ENGINE|$buildId|updateTaskByMap| get task($taskId) record failed."
                    )
                    return@transaction
                }
                val taskVar = mutableMapOf<String, Any>()
                val taskStatus: BuildStatus
                if (
                    recordTask.classType == ManualReviewUserTaskElement.classType ||
                    (recordTask.classType == MatrixStatusElement.classType &&
                        recordTask.originClassType == ManualReviewUserTaskElement.classType)
                ) {
                    taskStatus = BuildStatus.REVIEWING
                    val list = mutableListOf<String>()
                    taskVar[ManualReviewUserTaskElement::reviewUsers.name]?.let {
                        try {
                            (it as List<*>).forEach { reviewUser ->
                                list.addAll(
                                    buildVariableService.replaceTemplate(projectId, buildId, reviewUser.toString())
                                        .split(delimiters)
                                )
                            }
                        } catch (ignore: Throwable) {
                            return@let
                        }
                    }
                    taskVar[ManualReviewUserTaskElement::reviewUsers.name] = list
                } else if (
                    recordTask.classType == QualityGateInElement.classType ||
                    recordTask.classType == QualityGateOutElement.classType ||
                    recordTask.originClassType == QualityGateInElement.classType ||
                    recordTask.originClassType == QualityGateOutElement.classType
                ) {
                    taskStatus = BuildStatus.REVIEWING
                } else {
                    taskStatus = BuildStatus.RUNNING
                }

                // TODO #7983 即将废除的旧数据兼容
                if (taskVar[Element::startEpoch.name] == null) { // 自动重试，startEpoch 不会为null，所以不需要查redis来确认
                    taskVar[Element::startEpoch.name] = System.currentTimeMillis()
                }
                taskVar.remove(Element::elapsed.name)
                taskVar.remove(Element::errorType.name)
                taskVar.remove(Element::errorCode.name)
                taskVar.remove(Element::errorMsg.name)
                // #10751 增加对运行中重试的兼容，因为不新增执行次数，需要刷新上一次失败的结束时间
                if (recordTask.endTime != null) recordTaskDao.flushEndTimeWhenRetry(
                    dslContext = context, projectId = projectId, pipelineId = pipelineId,
                    buildId = buildId, taskId = taskId, executeCount = executeCount
                )
                recordTaskDao.updateRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = recordTask.taskVar.plus(taskVar),
                    buildStatus = taskStatus,
                    startTime = recordTask.startTime ?: LocalDateTime.now(),
                    endTime = null,
                    timestamps = null
                )
            }
        }
    }

    fun taskPause(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ) {
        updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = BuildStatus.PAUSE,
            taskVar = mapOf(
                TASK_PAUSE_TAG_VAR to true
            ),
            operation = "taskPause#$taskId",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to BuildRecordTimeStamp(
                    LocalDateTime.now().timestampmilli(), null
                )
            )
        )
    }

    fun taskAlreadyPause(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): Boolean {
        val record = getTaskBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
        return record?.taskVar?.get(TASK_PAUSE_TAG_VAR) == true
    }

    fun taskPauseCancel(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ) {
        updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = BuildStatus.CANCELED,
            taskVar = emptyMap(),
            operation = "taskCancel#$taskId",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            )
        )
    }

    fun taskPauseContinue(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        taskId: String,
        executeCount: Int
    ) {
        // #7983 此处需要保持Container状态独立刷新，不能放进更新task的并发锁
        containerBuildRecordService.updateContainerStatus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount,
            buildStatus = BuildStatus.QUEUE,
            operation = "updateElementWhenPauseContinue#$taskId"
        )
        // TODO #7983 重写同container下的插件input
        updateTaskStatus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = BuildStatus.QUEUE,
            operation = "updateElementWhenPauseContinue#$taskId",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            )
        )
    }

    fun taskEnd(taskBuildEndParam: TaskBuildEndParam): List<PipelineTaskStatusInfo> {

        val projectId = taskBuildEndParam.projectId
        val pipelineId = taskBuildEndParam.pipelineId
        val buildId = taskBuildEndParam.buildId
        val taskId = taskBuildEndParam.taskId
        val executeCount = taskBuildEndParam.executeCount
        val recordTask = recordTaskDao.getRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        ) ?: run {
            logger.warn(
                "ENGINE|$buildId|taskEnd| get task($taskId) record failed."
            )
            return emptyList()
        }
        // #7983 将RETRY中间态过滤，不体现在详情页面
        val buildStatus = taskBuildEndParam.buildStatus.let {
            if (it == BuildStatus.RETRY) null else it
        }
        val atomVersion = taskBuildEndParam.atomVersion
        val errorType = taskBuildEndParam.errorType
        update(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            executeCount = executeCount, buildStatus = BuildStatus.RUNNING,
            cancelUser = null, operation = "taskEnd#$taskId"
        ) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val now = LocalDateTime.now()
                // 插件存在自动重试，永远更新一次当前时间为结束时间
                recordTask.endTime = now
                val taskVar = mutableMapOf<String, Any>()
                if (atomVersion != null) {
                    // 将插件的执行版本刷新
                    if (
                        recordTask.classType == MarketBuildAtomElement.classType ||
                        recordTask.originClassType == MarketBuildAtomElement.classType ||
                        recordTask.classType == MarketBuildLessAtomElement.classType ||
                        recordTask.originClassType == MarketBuildLessAtomElement.classType
                    ) {
                        taskVar[MarketBuildAtomElement::version.name] = atomVersion
                    } else {
                        taskVar[MarketBuildAtomElement::version.name] = INIT_VERSION
                    }
                }
                var timestamps: MutableMap<BuildTimestampType, BuildRecordTimeStamp>? = null
                if (recordTask.status == BuildStatus.PAUSE.name) {
                    timestamps = mergeTimestamps(
                        recordTask.timestamps,
                        mapOf(
                            BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                                BuildRecordTimeStamp(null, now.timestampmilli())
                        )
                    )
                }
                // 重置暂停任务暂停状态位
                recordTask.taskVar.remove(TASK_PAUSE_TAG_VAR)
                if (errorType != null) {
                    taskVar[Element::errorType.name] = errorType.name
                    taskBuildEndParam.errorCode?.let { taskVar[Element::errorCode.name] = it }
                    taskBuildEndParam.errorMsg?.let { taskVar[Element::errorMsg.name] = it }
                }
                recordTask.generateTaskTimeCost()?.let {
                    taskVar[Element::timeCost.name] = it
                }
                recordTaskDao.updateRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = recordTask.taskVar.plus(taskVar),
                    buildStatus = buildStatus,
                    startTime = null,
                    endTime = now,
                    timestamps = timestamps
                )
            }
        }
        if (buildStatus?.isCancel() != true && buildStatus?.isSkip() != true) {
            // 如果状态不是取消状态或者跳过状态，无需处理后续更新task状态的逻辑
            return emptyList()
        }
        val pipelineTaskStatusInfos = mutableListOf<PipelineTaskStatusInfo>()
        val buildRecordContainer = containerBuildRecordService.getRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = taskBuildEndParam.containerId,
            executeCount = executeCount
        )
        buildRecordContainer?.let {
            val buildTask = pipelineBuildTaskDao.get(
                dslContext = dslContext, projectId = projectId, buildId = buildId, taskId = taskId, stepId = null
            ) ?: return@let
            val runCondition = buildTask.additionalOptions?.runCondition
            val containPostTaskFlag = buildRecordContainer.containPostTaskFlag
            val containerId = buildRecordContainer.containerId
            // 判断取消的task任务对应的container是否包含post任务
            val cancelTaskPostFlag = buildStatus == BuildStatus.CANCELED && containPostTaskFlag == true
            val currentTaskSeq = recordTask.taskSeq
            if (cancelTaskPostFlag) {
                val postTaskFlag = recordTask.elementPostInfo != null
                // 判断当前取消的任务是否是post任务
                if (!postTaskFlag && runCondition != RunCondition.PRE_TASK_FAILED_EVEN_CANCEL) {
                    // 查询post任务列表
                    val recordPostTasks = recordTaskDao.getRecords(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        executeCount = executeCount,
                        containerId = containerId,
                        queryPostTaskFlag = true
                    )
                    val startTaskSeq = currentTaskSeq + 1
                    var endTaskSeq = startTaskSeq
                    recordPostTasks.forEach { recordPostTask ->
                        // 计算post父任务序号
                        val parentElementJobIndex =
                            recordPostTask.elementPostInfo?.parentElementJobIndex ?: return@forEach
                        val parentTaskSeq = parentElementJobIndex + 2
                        // 判断父任务的序号是否在取消任务之后
                        if (parentTaskSeq <= currentTaskSeq) {
                            endTaskSeq = recordPostTask.taskSeq - 1
                        }
                    }
                    addCancelTaskStatusInfo(
                        taskBuildEndParam = taskBuildEndParam,
                        startTaskSeq = startTaskSeq,
                        endTaskSeq = endTaskSeq,
                        pipelineTaskStatusInfos = pipelineTaskStatusInfos
                    )
                }
            } else if (buildStatus.isCancel() && runCondition != RunCondition.PRE_TASK_FAILED_EVEN_CANCEL) {
                val startTaskSeq = currentTaskSeq + 1
                val endTaskSeq = VMUtils.genVMTaskSeq(containerId.toInt(), 0) - 1
                addCancelTaskStatusInfo(
                    taskBuildEndParam = taskBuildEndParam,
                    startTaskSeq = startTaskSeq,
                    endTaskSeq = endTaskSeq,
                    pipelineTaskStatusInfos = pipelineTaskStatusInfos
                )
            } else if (buildStatus.isSkip()) {
                pipelineTaskStatusInfos.add(
                    PipelineTaskStatusInfo(
                        taskId = taskId,
                        containerHashId = containerId,
                        buildStatus = buildStatus,
                        executeCount = executeCount,
                        message = taskBuildEndParam.errorMsg,
                        stepId = buildTask.stepId
                    )
                )
                updateTaskStatus(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    buildStatus = buildStatus,
                    operation = "taskSkip#$taskId"
                )
            }
        }
        return pipelineTaskStatusInfos
    }

    private fun addCancelTaskStatusInfo(
        taskBuildEndParam: TaskBuildEndParam,
        startTaskSeq: Int,
        endTaskSeq: Int,
        pipelineTaskStatusInfos: MutableList<PipelineTaskStatusInfo>
    ) {
        if (endTaskSeq < startTaskSeq) {
            return
        }
        val projectId = taskBuildEndParam.projectId
        val containerId = taskBuildEndParam.containerId
        val buildId = taskBuildEndParam.buildId
        val executeCount = taskBuildEndParam.executeCount
        // 把post任务和取消任务之间的任务置为UNEXEC状态
        val buildTasks = pipelineBuildTaskDao.getTasksInCondition(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            containerId = containerId,
            statusSet = null,
            startTaskSeq = startTaskSeq,
            endTaskSeq = endTaskSeq
        )
        var unExecTaskIds: MutableSet<String>? = null
        buildTasks.forEach { pipelineBuildTask ->
            val additionalOptions = pipelineBuildTask.additionalOptions
            if (!pipelineBuildTask.status.isFinish() && additionalOptions?.elementPostInfo == null) {
                if (unExecTaskIds == null) {
                    unExecTaskIds = mutableSetOf()
                }
                val unExecBuildStatus = BuildStatus.UNEXEC
                val taskId = pipelineBuildTask.taskId
                unExecTaskIds?.add(taskId)
                pipelineTaskStatusInfos.add(
                    PipelineTaskStatusInfo(
                        taskId = taskId,
                        containerHashId = containerId,
                        buildStatus = unExecBuildStatus,
                        executeCount = executeCount,
                        message = "Do not meet the run conditions, ignored.",
                        stepId = pipelineBuildTask.stepId
                    )
                )
            }
        }
        if (!unExecTaskIds.isNullOrEmpty()) {
            recordTaskDao.updateRecordStatus(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = taskBuildEndParam.pipelineId,
                buildId = buildId,
                executeCount = executeCount,
                buildStatus = BuildStatus.UNEXEC,
                taskIds = unExecTaskIds
            )
        }
    }

    fun updateTaskRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        taskVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        operation: String,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ) {
        update(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            executeCount = executeCount, buildStatus = BuildStatus.RUNNING,
            cancelUser = null, operation = operation
        ) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                val recordTask = recordTaskDao.getRecord(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount
                ) ?: run {
                    logger.warn("ENGINE|$buildId|updateTaskRecord| get task($taskId) record failed.")
                    return@transaction
                }
                var startTime: LocalDateTime? = null
                var endTime: LocalDateTime? = null
                val now = LocalDateTime.now()
                val newTimestamps = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()
                if (buildStatus?.isRunning() == true) {
                    if (recordTask.startTime == null) startTime = now
                    // #10751 增加对运行中重试的兼容，因为不新增执行次数，需要刷新上一次失败的结束时间
                    if (recordTask.endTime != null) recordTaskDao.flushEndTimeWhenRetry(
                        dslContext = transactionContext, projectId = projectId, pipelineId = pipelineId,
                        buildId = buildId, taskId = taskId, executeCount = executeCount
                    )
                }
                if (buildStatus?.isFinish() == true && recordTask.endTime == null) {
                    endTime = now
                    if (BuildStatus.parse(recordTask.status) == BuildStatus.REVIEWING) {
                        newTimestamps[BuildTimestampType.TASK_REVIEW_PAUSE_WAITING] =
                            BuildRecordTimeStamp(null, now.timestampmilli())
                    }
                }
                recordTaskDao.updateRecord(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = recordTask.taskVar.plus(taskVar),
                    buildStatus = buildStatus,
                    startTime = recordTask.startTime ?: startTime,
                    endTime = endTime,
                    timestamps = timestamps?.let { mergeTimestamps(timestamps, recordTask.timestamps) }
                )
            }
        }
    }

    fun getTaskBuildRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): BuildRecordTask? {
        return recordTaskDao.getRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
    }

    fun updateAsyncStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        asyncStatus: String
    ) {
        recordTaskDao.updateAsyncStatus(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            asyncStatus = asyncStatus
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskBuildRecordService::class.java)
        private const val TASK_PAUSE_TAG_VAR = "taskPause"
    }
}

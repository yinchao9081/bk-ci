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

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.pojo.TokenType
import com.tencent.devops.worker.common.api.quality.QualityGatewaySDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.RepoServiceFactory
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.script.bat.WindowsScriptTask
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.TaskUtil
import java.io.File
import java.net.URLDecoder
import org.slf4j.LoggerFactory

/**
 * 构建脚本任务
 */
@Suppress("LongMethod")
open class ScriptTask : ITask() {

    private val gatewayResourceApi = ApiFactory.create(QualityGatewaySDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val scriptType = taskParams["scriptType"] ?: throw TaskExecuteException(
            errorMsg = "Unknown script type of build script task",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_INPUT_INVAILD
        )

        // #4601 如果task.json没有指定字符集选项则保持为空
        val charsetType = taskParams["charsetType"]

        val continueNoneZero = taskParams["continueNoneZero"] ?: "false"
        // 如果脚本执行失败之后可以选择归档这个问题
        val archiveFileIfExecFail = taskParams["archiveFile"]
        val script = URLDecoder.decode(
            taskParams["script"]
                ?: throw TaskExecuteException(
                    errorMsg = "Empty build script content",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                ), "UTF-8"
        ).replace("\r", "")
        logger.info("Start to execute the script task($scriptType) ($script)")
        val command = CommandFactory.create(scriptType)
        val buildId = buildVariables.buildId
        val runtimeVariables = buildVariables.variables
        val projectId = buildVariables.projectId

        ScriptEnvUtils.cleanEnv(buildId, workspace)
        ScriptEnvUtils.cleanContext(buildId, workspace)

        val variables = if (buildTask.buildVariable == null) {
            runtimeVariables
        } else {
            runtimeVariables.plus(buildTask.buildVariable!!)
        }

        try {
            command.execute(
                buildId = buildId,
                stepId = buildTask.stepId,
                script = script,
                taskParam = taskParams,
                runtimeVariables = variables.plus(TaskUtil.getTaskEnvVariables(buildVariables, buildTask.taskId)),
                projectId = projectId,
                dir = workspace,
                buildEnvs = takeBuildEnvs(buildTask, buildVariables),
                continueNoneZero = continueNoneZero.toBoolean(),
                errorMessage = "Fail to run the plugin",
                charsetType = charsetType,
                taskId = buildTask.taskId
            )
        } catch (ignore: Throwable) {
            logger.warn("Fail to run the script task", ignore)
            if (!archiveFileIfExecFail.isNullOrBlank()) {
                LoggerService.addErrorLine("脚本执行失败， 归档${archiveFileIfExecFail}文件")
                val token = RepoServiceFactory.getInstance().getRepoToken(
                    userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
                    projectId = buildVariables.projectId,
                    repoName = "pipeline",
                    path = "/${buildVariables.pipelineId}/${buildVariables.buildId}",
                    type = TokenType.UPLOAD,
                    expireSeconds = TaskUtil.getTimeOut(buildTask).times(60)
                )
                val count = ArchiveUtils.archivePipelineFiles(
                    filePath = archiveFileIfExecFail,
                    workspace = workspace,
                    buildVariables = buildVariables,
                    token = token
                )
                if (count == 0) {
                    LoggerService.addErrorLine("脚本执行失败之后没有匹配到任何待归档文件")
                }
            }
            throw TaskExecuteException(
                errorMsg = "脚本执行失败" +
                    "\n======问题排查指引======\n" +
                    "当脚本退出码非0时，执行失败。可以从以下路径进行分析：\n" +
                    "1. 根据错误日志排查\n" +
                    "2. 在本地手动执行脚本。如果本地执行也失败，很可能是脚本逻辑问题；" +
                    "如果本地OK，排查构建环境（比如环境依赖、或者代码变更等）",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        } finally {
            // 成功失败都写入全局变量
            addEnv(ScriptEnvUtils.getEnv(buildId, workspace))
            // 上下文返回给全局时追加jobs前缀
            val jobPrefix = "jobs.${buildVariables.jobId ?: buildVariables.containerId}"
            addEnv(mapOf("$jobPrefix.os" to AgentEnv.getOS().name))
            addEnv(ScriptEnvUtils.getContext(buildId, workspace).map { context ->
                "$jobPrefix.${context.key}" to context.value
            }.toMap())
            ScriptEnvUtils.cleanWhenEnd(buildId, workspace)

            // 设置质量红线指标信息
            setGatewayValue(workspace)
        }
    }

    open fun takeBuildEnvs(
        buildTask: BuildTask,
        buildVariables: BuildVariables
    ): List<BuildEnv> = buildVariables.buildEnvs

    private fun setGatewayValue(workspace: File) {
        try {
            val gatewayFile = File(workspace, ScriptEnvUtils.getQualityGatewayEnvFile())
            if (!gatewayFile.exists()) return
            val data = gatewayFile.readLines().map {
                val key = it.split("=").getOrNull(0) ?: throw TaskExecuteException(
                    errorMsg = "Illegal gateway key set: $it",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                )
                val value = it.split("=").getOrNull(1) ?: throw TaskExecuteException(
                    errorMsg = "Illegal gateway key set: $it",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                )
                key to value.trim()
            }.toMap()
            val elementType = if (this is WindowsScriptTask) {
                WindowsScriptElement.classType
            } else {
                LinuxScriptElement.classType
            }
            LoggerService.addNormalLine("save gateway value($elementType): $data")
            gatewayResourceApi.saveScriptHisMetadata(elementType, data)
            gatewayFile.delete()
        } catch (ignore: Exception) {
            LoggerService.addErrorLine("save gateway value fail: ${ignore.message}")
            logger.error("setGatewayValue|${ignore.message}", ignore)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScriptTask::class.java)
    }
}

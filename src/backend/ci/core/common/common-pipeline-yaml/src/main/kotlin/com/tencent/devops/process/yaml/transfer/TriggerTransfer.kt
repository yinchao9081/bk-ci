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

package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.transfer.pojo.WebHookTriggerElementChanger
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.v3.models.on.EnableType
import com.tencent.devops.process.yaml.v3.models.on.IssueRule
import com.tencent.devops.process.yaml.v3.models.on.MrRule
import com.tencent.devops.process.yaml.v3.models.on.NoteRule
import com.tencent.devops.process.yaml.v3.models.on.PushRule
import com.tencent.devops.process.yaml.v3.models.on.ReviewRule
import com.tencent.devops.process.yaml.v3.models.on.TagRule
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TriggerTransfer @Autowired(required = false) constructor(
    val client: Client,
    @Autowired(required = false)
    val creator: TransferCreator,
    val transferCache: TransferCacheService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerTransfer::class.java)
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGit(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore,
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    eventType = CodeEventType.PUSH,
                    // todo action
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable)
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore,
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(tag.enable)
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore,
                    block = mr.blockMr,
                    webhookQueue = mr.webhookQueue,
                    enableCheck = mr.reportCommitCheck,
                    pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    // todo action
                    eventType = CodeEventType.MERGE_REQUEST,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(mr.enable)
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    includeCrState = review.states,
                    includeCrTypes = review.types,
                    eventType = CodeEventType.REVIEW,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(review.enable)
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    includeIssueAction = issue.action,
                    eventType = CodeEventType.ISSUES,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(issue.enable)
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeGitWebHookTriggerElement(
                    includeNoteTypes = note.types?.map {
                        when (it) {
                            "commit" -> "Commit"
                            "merge_request" -> "Review"
                            "issue" -> "Issue"
                            else -> it
                        }
                    },
                    includeNoteComment = note.comment.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.NOTE,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(note.enable)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun git2YamlTriggerOn(
        elements: List<WebHookTriggerElementChanger>,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper
    ): List<TriggerOn> {
        val res = mutableMapOf<String, TriggerOn>()
        elements.forEach { git ->
            val name = when (git.repositoryType) {
                TriggerRepositoryType.ID -> git.repositoryHashId ?: ""
                TriggerRepositoryType.NAME -> git.repositoryName ?: ""
                TriggerRepositoryType.SELF -> "self"
                else -> ""
            }
            val nowExist = res.getOrPut(name) {
                when (name) {
                    git.repositoryHashId -> TriggerOn(
                        repoName = transferCache.getGitRepository(projectId, RepositoryType.ID, name)?.aliasName
                    )

                    git.repositoryName -> TriggerOn(repoName = name)
                    else -> TriggerOn()
                }
            }
            when (git.eventType) {
                CodeEventType.PUSH -> nowExist.push = PushRule(
                    enable = git.enable.nullIfDefault(true),
                    branches = git.branchName?.disjoin() ?: emptyList(),
                    branchesIgnore = git.excludeBranchName?.disjoin(),
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name),
                    // todo action
                    action = null
                )

                CodeEventType.TAG_PUSH -> nowExist.tag = TagRule(
                    enable = git.enable.nullIfDefault(true),
                    tags = git.tagName?.disjoin(),
                    tagsIgnore = git.excludeTagName?.disjoin(),
                    fromBranches = git.fromBranches?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers
                )

                CodeEventType.MERGE_REQUEST -> nowExist.mr = MrRule(
                    enable = git.enable.nullIfDefault(true),
                    targetBranches = git.branchName?.disjoin(),
                    targetBranchesIgnore = git.excludeBranchName?.disjoin(),
                    sourceBranches = git.includeSourceBranchName?.disjoin(),
                    sourceBranchesIgnore = git.excludeSourceBranchName?.disjoin(),
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    blockMr = git.block,
                    webhookQueue = git.webhookQueue.nullIfDefault(false),
                    reportCommitCheck = git.enableCheck.nullIfDefault(true),
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name),
                    // todo action
                    action = null
                )

                CodeEventType.REVIEW -> nowExist.review = ReviewRule(
                    enable = git.enable.nullIfDefault(true),
                    states = git.includeCrState,
                    types = git.includeCrTypes
                )

                CodeEventType.ISSUES -> nowExist.issue = IssueRule(
                    enable = git.enable.nullIfDefault(true),
                    action = git.includeIssueAction
                )

                CodeEventType.NOTE -> nowExist.note = NoteRule(
                    enable = git.enable.nullIfDefault(true),
                    types = git.includeNoteTypes?.map {
                        when (it) {
                            "Commit" -> "commit"
                            "Review" -> "merge_request"
                            "Issue" -> "issue"
                            else -> it
                        }
                    }
                )

                CodeEventType.POST_COMMIT -> nowExist.push = PushRule(
                    enable = git.enable.nullIfDefault(true),
                    branches = null,
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin(),
                    users = git.includeUsers,
                    usersIgnore = git.excludeUsers,
                    pathFilterType = git.pathFilterType?.name.nullIfDefault(PathFilterType.NamePrefixFilter.name)
                )

                CodeEventType.CHANGE_COMMIT -> nowExist.push = PushRule(
                    enable = git.enable.nullIfDefault(true),
                    branches = null,
                    branchesIgnore = null,
                    paths = git.includePaths?.disjoin(),
                    pathsIgnore = git.excludePaths?.disjoin()
                )
            }
            aspectWrapper.setYamlTriggerOn(nowExist, PipelineTransferAspectWrapper.AspectType.AFTER)
        }
        return res.values.toList()
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerTGit(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            branchName = push.branches.nonEmptyOrNull()?.join(),
                            excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                            includePaths = push.paths.nonEmptyOrNull()?.join(),
                            excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                            includeUsers = push.users,
                            excludeUsers = push.usersIgnore,
                            pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                                ?: PathFilterType.NamePrefixFilter,
                            eventType = CodeEventType.PUSH,
                            // todo action
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(push.enable)
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            tagName = tag.tags.nonEmptyOrNull()?.join(),
                            excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                            fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                            includeUsers = tag.users,
                            excludeUsers = tag.usersIgnore,
                            eventType = CodeEventType.TAG_PUSH,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(tag.enable)
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                            excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                            includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                            excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                            includePaths = mr.paths.nonEmptyOrNull()?.join(),
                            excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                            includeUsers = mr.users,
                            excludeUsers = mr.usersIgnore,
                            block = mr.blockMr,
                            webhookQueue = mr.webhookQueue,
                            enableCheck = mr.reportCommitCheck,
                            pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                                ?: PathFilterType.NamePrefixFilter,
                            // todo action
                            eventType = CodeEventType.MERGE_REQUEST,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(mr.enable)
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeCrState = review.states,
                            includeCrTypes = review.types,
                            eventType = CodeEventType.REVIEW,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(review.enable)
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeIssueAction = issue.action,
                            eventType = CodeEventType.ISSUES,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(issue.enable)
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeTGitWebHookTriggerElement(
                    data = CodeTGitWebHookTriggerData(
                        input = CodeTGitWebHookTriggerInput(
                            includeNoteTypes = note.types?.map {
                                when (it) {
                                    "commit" -> "Commit"
                                    "merge_request" -> "Review"
                                    "issue" -> "Issue"
                                    else -> it
                                }
                            },
                            includeNoteComment = note.comment.nonEmptyOrNull()?.join(),
                            eventType = CodeEventType.NOTE,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(note.enable)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGithub(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore.nonEmptyOrNull()?.join(),
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    eventType = CodeEventType.PUSH,
                    // todo action
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable)
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    fromBranches = tag.fromBranches.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(tag.enable)
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore.nonEmptyOrNull()?.join(),
                    webhookQueue = mr.webhookQueue,
                    enableCheck = mr.reportCommitCheck,
                    pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    // todo action
                    eventType = CodeEventType.PULL_REQUEST,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(mr.enable)
            )
        }

        triggerOn.review?.let { review ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    includeCrState = review.states,
                    includeCrTypes = review.types,
                    eventType = CodeEventType.REVIEW,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(review.enable)
            )
        }

        triggerOn.issue?.let { issue ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    includeIssueAction = issue.action,
                    eventType = CodeEventType.ISSUES,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(issue.enable)
            )
        }

        triggerOn.note?.let { note ->
            elementQueue.add(
                CodeGithubWebHookTriggerElement(
                    includeNoteTypes = note.types?.map {
                        when (it) {
                            "commit" -> "Commit"
                            "merge_request" -> "Review"
                            "issue" -> "Issue"
                            else -> it
                        }
                    },
                    includeNoteComment = note.comment.nonEmptyOrNull()?.join(),
                    eventType = CodeEventType.NOTE,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(note.enable)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerSvn(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeSVNWebHookTriggerElement(
                    relativePath = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore.nonEmptyOrNull(),
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    // todo action
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerP4(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeP4WebHookTriggerElement(
                    data = CodeP4WebHookTriggerData(
                        input = CodeP4WebHookTriggerInput(
                            includePaths = push.paths.nonEmptyOrNull()?.join(),
                            excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                            eventType = CodeEventType.CHANGE_COMMIT,
                            repositoryType = repositoryType,
                            repositoryName = triggerOn.repoName
                        )
                    )
                ).checkTriggerElementEnable(push.enable).apply { version = "2.*" }
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerBase(yamlInput: YamlTransferInput, triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        triggerOn.manual?.let { manual ->
            elementQueue.add(
                ManualTriggerElement(
                    id = "T-1-1-1",
                    canElementSkip = manual.canElementSkip,
                    useLatestParameters = manual.useLatestParameters
                ).apply {
                    this.additionalOptions = ElementAdditionalOptions(enable = manual.enable ?: true)
                }
            )
        }

        triggerOn.schedules?.let { schedule ->
            schedule.forEach { timer ->
                val repoType = when {
                    !timer.repoId.isNullOrBlank() ->
                        RepositoryType.ID
                    !timer.repoName.isNullOrBlank() ->
                        RepositoryType.NAME
                    else -> null
                }
                elementQueue.add(
                    TimerTriggerElement(
                        repoType = repoType,
                        repoHashId = timer.repoId,
                        repoName = timer.repoName,
                        branches = timer.branches,
                        newExpression = timer.newExpression,
                        advanceExpression = timer.advanceExpression,
                        noScm = timer.always != true
                    ).checkTriggerElementEnable(timer.enable)
                )
            }
        }

        triggerOn.remote?.let { remote ->
            elementQueue.add(
                RemoteTriggerElement(
                    remoteToken = yamlInput.pipelineInfo?.pipelineId?.let {
                        transferCache.getPipelineRemoteToken(
                            userId = yamlInput.userId,
                            projectId = yamlInput.projectCode,
                            pipelineId = it
                        )
                    } ?: ""
                ).checkTriggerElementEnable(remote == EnableType.TRUE.value)
            )
        }
    }

    @Suppress("ComplexMethod")
    fun yaml2TriggerGitlab(triggerOn: TriggerOn, elementQueue: MutableList<Element>) {
        val repositoryType = if (triggerOn.repoName.isNullOrBlank()) {
            TriggerRepositoryType.SELF
        } else {
            TriggerRepositoryType.NAME
        }
        triggerOn.push?.let { push ->
            elementQueue.add(
                CodeGitlabWebHookTriggerElement(
                    branchName = push.branches.nonEmptyOrNull()?.join(),
                    excludeBranchName = push.branchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = push.paths.nonEmptyOrNull()?.join(),
                    excludePaths = push.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = push.users,
                    excludeUsers = push.usersIgnore,
                    pathFilterType = push.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    eventType = CodeEventType.PUSH,
                    // todo action
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(push.enable)
            )
        }

        triggerOn.tag?.let { tag ->
            elementQueue.add(
                CodeGitlabWebHookTriggerElement(
                    tagName = tag.tags.nonEmptyOrNull()?.join(),
                    excludeTagName = tag.tagsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = tag.users,
                    excludeUsers = tag.usersIgnore,
                    eventType = CodeEventType.TAG_PUSH,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(tag.enable)
            )
        }

        triggerOn.mr?.let { mr ->
            elementQueue.add(
                CodeGitlabWebHookTriggerElement(
                    branchName = mr.targetBranches.nonEmptyOrNull()?.join(),
                    excludeBranchName = mr.targetBranchesIgnore.nonEmptyOrNull()?.join(),
                    includeSourceBranchName = mr.sourceBranches.nonEmptyOrNull()?.join(),
                    excludeSourceBranchName = mr.sourceBranchesIgnore.nonEmptyOrNull()?.join(),
                    includePaths = mr.paths.nonEmptyOrNull()?.join(),
                    excludePaths = mr.pathsIgnore.nonEmptyOrNull()?.join(),
                    includeUsers = mr.users,
                    excludeUsers = mr.usersIgnore,
                    block = mr.blockMr,
                    pathFilterType = mr.pathFilterType?.let { PathFilterType.valueOf(it) }
                        ?: PathFilterType.NamePrefixFilter,
                    // todo action
                    eventType = CodeEventType.MERGE_REQUEST,
                    repositoryType = repositoryType,
                    repositoryName = triggerOn.repoName
                ).checkTriggerElementEnable(mr.enable)
            )
        }
    }

    private fun Element.checkTriggerElementEnable(enabled: Boolean?): Element {
        if (additionalOptions == null) {
            additionalOptions = ElementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS)
        }
        additionalOptions!!.enable = enabled ?: true
        return this
    }

    private fun List<String>.join() = this.joinToString(separator = ",")

    private fun String.disjoin() = this.split(",")

    private fun List<String>?.nonEmptyOrNull() = this?.ifEmpty { null }
}

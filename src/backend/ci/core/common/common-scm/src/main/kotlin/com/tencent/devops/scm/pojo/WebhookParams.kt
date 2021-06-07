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

package com.tencent.devops.scm.pojo

const val BK_CI_RUN = "BK_CI_RUN"
const val BK_CI_REF = "BK_CI_REF"
const val BK_CI_REPO_OWNER = "BK_CI_REPO_OWNER"
const val BK_CI_REPOSITORY = "BK_CI_REPOSITORY"

const val BK_REPO_WEBHOOK_REPO_TYPE = "BK_CI_REPO_WEBHOOK_REPO_TYPE"
const val BK_REPO_WEBHOOK_REPO_URL = "BK_CI_REPO_WEBHOOK_REPO_URL"
const val BK_REPO_WEBHOOK_REPO_NAME = "BK_CI_REPO_WEBHOOK_NAME"
const val BK_REPO_WEBHOOK_REPO_ALIAS_NAME = "BK_CI_REPO_WEBHOOK_ALIAS_NAME"
const val BK_REPO_WEBHOOK_HASH_ID = "BK_CI_REPO_WEB_HOOK_HASHID"

const val BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE = "BK_CI_REPO_GIT_WEBHOOK_COMMIT_MESSAGE"
const val BK_REPO_GIT_WEBHOOK_COMMIT_ID = "BK_CI_REPO_GIT_WEBHOOK_COMMITID"
const val BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT = "BK_CI_REPO_GIT_WEBHOOK_COMMITID_SHORT"
const val BK_REPO_GIT_WEBHOOK_EVENT_TYPE = "BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE"
const val BK_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS = "BK_CI_REPO_GIT_WEBHOOK_INCLUDE_BRANCH"
const val BK_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS = "BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_BRANCH"
const val BK_REPO_GIT_WEBHOOK_INCLUDE_PATHS = "BK_CI_REPO_GIT_WEBHOOK_INCLUDE_PATHS"
const val BK_REPO_GIT_WEBHOOK_EXCLUDE_PATHS = "BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_PATHS"
const val BK_REPO_GIT_WEBHOOK_EXCLUDE_USERS = "BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_USERS"
const val BK_REPO_GIT_WEBHOOK_BRANCH = "BK_CI_REPO_GIT_WEBHOOK_BRANCH"
const val BK_REPO_GIT_MANUAL_UNLOCK = "BK_CI_REPO_GIT_MANUAL_UNLOCK"

const val BK_REPO_GIT_WEBHOOK_PUSH_USERNAME = "BK_CI_REPO_GIT_WEBHOOK_PUSH_USERNAME"
const val BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT = "BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT"
const val BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT = "BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT"
const val BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT = "BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT"
const val BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_"
const val BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_"
const val BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_TIMESTAMP_"
const val BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_AUTHOR_"
const val BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_"
const val BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_"
const val BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_PREFIX = "BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_"
const val BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT = "BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT"
const val BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT = "BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT"
const val BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT = "BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT"
const val BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND = "BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND"
const val BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND = "BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND"

const val BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH = "BK_CI_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH"
const val BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_PATH = "BK_CI_GIT_WEBHOOK_FINAL_INCLUDE_PATH"

const val BK_REPO_GIT_WEBHOOK_MR_AUTHOR = "BK_CI_REPO_GIT_WEBHOOK_MR_AUTHOR"
const val BK_REPO_GIT_WEBHOOK_MR_ACTION = "BK_CI_REPO_GIT_WEBHOOK_MR_ACTION"
const val BK_REPO_GIT_WEBHOOK_MR_TARGET_URL = "BK_CI_REPO_GIT_WEBHOOK_TARGET_URL"
const val BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL = "BK_CI_REPO_GIT_WEBHOOK_SOURCE_URL"
const val BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH = "BK_CI_REPO_GIT_WEBHOOK_TARGET_BRANCH"
const val BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH = "BK_CI_REPO_GIT_WEBHOOK_SOURCE_BRANCH"
const val BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME = "BK_CI_REPO_GIT_WEBHOOK_MR_CREATE_TIME"
const val BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME = "BK_CI_REPO_GIT_WEBHOOK_MR_UPDATE_TIME"
const val BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP = "BK_CI_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP"
const val BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP = "BK_CI_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP"
const val BK_REPO_GIT_WEBHOOK_MR_ID = "BK_CI_REPO_GIT_WEBHOOK_MR_ID"
const val BK_REPO_GIT_WEBHOOK_MR_NUMBER = "BK_CI_REPO_GIT_WEBHOOK_MR_NUMBER"
const val BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION = "BK_CI_REPO_GIT_WEBHOOK_MR_DESC"
const val BK_REPO_GIT_WEBHOOK_MR_TITLE = "BK_CI_REPO_GIT_WEBHOOK_MR_TITLE"
const val BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE = "BK_CI_REPO_GIT_WEBHOOK_MR_ASSIGNEE"
const val BK_REPO_GIT_WEBHOOK_MR_URL = "BK_CI_REPO_GIT_WEBHOOK_MR_URL"
const val BK_REPO_GIT_WEBHOOK_MR_REVIEWERS = "BK_CI_REPO_GIT_WEBHOOK_MR_REVIEWERS"
const val BK_REPO_GIT_WEBHOOK_MR_MILESTONE = "BK_CI_REPO_GIT_WEBHOOK_MR_MILESTONE"
const val BK_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE = "BK_CI_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE"
const val BK_REPO_GIT_WEBHOOK_MR_LABELS = "BK_CI_REPO_GIT_WEBHOOK_MR_LABELS"
const val BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT = "BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT"
const val BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG = "BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG"

const val BK_REPO_GIT_WEBHOOK_TAG_NAME = "BK_CI_REPO_GIT_WEBHOOK_TAG_NAME"
const val BK_REPO_GIT_WEBHOOK_TAG_OPERATION = "BK_CI_REPO_GIT_WEBHOOK_TAG_OPERATION"
const val BK_REPO_GIT_WEBHOOK_TAG_USERNAME = "BK_CI_REPO_GIT_WEBHOOK_TAG_USERNAME"
const val BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM = "BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM"

const val BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME = "BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME"
const val BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE = "BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE"
const val BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME = "BK_CI_REPO_GITHUB_WEBHOOK_CREATE_USERNAME"

const val BK_REPO_SVN_WEBHOOK_REVERSION = "BK_CI_REPO_SVN_WEBHOOK_REVERSION"
const val BK_REPO_SVN_WEBHOOK_USERNAME = "BK_CI_REPO_SVN_WEBHOOK_USERNAME"
const val BK_REPO_SVN_WEBHOOK_COMMIT_TIME = "BK_CI_REPO_SVN_WEBHOOK_COMMIT_TIME"
const val BK_REPO_SVN_WEBHOOK_RELATIVE_PATH = "BK_CI_REPO_SVN_WEBHOOK_INCLUDE_PATHS"
const val BK_REPO_SVN_WEBHOOK_EXCLUDE_PATHS = "BK_CI_REPO_SVN_WEBHOOK_EXCLUDE_PATHS"
const val BK_REPO_SVN_WEBHOOK_INCLUDE_USERS = "BK_CI_REPO_SVN_WEBHOOK_INCLUDE_USERS"
const val BK_REPO_SVN_WEBHOOK_EXCLUDE_USERS = "BK_CI_REPO_SVN_WEBHOOK_EXCLUDE_USERS"

const val PIPELINE_WEBHOOK_MR_ID = "BK_CI_HOOK_MR_ID" // bk_hookMergeRequestId
const val PIPELINE_WEBHOOK_MR_COMMITTER = "BK_CI_HOOK_MR_COMMITTER" // "bk_hookMergeRequest_committer"
const val PIPELINE_WEBHOOK_SOURCE_BRANCH = "BK_CI_HOOK_SOURCE_BRANCH" // hookSourceBranch
const val PIPELINE_WEBHOOK_TARGET_BRANCH = "BK_CI_HOOK_TARGET_BRANCH" // hookTargetBranch
const val PIPELINE_WEBHOOK_SOURCE_PROJECT_ID = "BK_CI_HOOK_SOURCE_PROJECT_ID"
const val PIPELINE_WEBHOOK_TARGET_PROJECT_ID = "BK_CI_HOOK_TARGET_PROJECT_ID"
const val PIPELINE_WEBHOOK_SOURCE_REPO_NAME = "BK_CI_HOOK_SOURCE_REPO_NAME"
const val PIPELINE_WEBHOOK_TARGET_REPO_NAME = "BK_CI_HOOK_TARGET_REPO_NAME"
const val MATCH_BRANCH = "matchBranch"
const val MATCH_PATHS = "matchPaths"

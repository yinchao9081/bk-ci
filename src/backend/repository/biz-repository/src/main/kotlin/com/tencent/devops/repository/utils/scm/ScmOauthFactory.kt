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

package com.tencent.devops.repository.utils.scm

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.scm.IScm
import com.tencent.devops.repository.iscm.CodeGitScmOauthImpl
import com.tencent.devops.repository.iscm.CodeGitlabScmImpl
import com.tencent.devops.repository.iscm.CodeSvnScmImpl
import com.tencent.devops.repository.config.GitConfig
import com.tencent.devops.repository.config.SVNConfig

object ScmOauthFactory {

    fun getScm(
            projectName: String,
            url: String,
            type: ScmType,
            branchName: String?,
            privateKey: String?,
            passPhrase: String?,
            token: String?,
            region: CodeSvnRegion?,
            userName: String?,
            event: String?
    ): IScm {
        return when (type) {
            ScmType.CODE_SVN -> {
                if (region == null) {
                    throw RuntimeException("The svn region is null")
                }

                if (userName == null) {
                    throw RuntimeException("The svn username is null")
                }

                if (privateKey == null) {
                    throw RuntimeException("The svn private key is null")
                }
                val svnConfig = SpringContextUtil.getBean(SVNConfig::class.java)
                CodeSvnScmImpl(projectName,
                        branchName,
                        url,
                        userName,
                        privateKey,
                        passPhrase,
                        svnConfig)
            }
            ScmType.CODE_GIT -> {
                if (token == null) {
                    throw RuntimeException("The git token is null")
                }
                val gitConfig = SpringContextUtil.getBean(GitConfig::class.java)
                CodeGitScmOauthImpl(projectName, branchName, url, privateKey, passPhrase, token, gitConfig, event)
            }
            ScmType.CODE_GITLAB -> {
                if (token == null) {
                    throw RuntimeException("The gitlab access token is null")
                }
                val gitConfig = SpringContextUtil.getBean(GitConfig::class.java)
                CodeGitlabScmImpl(projectName, branchName, url, token, gitConfig)
            }
            else -> throw RuntimeException("Unknown repo($type)")
        }
    }
}
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

package com.tencent.devops.scm.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_GIT_CI"], description = "Service Code GIT CI resource")
@Path("/service/gitci/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGitCiResource {

    @ApiOperation("获取项目的token")
    @GET
    @Path("/getToken")
    fun getToken(
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitToken>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/checkUserGitAuth")
    fun checkUserGitAuth(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<Boolean>

    @ApiOperation("获取项目的token")
    @DELETE
    @Path("/clearToken")
    fun clearToken(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String
    ): Result<Boolean>

    @ApiOperation("获取GitCode项目成员信息")
    @GET
    @Path("/getMembers")
    fun getMembers(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @ApiParam(value = "搜索用户关键字", required = false)
        @QueryParam("search")
        search: String?
    ): Result<List<GitMember>>

    @ApiOperation("获取项目分支信息")
    @GET
    @Path("/getBranches")
    fun getBranches(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @ApiParam(value = "搜索用户关键字", required = true)
        @QueryParam("search")
        search: String?,
        @ApiParam(value = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("orderBy")
        orderBy: GitCodeBranchesOrder?,
        @ApiParam(value = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("sort")
        sort: GitCodeBranchesSort?
    ): Result<List<String>>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/getUserId")
    fun getGitUserId(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        rtxUserId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<String?>

    @ApiOperation("获取指定项目详细信息")
    @GET
    @Path("/getProjectInfo")
    fun getProjectInfo(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam("是否使用accessToken", required = true)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean
    ): Result<GitCIProjectInfo?>

    @ApiOperation("获取git文件内容")
    @GET
    @Path("/gitci/getGitCIFileContent")
    fun getGitCIFileContent(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String,
        @ApiParam("是否使用accessToken", required = true)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean
    ): Result<String>

    @ApiOperation("获取工蜂项目详细信息(使用超级token)")
    @GET
    @Path("/getGitCodeProjectInfo")
    fun getGitCodeProjectInfo(
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitCodeProjectInfo?>

    @ApiOperation("查询合并请求的代码变更")
    @GET
    @Path("getMergeRequestChangeInfo")
    fun getMergeRequestChangeInfo(
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrChangeInfo?>
}

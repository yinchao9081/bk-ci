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
 *
 */

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.manager.vo.ManagerGroupMemberVo
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionRoleMemberImpl
import com.tencent.devops.common.client.Client
import org.jvnet.hk2.annotations.Service
import org.springframework.beans.factory.annotation.Autowired

@Service
class TxPermissionRoleMemberImpl @Autowired constructor(
    override val iamManagerService: ManagerService,
    private val permissionGradeService: PermissionGradeService,
    private val client: Client
) : AbsPermissionRoleMemberImpl(iamManagerService, permissionGradeService) {
    override fun createRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        members: List<String>,
        managerGroup: Boolean
    ) {
        super.createRoleMember(userId, projectId, roleId, members, managerGroup)
    }

    override fun deleteRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        members: List<String>,
        managerGroup: Boolean
    ) {
        super.deleteRoleMember(userId, projectId, roleId, members, managerGroup)
    }

    override fun getRoleMember(projectId: Int, roleId: Int): ManagerGroupMemberVo {
        return super.getRoleMember(projectId, roleId)
    }

    override fun checkUser(userId: String) {
        return
    }
}

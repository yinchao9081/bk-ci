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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("ATOM_HANDLE_BUILD_RESULT")
class AtomHandleBuildResultServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomDao: MarketAtomDao,
    private val marketAtomService: MarketAtomService
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(AtomHandleBuildResultServiceImpl::class.java)

    override fun handleStoreBuildResult(storeBuildResultRequest: StoreBuildResultRequest): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val atomId = storeBuildResultRequest.storeId
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId)
        logger.info("handleStoreBuildResult atomRecord is:$atomRecord")
        if (null == atomRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomId))
        }
        var atomStatus = AtomStatusEnum.TESTING // 构建成功将插件状态置位测试状态
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            atomStatus = AtomStatusEnum.BUILD_FAIL // 构建失败
        }
        marketAtomService.setAtomBuildStatusByAtomCode(
            atomCode = atomRecord.atomCode,
            version = atomRecord.version,
            userId = storeBuildResultRequest.userId,
            atomStatus = atomStatus,
            msg = null
        )
        return Result(true)
    }
}

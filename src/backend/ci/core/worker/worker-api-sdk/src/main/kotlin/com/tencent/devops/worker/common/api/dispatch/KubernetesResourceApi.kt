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

package com.tencent.devops.worker.common.api.dispatch

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import io.fabric8.kubernetes.api.model.apps.Deployment
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class KubernetesResourceApi : AbstractBuildResourceApi(), KubernetesSDKApi {

    override fun deployApp(userId: String, deployAppJsonStr: String): Result<Boolean> {
        val path = "/ms/dispatch/api/build/kubernetes/deploy/app"
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            deployAppJsonStr
        )
        val headMap = mapOf(AUTH_HEADER_USER_ID to userId)
        val request = buildPost(path, body, headMap)
        val responseContent = request(request, "deploy app fail")
        return objectMapper.readValue(responseContent)
    }

    override fun getKubernetesDeploymentInfo(
        userId: String,
        namespaceName: String,
        deploymentName: String,
        apiUrl: String,
        token: String
    ): Result<Deployment> {
        val path = "/ms/dispatch/api/build/kubernetes/namespaces/$namespaceName/deployments/$deploymentName?" +
            "apiUrl=$apiUrl&token=$token"
        val headMap = mapOf(AUTH_HEADER_USER_ID to userId)
        val request = buildGet(path, headMap)
        val responseContent = request(request, "get deployment info fail")
        return objectMapper.readValue(responseContent)
    }
}

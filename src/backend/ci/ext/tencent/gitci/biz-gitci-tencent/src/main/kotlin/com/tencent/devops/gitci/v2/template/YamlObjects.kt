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

package com.tencent.devops.gitci.v2.template

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.Container
import com.tencent.devops.common.ci.v2.Credentials
import com.tencent.devops.common.ci.v2.Service
import com.tencent.devops.common.ci.v2.ServiceWith
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.Strategy
import com.tencent.devops.common.ci.v2.Variable

object YamlObjects {

    fun getVariable(variable: Map<String, Any>): Variable {
        return Variable(
            value = variable["value"]?.toString(),
            readonly = getNullValue("readonly", variable)?.toBoolean()
        )
    }

    fun getStep(step: Map<String, Any>): Step {
        return Step(
            name = step["name"]?.toString(),
            id = step["id"]?.toString(),
            ifFiled = step["if"]?.toString(),
            uses = step["uses"]?.toString(),
            with = if (step["with"] == null) {
                mapOf()
            } else {
                step["with"] as Map<String, Any>
            },
            timeoutMinutes = getNullValue("timeout-minutes", step)?.toInt(),
            continueOnError = getNullValue("continue-on-error", step)?.toBoolean(),
            retryTimes = step["retry-times"]?.toString(),
            env = step["env"]?.toString(),
            run = step["run"]?.toString()
        )
    }

    fun getService(service: Any): Map<String, Service> {
        val serviceMap = service as Map<String, Any?>
        val newServiceMap = mutableMapOf<String, Service>()
        serviceMap.forEach { key, value ->
            val with = (value as Map<String, Any>)["with"] as Map<String, Any>
            newServiceMap.putAll(
                mapOf(
                    key to Service(
                        image = getNotNullValue(key = "image", mapName = "Container", map = value),
                        with = ServiceWith(
                            password = getNotNullValue(key = "password", mapName = "with", map = with)
                        )
                    )
                )
            )
        }
        return newServiceMap
    }

    fun getContainer(container: Any): Container {
        val containerMap = container as Map<String, Any?>
        return Container(
            image = getNotNullValue(key = "image", mapName = "Container", map = containerMap),
            credentials = if (containerMap["credentials"] == null) {
                null
            } else {
                val credentialsMap = containerMap["credentials"] as Map<String, String>
                Credentials(
                    username = credentialsMap["username"]!!,
                    password = credentialsMap["password"]!!
                )
            }
        )
    }

    fun getStrategy(strategy: Any?): Strategy {
        val strategyMap = strategy as Map<String, Any?>
        return Strategy(
            matrix = strategyMap["matrix"],
            fastKill = getNullValue("fast-kill", strategyMap)?.toBoolean(),
            maxParallel = getNullValue("max-parallel", strategyMap)
        )
    }

    inline fun <reified T> getObjectFromYaml(path: String, template: String): T {
        return try {
            YamlUtil.getObjectMapper().readValue(template, T::class.java)
        } catch (e: Exception) {
            throw RuntimeException("$path wrong format！ ${e.message}")
        }
    }

    fun <T> transNullValue(key: String, map: Map<String, Any?>): T? {
        return if (map[key] == null) {
            null
        } else {
            map[key] as T
        }
    }

    fun getNullValue(key: String, map: Map<String, Any?>): String? {
        return if (map[key] == null) {
            null
        } else {
            map[key].toString()
        }
    }

    fun getNotNullValue(key: String, mapName: String, map: Map<String, Any?>): String {
        return if (map[key] == null) {
            throw RuntimeException("$mapName need $key")
        } else {
            map[key].toString()
        }
    }
}

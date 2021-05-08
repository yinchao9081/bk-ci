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

package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.VersionUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.experience.constant.ExperienceConditionEnum
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceLastDownloadDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.Executors

@Service
class ExperienceAppService(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val experienceDao: ExperienceDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceBaseService: ExperienceBaseService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceLastDownloadDao: ExperienceLastDownloadDao,
    private val client: Client
) {

    private val executorService = Executors.newFixedThreadPool(2)

    fun list(
        userId: String,
        offset: Int,
        limit: Int,
        groupByBundleId: Boolean,
        platform: Int? = null
    ): Pagination<AppExperience> {
        return experienceBaseService.list(
            userId = userId,
            offset = offset,
            limit = limit,
            groupByBundleId = groupByBundleId,
            platform = platform
        )
    }

    @SuppressWarnings("ComplexMethod")
    fun detail(userId: String, experienceHashId: String, platform: Int, appVersion: String?): AppExperienceDetail {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val isOldVersion = VersionUtil.compare(appVersion, "2.0.0") < 0

        val isPublic = experienceBaseService.isPublic(experienceId)
        val isInPrivate = experienceBaseService.isInPrivate(experienceId, userId)

        // 新版本且没权限
        if (!isOldVersion && !isPublic && !isInPrivate) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "没有权限访问资源",
                errorCode = ExperienceMessageCode.USER_NEED_EXP_X_PERMISSION
            )
        }

        val experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val bundleIdentifier = experience.bundleIdentifier
        val isExpired = DateUtil.isExpired(experience.endDate)
        val logoUrl = UrlUtil.toOuterPhotoAddr(experience.logoUrl)
        val projectName = experience.projectId
        val version = experience.version
        val shareUrl = experienceDownloadService.getQrCodeUrl(experienceHashId)
        val path = experience.artifactoryPath
        val artifactoryType = ArtifactoryType.valueOf(experience.artifactoryType)
        val experienceName =
            if (StringUtils.isBlank(experience.experienceName)) experience.projectId else experience.experienceName
        val versionTitle =
            if (StringUtils.isBlank(experience.versionTitle)) experience.name else experience.versionTitle
        val categoryId = if (experience.category < 0) ProductCategoryEnum.LIFE.id else experience.category
        val isPrivate = experienceBaseService.isPrivate(experienceId)
        val experienceCondition = getExperienceCondition(isPublic, isPrivate, isInPrivate)
        val lastDownloadMap = experienceBaseService.getLastDownloadMap(userId)

        val changeLog = if (isOldVersion) {
            getChangeLog(
                userId = userId,
                projectId = projectId,
                bundleIdentifier = bundleIdentifier,
                platform = null,
                page = 1,
                pageSize = 1000,
                isOldVersion = true
            )
        } else {
            emptyList() // 新版本使用changeLog接口
        }

        // 同步文件大小到数据表
        syncExperienceSize(experience, projectId, artifactoryType, path)

        return AppExperienceDetail(
            experienceHashId = experienceHashId,
            size = experience.size,
            logoUrl = logoUrl,
            shareUrl = shareUrl,
            name = projectName,
            packageName = experience.name,
            platform = PlatformEnum.valueOf(experience.platform),
            version = version,
            expired = isExpired,
            canExperience = isPublic || isInPrivate,
            online = experience.online,
            changeLog = changeLog,
            experienceName = experienceName,
            versionTitle = versionTitle,
            categoryId = categoryId,
            productOwner = objectMapper.readValue(experience.productOwner),
            createDate = experience.updateTime.let { if (isOldVersion) it.timestamp() else it.timestampmilli() },
            endDate = experience.endDate.let { if (isOldVersion) it.timestamp() else it.timestampmilli() },
            publicExperience = isPublic,
            remark = experience.remark,
            bundleIdentifier = experience.bundleIdentifier,
            experienceCondition = experienceCondition.id,
            appScheme = experience.scheme,
            lastDownloadHashId = lastDownloadMap[experience.projectId +
                    experience.bundleIdentifier +
                    experience.platform]
                ?.let { l -> HashUtil.encodeLongId(l) } ?: ""
        )
    }

    private fun getExperienceCondition(
        isPublic: Boolean,
        isPrivate: Boolean,
        isInPrivate: Boolean
    ): ExperienceConditionEnum {
        return if (isPublic && !isPrivate) {
            ExperienceConditionEnum.JUST_PUBLIC
        } else if (!isPublic && isPrivate) {
            ExperienceConditionEnum.JUST_PRIVATE
        } else if (isInPrivate) {
            ExperienceConditionEnum.BOTH_WITH_PRIVATE
        } else {
            ExperienceConditionEnum.BOTH_WITHOUT_PRIVATE
        }
    }

    fun changeLog(
        userId: String,
        experienceHashId: String,
        page: Int,
        pageSize: Int
    ): Pagination<ExperienceChangeLog> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val changeLog =
            getChangeLog(
                userId = userId,
                projectId = experience.projectId,
                bundleIdentifier = experience.bundleIdentifier,
                platform = experience.platform,
                page = if (page <= 0) 1 else page,
                pageSize = if (pageSize <= 0) 10 else pageSize,
                isOldVersion = false
            )
        val hasNext = if (changeLog.size < pageSize) {
            false
        } else {
            experienceDao.countByBundleIdentifier(
                dslContext,
                experience.projectId,
                experience.bundleIdentifier,
                experience.platform
            ) > page * pageSize
        }

        return Pagination(hasNext, changeLog)
    }

    private fun getChangeLog(
        userId: String,
        projectId: String,
        bundleIdentifier: String,
        platform: String?,
        page: Int,
        pageSize: Int,
        isOldVersion: Boolean
    ): List<ExperienceChangeLog> {
        val recordIds = experienceBaseService.getRecordIdsByUserId(userId, GroupIdTypeEnum.JUST_PRIVATE)
        val now = LocalDateTime.now()
        val lastDownloadRecord = platform?.let {
            experienceLastDownloadDao.get(
                dslContext,
                userId = userId,
                bundleId = bundleIdentifier,
                projectId = projectId,
                platform = it
            )
        }

        val experienceList = experienceDao.listByBundleIdentifier(
            dslContext = dslContext,
            projectId = projectId,
            bundleIdentifier = bundleIdentifier,
            platform = platform,
            recordIds = recordIds,
            offset = (page - 1) * pageSize,
            limit = pageSize
        )

        return experienceList.map {
            ExperienceChangeLog(
                experienceHashId = HashUtil.encodeLongId(it.id),
                version = it.version,
                creator = it.creator,
                createDate = it.createTime.run { if (isOldVersion) timestamp() else timestampmilli() },
                changelog = it.remark ?: "",
                experienceName = it.experienceName,
                size = it.size,
                logoUrl = UrlUtil.toOuterPhotoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier,
                appScheme = it.scheme,
                expired = now.isAfter(it.endDate),
                lastDownloadHashId = lastDownloadRecord?.let { last ->
                    HashUtil.encodeLongId(last.lastDonwloadRecordId)
                } ?: "",
                versionTitle = it.versionTitle
            )
        }.toList()
    }

    private fun syncExperienceSize(
        experience: TExperienceRecord,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ) {
        if (experience.size == 0L) {
            executorService.submit {
                val fileDetail =
                    client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data
                if (null != fileDetail) {
                    experienceDao.updateSize(dslContext, experience.id, fileDetail.size)
                    experience.size = fileDetail.size
                }
            }
        }
    }

    fun downloadUrl(userId: String, experienceHashId: String): DownloadUrl {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        return experienceDownloadService.getExternalDownloadUrl(userId, experienceId)
    }

    fun history(userId: String, appVersion: String?, projectId: String): List<AppExperienceSummary> {
        val expireTime = DateUtil.today()
        val experienceList = experienceDao.list(dslContext, projectId, null, null)

        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw RuntimeException("ProjectId $projectId cannot find.")
        val logoUrl = UrlUtil.toOuterPhotoAddr(projectInfo.logoAddr)

        val recordIds = experienceBaseService.getRecordIdsByUserId(userId, GroupIdTypeEnum.ALL)
        val isOldVersion = VersionUtil.compare(appVersion, "2.0.0") < 0

        val appExperienceSummaryList = experienceList.map {
            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = recordIds.contains(it.id) || it.creator == userId

            AppExperienceSummary(
                experienceHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                platform = PlatformEnum.valueOf(it.platform),
                version = it.version,
                remark = it.remark ?: "",
                expireDate = it.endDate.run { if (isOldVersion) timestamp() else timestampmilli() },
                source = Source.valueOf(it.source),
                logoUrl = logoUrl,
                creator = it.creator,
                expired = isExpired,
                canExperience = canExperience,
                online = it.online
            )
        }
        return appExperienceSummaryList.filter { appExperienceSummary ->
            appExperienceSummary.canExperience && !appExperienceSummary.expired
        }
    }
}

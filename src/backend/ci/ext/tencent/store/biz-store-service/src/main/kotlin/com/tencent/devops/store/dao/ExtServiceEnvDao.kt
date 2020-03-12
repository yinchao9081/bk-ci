package com.tencent.devops.store.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.model.store.tables.records.TExtensionServiceEnvInfoRecord
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.ExtServiceEnvUpdateInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceEnvDao {
    fun create(
        dslContext: DSLContext,
        extServiceEnvCreateInfo: ExtServiceEnvCreateInfo
    ) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                LANGUAGE,
                PKG_PATH,
                PKG_SHA_CONTENT,
                DOCKER_FILE_CONTENT,
                IMAGE_PATH,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    UUIDUtil.generate(),
                    extServiceEnvCreateInfo.serviceId,
                    extServiceEnvCreateInfo.language,
                    extServiceEnvCreateInfo.pkgPath,
                    extServiceEnvCreateInfo.pkgShaContent,
                    extServiceEnvCreateInfo.dockerFileContent,
                    extServiceEnvCreateInfo.imagePath,
                    extServiceEnvCreateInfo.creatorUser,
                    extServiceEnvCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceEnvInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceEnvUpdateInfo: ExtServiceEnvUpdateInfo
    ) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            val baseStep = dslContext.update(this)
            val language = extServiceEnvUpdateInfo.language
            if (null != language) {
                baseStep.set(LANGUAGE, language)
            }
            val pkgPath = extServiceEnvUpdateInfo.pkgPath
            if (null != pkgPath) {
                baseStep.set(PKG_PATH, pkgPath)
            }
            val pkgShaContent = extServiceEnvUpdateInfo.pkgShaContent
            if (null != pkgShaContent) {
                baseStep.set(PKG_SHA_CONTENT, pkgShaContent)
            }
            val dockerFileContent = extServiceEnvUpdateInfo.dockerFileContent
            if (null != dockerFileContent) {
                baseStep.set(DOCKER_FILE_CONTENT, dockerFileContent)
            }
            val imagePath = extServiceEnvUpdateInfo.imagePath
            if (null != imagePath) {
                baseStep.set(IMAGE_PATH, imagePath)
            }
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun getMarketServiceEnvInfoByServiceId(dslContext: DSLContext, serviceId: String): TExtensionServiceEnvInfoRecord? {
        return with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            dslContext.selectFrom(this)
                .where(SERVICE_ID.eq(serviceId))
                .fetchOne()
        }
    }
}
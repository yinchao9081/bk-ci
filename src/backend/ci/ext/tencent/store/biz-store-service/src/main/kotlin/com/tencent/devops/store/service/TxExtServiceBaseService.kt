package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.dispatch.pojo.AppDeployment
import com.tencent.devops.dispatch.pojo.AppIngress
import com.tencent.devops.dispatch.pojo.AppService
import com.tencent.devops.dispatch.pojo.DeployApp
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceExtServiceBuildPipelineInitResource
import com.tencent.devops.process.pojo.pipeline.ExtServiceBuildInitPipelineReq
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.config.ExtServiceBcsConfig
import com.tencent.devops.store.config.ExtServiceBcsNameSpaceConfig
import com.tencent.devops.store.config.ExtServiceDeploymentConfig
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import com.tencent.devops.store.config.ExtServiceIngressConfig
import com.tencent.devops.store.config.ExtServiceServiceConfig
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceBuildAppRelDao
import com.tencent.devops.store.dao.ExtServiceBuildInfoDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ExtServiceBaseInfoDTO
import com.tencent.devops.store.pojo.dto.ExtServiceImageInfoDTO
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.MessageFormat

@Service
class TxExtServiceBaseService : ExtServiceBaseService() {

    @Value("\${git.service.nameSpaceId}")
    private lateinit var serviceNameSpaceId: String

    @Autowired
    private lateinit var storePipelineRelDao: StorePipelineRelDao

    @Autowired
    private lateinit var extServiceBuildInfoDao: ExtServiceBuildInfoDao

    @Autowired
    private lateinit var extServiceBuildAppRelDao: ExtServiceBuildAppRelDao

    @Autowired
    private lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao

    @Autowired
    private lateinit var extServiceBcsConfig: ExtServiceBcsConfig

    @Autowired
    private lateinit var extServiceBcsNameSpaceConfig: ExtServiceBcsNameSpaceConfig

    @Autowired
    private lateinit var extServiceImageSecretConfig: ExtServiceImageSecretConfig

    @Autowired
    private lateinit var extServiceDeploymentConfig: ExtServiceDeploymentConfig

    @Autowired
    private lateinit var extServiceServiceConfig: ExtServiceServiceConfig

    @Autowired
    private lateinit var extServiceIngressConfig: ExtServiceIngressConfig

    override fun handleServicePackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleServicePackage marketServiceCreateRequest is:$extensionInfo,serviceCode is:$serviceCode,userId is:$userId")
        extensionInfo.authType ?: return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf("authType"),
            null
        )
        extensionInfo.visibilityLevel ?: return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf("visibilityLevel"),
            null
        )
        val repositoryInfo: RepositoryInfo?
        if (extensionInfo.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (extensionInfo.privateReason.isNullOrBlank()) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_NULL,
                    arrayOf("privateReason"),
                    null
                )
            }
        }
        // 远程调工蜂接口创建代码库
        try {
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId,
                extensionInfo.projectCode,
                serviceCode,
                storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext,
                    extensionInfo.language!!,
                    StoreTypeEnum.SERVICE
                ).sampleProjectPath,
                serviceNameSpaceId.toInt(),
                extensionInfo.visibilityLevel,
                TokenTypeEnum.PRIVATE_KEY
            )
            logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
            if (createGitRepositoryResult.isOk()) {
                repositoryInfo = createGitRepositoryResult.data
            } else {
                return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, null)
            }
        } catch (e: Exception) {
            logger.error("createGitCodeRepository error  is :$e", e)
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        if (null == repositoryInfo) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        return Result(mapOf("repositoryHashId" to repositoryInfo.repositoryHashId!!, "codeSrc" to repositoryInfo.url))
    }

    override fun getExtServicePackageSourceType(serviceCode: String): ExtServicePackageSourceTypeEnum {
        // 内部版暂时只支持代码库打包的方式，后续支持用户传可执行包的方式
        return ExtServicePackageSourceTypeEnum.REPO
    }

    override fun getRepositoryInfo(projectCode: String?, repositoryHashId: String?): Result<Repository?> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun asyncHandleUpdateService(context: DSLContext, serviceId: String, userId: String) {
        runPipeline(context, serviceId, userId)
    }

    private fun runPipeline(context: DSLContext, serviceId: String, userId: String): Boolean {
        val serviceRecord = extServiceDao.getServiceById(context, serviceId) ?: return false
        val serviceCode = serviceRecord.serviceCode
        val version = serviceRecord.version
        val servicePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(context, serviceCode, StoreTypeEnum.SERVICE)
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            context,
            serviceCode,
            StoreTypeEnum.SERVICE.type.toByte()
        ) // 查找新增扩展服务时关联的项目
        val buildInfo = extServiceBuildInfoDao.getServiceBuildInfo(context, serviceId)
        logger.info("the buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        val repoAddr = extServiceImageSecretConfig.repoRegistryUrl
        val imageName = "${extServiceImageSecretConfig.imageNamePrefix}$serviceCode"
        val extServiceImageInfo = ExtServiceImageInfoDTO(
            imageName = imageName,
            imageTag = version,
            repoAddr = repoAddr,
            username = extServiceImageSecretConfig.repoUsername,
            password = extServiceImageSecretConfig.repoPassword
        )
        val deployApp = DeployApp(
            bcsUrl = extServiceBcsConfig.masterUrl,
            token = extServiceBcsConfig.token,
            namespaceName = "${extServiceBcsNameSpaceConfig.namespaceName}-prepare",
            appCode = serviceCode,
            appDeployment = AppDeployment(
                replicas = extServiceDeploymentConfig.replicas.toInt(),
                image = "$repoAddr/$imageName:$version",
                pullImageSecretName = "${extServiceDeploymentConfig.pullImageSecretName}-prepare",
                containerPort = extServiceDeploymentConfig.containerPort.toInt()
            ),
            appService = AppService(
                servicePort = extServiceServiceConfig.servicePort.toInt()
            ),
            appIngress = AppIngress(
                host = MessageFormat(extServiceIngressConfig.host).format(arrayOf(serviceCode)),
                contextPath = extServiceIngressConfig.contextPath,
                ingressAnnotationMap = mapOf(
                    "kubernetes.io/ingress.class" to extServiceIngressConfig.annotationClass,
                    "kubernetes.io/ingress.subnetId" to extServiceIngressConfig.annotationSubnetId
                )
            )
        )
        if (null == servicePipelineRelRecord) {
            // 为用户初始化构建流水线并触发执行
            val serviceBaseInfo = ExtServiceBaseInfoDTO(
                serviceId = serviceId,
                serviceCode = serviceCode,
                version = serviceRecord.version,
                extServiceImageInfo = extServiceImageInfo,
                extServiceDeployInfo = deployApp
            )
            val serviceBuildAppInfoRecords = extServiceBuildAppRelDao.getExtServiceBuildAppInfo(context, serviceId)
            val buildEnv = mutableMapOf<String, String>()
            serviceBuildAppInfoRecords?.forEach {
                buildEnv[it["appName"] as String] = it["appVersion"] as String
            }
            val extServiceFeature = extFeatureDao.getServiceByCode(context, serviceCode)!!
            val extServiceBuildInitPipelineReq = ExtServiceBuildInitPipelineReq(
                repositoryHashId = extServiceFeature.repositoryHashId,
                repositoryPath = buildInfo.value2(),
                script = script,
                extServiceBaseInfo = serviceBaseInfo,
                buildEnv = buildEnv
            )
            val serviceMarketInitPipelineResp = client.get(ServiceExtServiceBuildPipelineInitResource::class)
                .initExtServiceBuildPipeline(userId, projectCode!!, extServiceBuildInitPipelineReq).data
            logger.info("the serviceMarketInitPipelineResp is:$serviceMarketInitPipelineResp")
            if (null != serviceMarketInitPipelineResp) {
                storePipelineRelDao.add(context, serviceCode, StoreTypeEnum.SERVICE, serviceMarketInitPipelineResp.pipelineId)
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = serviceMarketInitPipelineResp.extServiceStatus.status.toByte(),
                    userId = userId,
                    msg = null
                )
                val buildId = serviceMarketInitPipelineResp.buildId
                if (null != buildId) {
                    storePipelineBuildRelDao.add(context, serviceId, serviceMarketInitPipelineResp.pipelineId, buildId)
                }
            }
        } else {
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams["serviceCode"] = serviceCode
            startParams["version"] = serviceRecord.version
            startParams["extServiceImageInfo"] = JsonUtil.toJson(extServiceImageInfo)
            startParams["extServiceDeployInfo"] = JsonUtil.toJson(deployApp)
            startParams["script"] = script
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, projectCode!!, servicePipelineRelRecord.pipelineId, startParams,
                ChannelCode.AM
            ).data
            logger.info("the buildIdObj is:$buildIdObj")
            if (null != buildIdObj) {
                storePipelineBuildRelDao.add(context, serviceId, servicePipelineRelRecord.pipelineId, buildIdObj.id)
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = ExtServiceStatusEnum.BUILDING.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建中
            } else {
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = ExtServiceStatusEnum.BUILD_FAIL.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建失败
            }
        }
        return true
    }
}
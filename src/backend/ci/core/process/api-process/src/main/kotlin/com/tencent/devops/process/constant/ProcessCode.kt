package com.tencent.devops.process.constant

object ProcessCode {
    const val BK_ETH1_NETWORK_CARD_IP_EMPTY = "BkEth1NetworkCardIpEmpty" // eth1 网卡Ip为空，因此，获取eth0的网卡ip
    const val BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY = "BkLoopbackAddressOrNicEmpty" // loopback地址或网卡名称为空
    const val BK_FAILED_GET_NETWORK_CARD = "BkFailedGetNetworkCard" // 获取网卡失败













    const val BK_INVALID_NOTIFICATION_RECIPIENT = "BkInvalidNotificationRecipient" // 通知接收者不合法:
    const val BK_WECOM_NOTICE = "BkWecomNotice" // 企业微信通知内容:
    const val BK_MOBILE_VIEW_DETAILS = "BkMobileViewDetails" // {0}\n\n 手机查看详情：{1} \n 电脑查看详情：{2}
    const val BK_SEND_WECOM_CONTENT = "BkSendWecomContent" // 发送企业微信内容: ({0}) 到 {1}
    const val BK_SEND_WECOM_CONTENT_SUCCESSFULLY = "BkSendWecomContentSuccessfully" // 发送企业微信内容: ({0}) 到 {1}成功
    const val BK_SEND_WECOM_CONTENT_FAILED = "BkSendWecomContentFailed" // 发送企业微信内容: ({0}) 到 {1}失败: {2}
    const val BK_MATCHING_FILE = "BkMatchingFile" // 匹配文件中:
    const val BK_UPLOAD_CORRESPONDING_FILE = "BkUploadCorrespondingFile" // 上传对应文件到织云成功!
    const val BK_START_UPLOADING_CORRESPONDING_FILES = "BkStartUploadingCorrespondingFiles" // 开始上传对应文件到织云...
    const val BK_INCORRECT_EXCEL_FORMAT = "BkIncorrectExcelFormat" // Excel格式错误，或文件不存在
    const val BK_ILLEGAL_MAXIMUM_QUEUE_LENGTH = "BkIllegalMaximumQueueLength" // 最大排队时长非法
    const val BK_ILLEGAL_MAXIMUM_NUMBER = "BkIllegalMaximumNumber" // 最大排队数量非法
    const val BK_TCLS_ENVIRONMENT_MESSAGE = "BkTclsEnvironmentMessage" // 获取 TCLS 环境失败，请检查用户名密码是否正确，错误信息：
    const val BK_TCLS_ENVIRONMENT = "BkTclsEnvironment" // 获取 TCLS 环境失败，请检查用户名密码是否正确
    const val BK_PIPELINE_STAGE_EMPTY = "BkPipelineStageEmpty" // 流水线Stage为空
    const val BK_PIPELINE_NOT_EXIST = "BkPipelineNotExist" // 流水线不存在
    const val BK_PIPELINE_CHOREOGRAPHY_NOT_EXIST = "BkPipelineChoreographyNotExist" // 流水线编排不存在
    const val BK_BUILD_TRIGGER = "BkBuildTrigger" // 构建触发
    const val BK_PULL_GIT_WAREHOUSE_CODE = "BkPullGitWarehouseCode" // 拉取Git仓库代码
    const val BK_USER_NOT_PERMISSION_DOWNLOAD = "BkUserNotPermissionDownload" // 用户({0})在工程({1})下没有流水线{2}下载构建权限
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED = "BkAutomaticExportNotSupported" // ### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###
    const val BK_BUILD_CLUSTERS_THROUGH = "BkBuildClustersThrough" // ### 可以通过 runs-on: macos-10.15 使用macOS公共构建集群。
    const val BK_NOTE_DEFAULT_XCODE_VERSION = "BkNoteDefaultXcodeVersion" // 注意默认的Xcode版本为12.2，若需自定义，请在JOB下自行执行 xcode-select 命令切换 ###
    const val BK_PLEASE_USE_STAGE_AUDIT = "BkPleaseUseStageAudit" // 人工审核插件请改用Stage审核 ###
    const val BK_PLUG_NOT_SUPPORTED = "BkPlugNotSupported" // # 注意：不支持插件【{0}({1})】的导出
    const val BK_FIND_RECOMMENDED_REPLACEMENT_PLUG = "BkFindRecommendedReplacementPlug" // 请在蓝盾研发商店查找推荐的替换插件！
    const val BK_OLD_PLUG_NOT_SUPPORT = "BkOldPlugNotSupport" // 内置老插件不支持导出，请使用市场插件 ###
    const val BK_VARIABLE_NAME_NOT_UNIQUE = "BkVariableNameNotUnique" // 变量名[{0}]来源不唯一，请修改变量名称或增加插件输出命名空间：
    const val BK_NO_RIGHT_EXPORT_PIPELINE = "BkNoRightExportPipeline" // 用户({0})无权限在工程({1})下导出流水线
    const val BK_PROJECT_ID = "BkProjectId" // # 项目ID:
    const val BK_PIPELINED_ID = "BkPipelinedId" // # 流水线ID:
    const val BK_PIPELINE_NAME = "BkPipelineName" // # 流水线名称:
    const val BK_EXPORT_TIME = "BkExportTime" // # 导出时间:
    const val BK_EXPORT_SYSTEM_CREDENTIALS = "BkExportSystemCredentials" // # 注意：不支持系统凭证(用户名、密码)的导出，请在stream项目设置下重新添加凭据：https://iwiki.woa.com/p/800638064 ！ \n
    const val BK_SENSITIVE_INFORMATION_IN_PARAMETERS = "BkSensitiveInformationInParameters" // # 注意：[插件]输入参数可能存在敏感信息，请仔细检查，谨慎分享！！！ \n
    const val BK_STREAM_NOT_SUPPORT = "BkStreamNotSupport" // # 注意：[插件]Stream不支持蓝盾老版本的插件，请在研发商店搜索新插件替换 \n
    const val BK_PARAMETERS_BE_EXPORTED = "BkParametersBeExported" // # \n# tips：部分参数导出会存在\[该字段限制导出，请手动填写]\,需要手动指定。原因有:\n
    const val BK_IDENTIFIED_SENSITIVE_INFORMATION = "BkIdentifiedSensitiveInformation" // # ①识别出为敏感信息，不支持导出\n
    const val BK_UNKNOWN_CONTEXT_EXISTS = "BkUnknownContextExists" // # ②部分字段校验格式时存在未知上下文，不支持导出\n
    const val BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE = "BkAutomaticExportNotSupportedImage" // ### 该镜像暂不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###
    const val BK_ENTER_URL_ADDRESS_IMAGE = "BkEnterUrlAddressImage" // ###请直接填入镜像(TLinux2.2公共镜像)的URL地址，若存在鉴权请增加 credentials 字段###
    const val BK_ADMINISTRATOR = "BkAdministrator" // 管理员
    const val BK_QUICK_APPROVAL_MOA = "BkQuickApprovalMoa" // 【通过MOA快速审批】
    const val BK_QUICK_APPROVAL_PC = "BkQuickApprovalPc" // 【通过PC快速审批】
    const val BK_NOT_CONFIRMED_CAN_EXECUTED = "BkNotConfirmedCanExecuted" // 插件 {0} 尚未确认是否可以在工蜂CI执行
    const val BK_CONTACT_PLUG_DEVELOPER = "BkContactPlugDeveloper" // ，请联系插件开发者
    const val BK_CHECK_INTEGRITY_YAML = "BkCheckIntegrityYaml" // 请检查YAML的完整性，或切换为研发商店推荐的插件后再导出
    const val BK_BEE_CI_NOT_SUPPORT = "BkBeeCiNotSupport" // 工蜂CI不支持蓝盾老版本插件
    const val BK_SEARCH_STORE = "BkSearchStore" // 请在研发商店搜索新插件替换
    const val BK_NOT_SUPPORT_CURRENT_CONSTRUCTION_MACHINE = "BkNotSupportCurrentConstructionMachine" // # 注意：工蜂CI暂不支持当前类型的构建机
    const val BK_EXPORT = "BkExport" // 的导出,
    const val BK_CHECK_POOL_FIELD = "BkCheckPoolField" // 需检查JOB({0})的Pool字段
    const val BK_CONSTRUCTION_MACHINE_NOT_SUPPORTED = "BkConstructionMachineNotSupported" // # 注意：暂不支持当前类型的构建机
    const val BK_NOT_EXIST_UNDER_NEW_BUSINESS = "BkNotExistUnderNewBusiness" //# 注意：【{0}】的环境【{1}】在新业务下可能不存在，
    const val BK_CHECK_OPERATING_SYSTEM_CORRECT = "BkCheckOperatingSystemCorrect" //请手动修改成存在的环境，并检查操作系统是否正确
    const val BK_NODE_NOT_EXIST_UNDER_NEW_BUSINESS = "BkNodeNotExistUnderNewBusiness" //# 注意：【{0}】的节点【{1}】在新业务下可能不存在，
    const val BK_PLEASE_MANUALLY_MODIFY = "BkPleaseManuallyModify" // 请手动修改成存在的节点
    const val BK_ONLY_VISIBLE_PCG_BUSINESS = "BkOnlyVisiblePcgBusiness" // # 注意：【{0}】仅对PCG业务可见，请检查当前业务是否属于PCG！ \n
    const val BK_WORKER_BEE_CI_NOT_SUPPORT = "BkWorkerBeeCiNotSupport" // # 注意：[插件]工蜂CI不支持依赖蓝盾项目的服务（如凭证、节点等），
    const val BK_MODIFICATION_GUIDELINES = "BkModificationGuidelines" // 请联系插件开发者改造插件，改造指引：https://iwiki.woa.com/x/CqARHg \n
    const val BK_CREATE_SERVICE = "BkCreateService" // 创建{0}服务
    const val BK_PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST = "BkPublicBuildResourcePoolNotExist" // 公共构建资源池不存在，请检查yml配置.
    const val BK_FAILED_GET_USER_INFORMATION = "BkFailedGetUserInformation" // 获取用户{0} 信息失败

    }
package com.tencent.devops.external.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表蓝盾平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-蓝盾环境 06：experience-版本体验 07：image-镜像 08：log-蓝盾日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-蓝盾支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22：external-外部）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 * @since: 2019-03-05
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ExternalMessageCode {
    const val PARAMETER_ERROR = "2122001"//参数错误
    const val GITHUB_AUTHENTICATION_FAILED = "2122002"//GitHub认证失败
    const val ACCOUNT_NOT_PERMISSIO = "2122003"//账户没有{0}的权限
    const val GITHUB_WAREHOUSE_NOT_EXIST = "2122004"//GitHub仓库不存在或者是账户没有该项目{0}的权限
    const val GITHUB_PLATFORM_FAILED = "2122005"//GitHub平台{0}失败





}
USE devops_ci_environment;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_ENV
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ENV` (
  `ENV_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `ENV_NAME` varchar(128) NOT NULL COMMENT '环境名称',
  `ENV_DESC` varchar(128) NOT NULL COMMENT '环境描述',
  `ENV_TYPE` varchar(128) NOT NULL COMMENT '环境类型（开发环境{DEV}|测试环境{TEST}|构建环境{BUILD}）',
  `ENV_VARS` text NOT NULL COMMENT '环境变量',
  `CREATED_USER` varchar(64) NOT NULL COMMENT '创建人',
  `UPDATED_USER` varchar(64) NOT NULL COMMENT '修改人',
  `CREATED_TIME` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATED_TIME` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `IS_DELETED` bit(1) NOT NULL COMMENT '是否删除',
  PRIMARY KEY (`ENV_ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环境信息表';

-- ----------------------------
-- Table structure for T_ENVIRONMENT_AGENT_PIPELINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ENVIRONMENT_AGENT_PIPELINE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `AGENT_ID` bigint(20) NOT NULL COMMENT '构建机ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `USER_ID` varchar(32) NOT NULL COMMENT '用户ID',
  `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '更新时间',
  `STATUS` int(11) NOT NULL COMMENT '状态',
  `PIPELINE` varchar(1024) NOT NULL COMMENT 'Pipeline Type',
  `RESPONSE` text COMMENT '',
  PRIMARY KEY (`ID`),
  KEY `AGENT_ID` (`AGENT_ID`),
  KEY `STATUS` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_ENVIRONMENT_SLAVE_GATEWAY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ENVIRONMENT_SLAVE_GATEWAY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NAME` varchar(32) NOT NULL COMMENT '名称',
  `SHOW_NAME` varchar(32) NOT NULL COMMENT '展示名称',
  `GATEWAY` varchar(127) DEFAULT '' COMMENT '网关地址',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_ENVIRONMENT_THIRDPARTY_AGENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ENVIRONMENT_THIRDPARTY_AGENT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NODE_ID` bigint(20) DEFAULT NULL COMMENT '节点ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `HOSTNAME` varchar(128) DEFAULT '' COMMENT '主机名称',
  `IP` varchar(64) DEFAULT '' COMMENT 'ip地址',
  `OS` varchar(16) NOT NULL COMMENT '操作系统',
  `DETECT_OS` varchar(128) DEFAULT '' COMMENT '检测操作系统',
  `STATUS` int(11) NOT NULL COMMENT '状态',
  `SECRET_KEY` varchar(256) NOT NULL COMMENT '密钥',
  `CREATED_USER` varchar(64) NOT NULL COMMENT '创建者',
  `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
  `START_REMOTE_IP` varchar(64) DEFAULT NULL COMMENT '主机IP',
  `GATEWAY` varchar(256) DEFAULT '' COMMENT '目标服务网关',
  `VERSION` varchar(128) DEFAULT NULL COMMENT '版本号',
  `MASTER_VERSION` varchar(128) DEFAULT NULL COMMENT '主版本',
  `PARALLEL_TASK_COUNT` int(11) DEFAULT NULL COMMENT '并行任务计数',
  `AGENT_INSTALL_PATH` varchar(512) DEFAULT NULL COMMENT '构建机安装路径',
  `STARTED_USER` varchar(64) DEFAULT NULL COMMENT '启动者',
  `AGENT_ENVS` text COMMENT '环境变量',
  `FILE_GATEWAY` varchar(256) DEFAULT '' COMMENT '文件网关路径',
  `AGENT_PROPS` text COMMENT 'agent config 配置项Json',
  PRIMARY KEY (`ID`),
  KEY `idx_agent_node` (`NODE_ID`),
  KEY `idx_agent_project` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方构建机agent信息表';

-- ----------------------------
-- Table structure for T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `AGENT_ID` bigint(20) NOT NULL COMMENT '构建机ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `ACTION` varchar(64) NOT NULL COMMENT '操作',
  `ACTION_TIME` datetime NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`ID`),
  KEY `AGENT_ID` (`AGENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_ENV_NODE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ENV_NODE` (
  `ENV_ID` bigint(20) NOT NULL COMMENT '环境ID',
  `NODE_ID` bigint(20) NOT NULL COMMENT '节点ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  PRIMARY KEY (`ENV_ID`,`NODE_ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环境-节点映射表';

-- ----------------------------
-- Table structure for T_NODE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NODE` (
  `NODE_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '节点ID 主键ID',
  `NODE_STRING_ID` varchar(32) DEFAULT NULL COMMENT '节点ID字符串',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `NODE_IP` varchar(64) NOT NULL COMMENT '节点IP',
  `NODE_NAME` varchar(64) NOT NULL COMMENT '节点名称',
  `NODE_STATUS` varchar(64) NOT NULL COMMENT '节点状态',
  `NODE_TYPE` varchar(64) NOT NULL COMMENT '节点类型',
  `NODE_CLUSTER_ID` varchar(128) DEFAULT NULL COMMENT '集群ID',
  `NODE_NAMESPACE` varchar(128) DEFAULT NULL COMMENT '节点命名空间',
  `CREATED_USER` varchar(64) NOT NULL COMMENT '创建者',
  `CREATED_TIME` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `EXPIRE_TIME` timestamp NULL DEFAULT NULL COMMENT '过期时间',
  `OS_NAME` varchar(128) DEFAULT NULL COMMENT '操作系统名称',
  `OPERATOR` varchar(256) DEFAULT NULL COMMENT '操作者',
  `BAK_OPERATOR` varchar(256) DEFAULT NULL COMMENT '备份责任人',
  `AGENT_STATUS` bit(1) DEFAULT NULL COMMENT '构建机状态',
  `DISPLAY_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '别名',
  `IMAGE` varchar(512) DEFAULT NULL COMMENT '镜像',
  `TASK_ID` bigint(20) DEFAULT NULL COMMENT '任务id',
  `LAST_MODIFY_TIME` timestamp NULL DEFAULT NULL COMMENT '最近修改时间',
  `LAST_MODIFY_USER` varchar(512) DEFAULT NULL COMMENT '最近修改者',
  `BIZ_ID` bigint(20) DEFAULT NULL COMMENT '所属业务',
  PRIMARY KEY (`NODE_ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点信息表';

-- ----------------------------
-- Table structure for T_PROJECT_CONFIG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_CONFIG` (
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `UPDATED_USER` varchar(64) NOT NULL COMMENT '修改者',
  `UPDATED_TIME` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `BCSVM_ENALBED` bit(1) NOT NULL DEFAULT b'0' COMMENT '',
  `BCSVM_QUOTA` int(11) NOT NULL DEFAULT '0' COMMENT '',
  `IMPORT_QUOTA` int(11) NOT NULL DEFAULT '30' COMMENT '',
  `DEV_CLOUD_ENALBED` bit(1) NOT NULL DEFAULT b'0' COMMENT '',
  `DEV_CLOUD_QUOTA` int(11) NOT NULL DEFAULT '0' COMMENT '',
  PRIMARY KEY (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_AGENT_FAILURE_NOTIFY_USER`
(
    `ID`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `USER_ID`      varchar(32) DEFAULT '' COMMENT '用户ID',
    `NOTIFY_TYPES` varchar(32) DEFAULT '' COMMENT '通知类型',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `USER_ID` (`USER_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS`
(
    `PROJECT_ID`   varchar(64) NOT NULL COMMENT '项目ID',
    `ENALBE`       tinyint(1) DEFAULT NULL COMMENT '是否启用',
    `CREATED_TIME` datetime    NOT NULL COMMENT '创建时间',
    `UPDATED_TIME` datetime    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`PROJECT_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_AGENT_PIPELINE_REF`
(
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `NODE_ID` bigint(20) NOT NULL COMMENT '节点ID',
    `AGENT_ID` bigint(20) NOT NULL COMMENT '构建机ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `PIEPLINE_NAME` varchar(255) NOT NULL COMMENT '流水线名称',
    `VM_SEQ_ID` varchar(34) DEFAULT NULL COMMENT '构建序列号',
    `JOB_ID` varchar(34) DEFAULT NULL COMMENT 'JOB ID',
    `JOB_NAME` varchar(255) NOT NULL COMMENT 'JOB NAME',
    `LAST_BUILD_TIME` datetime DEFAULT NULL COMMENT '最近构建时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uindex_project_agent_pipeline_seq` (`PROJECT_ID`,`AGENT_ID`,`PIPELINE_ID`,`VM_SEQ_ID`),
    key `idx_agent_id` (`AGENT_ID`),
    key `idx_pipeline` (`PROJECT_ID`,`PIPELINE_ID`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_ENV_SHARE_PROJECT` (
  `ENV_ID` bigint(20) NOT NULL COMMENT '环境ID',
  `ENV_NAME` varchar(128) NOT NULL COMMENT '环境名称' COLLATE utf8mb4_bin,
  `MAIN_PROJECT_ID` varchar(64) NOT NULL COMMENT '主项目ID',
  `SHARED_PROJECT_ID` varchar(64) NOT NULL COMMENT '共享的目标项目ID',
	`SHARED_PROJECT_NAME` varchar(1024) NULL COMMENT '目标项目名称',
	`TYPE` varchar(64) NOT NULL COMMENT '类型',
  `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
  `CREATE_TIME` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ENV_ID`,`MAIN_PROJECT_ID`,`SHARED_PROJECT_ID`),
  KEY `SHARED_PROJECT_ID` (`SHARED_PROJECT_ID`),
  KEY `ENV_NAME` (`ENV_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

SET FOREIGN_KEY_CHECKS = 1;

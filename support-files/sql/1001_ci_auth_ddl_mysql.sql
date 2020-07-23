USE devops_ci_auth;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_LOG_INDICES
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_GROUP` (
  `ID` varchar(64) NOT NULL COMMENT '主健ID',
  `GROUP_NAME` varchar(32) NOT NULL DEFAULT '""' COMMENT '用户组名称',
  `GROUP_CODE` varchar(32) NOT NULL COMMENT '用户组标识 默认用户组标识一致',
  `GROUP_TYPE` tinyint(2) NOT NULL DEFAULT '0' COMMENT '用户组类型:0.默认 1.用户自定义',
  `PROJECT_CODE` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户组所属项目',
  `CREATE_USER` varchar(64) NOT NULL DEFAULT '""' COMMENT '添加人',
  `UPDATE_USER` varchar(64) DEFAULT NULL COMMENT '修改人',
  `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `GROUP_NAME+PROJECT_CODE` (`GROUP_NAME`,`PROJECT_CODE`),
  KEY `PROJECT_CODE` (`PROJECT_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4

CREATE TABLE IF NOT EXISTS `T_AUTH_GROUP_PERSSION` (
  `ID` varchar(64) NOT NULL COMMENT '主健ID',
  `AUTH_ACTION` varchar(64) NOT NULL DEFAULT '""' COMMENT '权限动作',
  `GROUP_CODE` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户组编号 默认7个内置组编号固定 自定义组编码随机',
  `CREATE_USER` varchar(64) NOT NULL DEFAULT '""' COMMENT '创建人',
  `UPDATE_USER` varchar(64) DEFAULT NULL COMMENT '修改人',
  `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4

CREATE TABLE IF NOT EXISTS `T_AUTH_GROUP_USER` (
  `ID` varchar(64) NOT NULL COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户ID',
  `GROUP_ID` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户组ID',
  `CREATE_USER` varchar(64) NOT NULL DEFAULT '""' COMMENT '添加用户',
  `CREATE_TIME` datetime(3) NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4

SET FOREIGN_KEY_CHECKS = 1;

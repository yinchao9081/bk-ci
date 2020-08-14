-- 连续查询
-- 核心插件相关
CREATE CONTINUOUS QUERY cq_atom_total_count ON monitoring  BEGIN SELECT count(errorCode) as total_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_failed_count ON monitoring  BEGIN SELECT count(errorCode) as failed_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where errorCode != '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_success_count ON monitoring  BEGIN SELECT count(errorCode) as success_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where errorCode = '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_success_rat ON monitoring  BEGIN SELECT sum(success_count) * 100 / sum(total_count) as success_rat INTO AtomMonitorData_success_rat_count FROM AtomMonitorData_success_rat_count group by time(5m) END

CREATE CONTINUOUS QUERY cq_atom_linuxscript_total_count ON monitoring  BEGIN SELECT count(errorCode) as linuxscript_total_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'linuxScript' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_linuxscript_failed_count ON monitoring  BEGIN SELECT count(errorCode) as linuxscript_failed_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'linuxScript' and errorCode != '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_linuxscript_success_count ON monitoring  BEGIN SELECT count(errorCode) as linuxscript_success_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'linuxScript' and errorCode = '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_linuxscript_success_rat ON monitoring  BEGIN SELECT sum(linuxscript_success_count) * 100 / sum(linuxscript_total_count) as linuxscript_success_rat INTO AtomMonitorData_success_rat_count FROM AtomMonitorData_success_rat_count group by time(5m) END

CREATE CONTINUOUS QUERY cq_atom_UploadArtifactory_total_count ON monitoring  BEGIN SELECT count(errorCode) as UploadArtifactory_total_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'UploadArtifactory' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_UploadArtifactory_failed_count ON monitoring  BEGIN SELECT count(errorCode) as UploadArtifactory_failed_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'UploadArtifactory' and errorCode != '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_UploadArtifactory_success_count ON monitoring  BEGIN SELECT count(errorCode) as UploadArtifactory_success_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'UploadArtifactory' and errorCode = '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_UploadArtifactory_success_rat ON monitoring  BEGIN SELECT sum(UploadArtifactory_success_count) * 100 / sum(UploadArtifactory_total_count) as UploadArtifactory_success_rat INTO AtomMonitorData_success_rat_count FROM AtomMonitorData_success_rat_count group by time(5m) END

CREATE CONTINUOUS QUERY cq_atom_CODE_GIT_total_count ON monitoring  BEGIN SELECT count(errorCode) as CODE_GIT_total_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'CODE_GIT' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_CODE_GIT_failed_count ON monitoring  BEGIN SELECT count(errorCode) as CODE_GIT_failed_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'CODE_GIT' and errorCode != '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_CODE_GIT_success_count ON monitoring  BEGIN SELECT count(errorCode) as CODE_GIT_success_count INTO AtomMonitorData_success_rat_count FROM AtomMonitorData where atomCode = 'CODE_GIT' and errorCode = '0' group by time(5m) END
CREATE CONTINUOUS QUERY cq_atom_CODE_GIT_success_rat ON monitoring  BEGIN SELECT sum(CODE_GIT_success_count) * 100 / sum(CODE_GIT_total_count) as CODE_GIT_success_rat INTO AtomMonitorData_success_rat_count FROM AtomMonitorData_success_rat_count group by time(5m) END

-- 用户登录相关
CREATE CONTINUOUS QUERY cq_user_total_count ON monitoring BEGIN SELECT count(errorCode) AS user_total_count INTO monitoring.monitoring_retention.UsersStatus_success_rat_count FROM monitoring.monitoring_retention.UsersStatus GROUP BY time(5m) END
CREATE CONTINUOUS QUERY cq_user_failed_count ON monitoring BEGIN SELECT count(errorCode) AS user_failed_count INTO monitoring.monitoring_retention.UsersStatus_success_rat_count FROM monitoring.monitoring_retention.UsersStatus WHERE errorCode != '0' GROUP BY time(5m) END
CREATE CONTINUOUS QUERY cq_user_success_count ON monitoring BEGIN SELECT count(errorCode) AS user_success_count INTO monitoring.monitoring_retention.UsersStatus_success_rat_count FROM monitoring.monitoring_retention.UsersStatus WHERE errorCode = '0' GROUP BY time(5m) END
CREATE CONTINUOUS QUERY cq_user_success_rat ON monitoring BEGIN SELECT sum(user_success_count) * 100 / sum(user_total_count) AS UsersStatus_success_rat INTO monitoring.monitoring_retention.UsersStatus_success_rat_count FROM monitoring.monitoring_retention.UsersStatus_success_rat_count GROUP BY time(5m) END

-- 构建机准备相关
CREATE CONTINUOUS QUERY cq_dispatch_devcloud_total_count ON monitoring BEGIN SELECT count(errorCode) AS devcloud_total_count INTO monitoring.monitoring_retention.DispatchStatus_success_rat_count FROM monitoring.monitoring_retention.DispatchStatus WHERE GROUP BY time(5m),buildType END
CREATE CONTINUOUS QUERY cq_dispatch_devcloud_failed_count ON monitoring BEGIN SELECT count(errorCode) AS devcloud_failed_count INTO monitoring.monitoring_retention.DispatchStatus_success_rat_count FROM monitoring.monitoring_retention.DispatchStatus WHERE errorCode != '0' GROUP BY time(5m),buildType END
CREATE CONTINUOUS QUERY cq_dispatch_devcloud_success_count ON monitoring BEGIN SELECT count(errorCode) AS devcloud_success_count INTO monitoring.monitoring_retention.DispatchStatus_success_rat_count FROM monitoring.monitoring_retention.DispatchStatus WHERE errorCode = '0' GROUP BY time(5m),buildType END
CREATE CONTINUOUS QUERY cq_dispatch_devcloud_success_rat ON monitoring BEGIN SELECT sum(devcloud_success_count) * 100 / sum(devcloud_total_count) AS devcloud_success_rat INTO monitoring.monitoring_retention.DispatchStatus_success_rat_count FROM monitoring.monitoring_retention.DispatchStatus_success_rat_count GROUP BY time(5m) END
CREATE CONTINUOUS QUERY cq_dispatch_devcloud_start_count ON monitoring BEGIN SELECT count(errorCode) AS devcloud_total_count INTO monitoring.monitoring_retention.DispatchStatus_success_rat_count FROM monitoring.monitoring_retention.DispatchStatus WHERE actionType='start' AND errorCode='0' GROUP BY time(5m),buildType END
CREATE CONTINUOUS QUERY cq_dispatch_devcloud_stop_count ON monitoring BEGIN SELECT count(errorCode) AS devcloud_total_count INTO monitoring.monitoring_retention.DispatchStatus_success_rat_count FROM monitoring.monitoring_retention.DispatchStatus WHERE actionType='stop' AND errorCode='0' GROUP BY time(5m),buildType END

-- codecc相关
CREATE CONTINUOUS QUERY cq_codecc_reduce ON monitoring BEGIN SELECT count(buildId) as total_count,mean(elapseTime) as avg_time INTO monitoring.monitoring_retention.CodeccMonitor_reduce FROM monitoring.monitoring_retention.CodeccMonitor WHERE elapseTime>0 GROUP BY time(5m),errorCode,bgId,toolName END
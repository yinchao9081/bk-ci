#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
source ${CTRL_DIR:-/data/install}/load_env.sh
set +a
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CI_SRC_DIR=${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}
cd "$CTRL_DIR"

echo "注册 蓝鲸 ESB"
./bin/add_or_update_appcode.sh "$BK_CI_APP_CODE" "$BK_CI_APP_TOKEN" "蓝盾" "mysql-paas"  # 注册app。第4个参数即是login-path。

echo "配置数据库访问权限."
./bin/setup_mysql_loginpath.sh -n mysql-ci -h "${BK_CI_MYSQL_ADDR%:*}" -u "$BK_CI_MYSQL_USER" -p "$BK_CI_MYSQL_PASSWORD"
sleep 1
# 如果使用了蓝鲸默认的数据库, 则自动刷新权限.
if [ "${BK_CI_MYSQL_ADDR%:*}" = "$BK_MYSQL_IP" ]; then
  # 提前渲染变量, 避免mysql服务端没有ci的变量.
  ./pcmd.sh -H "$BK_MYSQL_IP" "$CTRL_DIR/bin/grant_mysql_priv.sh -n default-root -u \"$BK_CI_MYSQL_USER\" -p \"$BK_CI_MYSQL_PASSWORD\" -H \"$(<$CTRL_DIR/.controller_ip)\""
  ./pcmd.sh -H "$BK_MYSQL_IP" "$CTRL_DIR/bin/grant_mysql_priv.sh -n default-root -u \"$BK_CI_MYSQL_USER\" -p \"$BK_CI_MYSQL_PASSWORD\" -H \"$BK_CI_IP_COMMA\""
else
  echo "你使用了第三方MySQL数据库. 请自行检查CI全部节点能否访问数据库."
fi

echo "导入 SQL 文件"
./bin/sql_migrate.sh -n mysql-ci $BK_PKG_SRC_PATH/ci/support-files/sql/*.sql

echo "RabbitMQ"
echo " 创建vhost及账户."
./pcmd.sh -H "$BK_RABBITMQ_IP" "$CTRL_DIR/bin/add_rabbitmq_user.sh -u \"$BK_CI_RABBITMQ_USER\" -p \"$BK_CI_RABBITMQ_PASSWORD\" -h \"$BK_CI_RABBITMQ_VHOST\"; rabbitmqctl change_password \"$BK_CI_RABBITMQ_USER\" \"$BK_CI_RABBITMQ_PASSWORD\""


echo "导入 IAM 权限模板"
./bin/bkiam_migrate.sh -t "$BK_IAM_PRIVATE_URL" -a "$BK_CI_APP_CODE" -s "$BK_CI_APP_TOKEN" $BK_PKG_SRC_PATH/ci/support-files/bkiam/*.json

# 处理rabbitmq插件.
rabbitmq_plugin_url="https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.8.0/rabbitmq_delayed_message_exchange-3.8.0.ez"
echo " 检查所有服务端的 rabbitmq_delayed_message_exchange 插件是否启用"
if ./pcmd.sh -m rabbitmq 'rabbitmq-plugins list | grep rabbitmq_delayed_message_exchange | grep -F "E*"'; then
  echo "RabbitMQ插件rabbitmq_delayed_message_exchange已经启用."
else
  echo "RabbitMQ插件rabbitmq_delayed_message_exchange未启用."
  echo "参考下载地址: $rabbitmq_plugin_url"
  #echo " 服务端启用rabbitmq_delayed_message_exchange插件."
  #./pcmd.sh -m rabbitmq 'rabbitmq-plugins enable rabbitmq_delayed_message_exchange'
fi

# 不影响部署结果, 如果报错可以忽略:
echo "注册 PaaS 桌面图标"
$BK_CI_SRC_DIR/scripts/bk-ci-reg-paas-app.sh || {
  echo "未能注册PaaS桌面图标. 您可以忽略此步骤."
  exit 1
}


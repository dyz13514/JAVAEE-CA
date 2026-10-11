# 本机 demo 数据库

`database/demo.sql` 为当前实体建立独立的 `cats_demo` 库，使用 MySQL 8.4。
它只用于首次导入空库，没有 DROP、TRUNCATE 或 DELETE；已有表会使导入停止。
原来的 `cats` 数据库不受影响。不要在导入报错后使用 `--force` 继续。

先用 MySQL 客户端导入 `database/demo.sql`，再在项目根目录运行：

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) bash mvnw -Dspring-boot.run.profiles=demo spring-boot:run
```

本机用户名和密码仍使用 Git 忽略的 `config/application.properties`。
`demo` 配置使用 `ddl-auto=validate` 检查结构，关闭邮件外发，并隔离演示报销附件目录。
原先的 Java 演示数据加载器在此配置下不运行，避免旧目录和测试员工重新出现。

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | admin | admin123 |
| 上级经理 | director1 | director123 |
| 经理 | manager1 | manager123 |
| 员工 | emp1、emp2 | emp123 |
| 待注册审核 | newstaff1 | newstaff123 |

SQL 中保存 BCrypt 密码。经理 manager1 的主管是 director1；emp1、emp2 的主管是 manager1。
所有演示账号都无邮件地址，不会给真实收件人发送邮件。

包含年度额度、三种原始类别、三家演示机构、待审批/已修改/已批准/已完成/已拒绝/已撤回申请及待审核注册。
未来日期以导入当天之后的周一为基准，不依赖固定开课排期。
已完成申请可用来提交报销；报销附件需要通过页面实际上传，不伪造缺失的收据或证书文件。
不导入旧 Course 表、固定排期或原先的六条示例目录。

这是独立演示数据；旧迁移脚本已从最新版仓库删除，此 SQL 不依赖这些脚本。

# 申请与培训机构正式关联

CourseApplication.provider 现在是 TrainingProvider 对象。表单提交 providerId，
Service 查询机构表后保存关联，数据库通过 provider_id 外键连接。
机构改名后，申请显示和搜索使用新名称；被常用课程或任何申请使用的机构不能删除。
申请的 fee、trainingDays 和额度计算规则保持原样。

## 新数据库

没有旧数据的数据库不需要迁移脚本，正常启动时 Hibernate 创建关联字段。
申请前，管理员需要先创建机构，或使用 DataLoader 已建立的机构。

## 已有数据库

不要直接依赖 ddl-auto=update 将名称转换为机构 ID，它不会替你匹配旧机构。

1. 停止 Spring Boot，导出完整数据库备份。
2. 如果仍是旧 Course / String category 结构，先执行
   database/migrate-category-and-catalogue.sql；已经升级过的库跳过该步骤。
3. 在 MySQL Workbench 中选择自己的数据库，先检查空机构名称：

```sql
SELECT id, course_title, provider
FROM course_applications
WHERE provider IS NULL OR TRIM(provider) = '';
```

有结果时，根据实际课程确定机构，并填回旧 provider 名称。
例如，只有确认申请 12 的实际机构确实为 NUS-ISS 时，才执行：

```sql
UPDATE course_applications SET provider = 'NUS-ISS' WHERE id = 12;
```

不要猜测机构或批量把空值指派给某家机构。

4. 打开 database/migrate-application-provider.sql，执行整个文件。
   已存在的机构按名称匹配，保留 ID；旧申请填写的其他非空机构名称会建成机构记录。
   名称忽略大小写并去除两端空格；存在歧义时脚本会停止，要求先整理机构记录。
   即使此前启动时已添加空的 provider_id，脚本仍可完成转换。
5. 确认旧、新申请数量一致，再启动项目。

脚本创建 archive_applications_before_provider 和 archive_providers_before_provider
保留转换前的记录，并保留费用、天数、状态、员工和申请 ID。
成功后，旧 provider 字符串列被删除。脚本只执行一次。
如果执行出错，先停止并检查原因；MySQL 结构修改不能整体回滚，
不要直接重复执行。失败过程可能留下存储过程，恢复备份后可按需执行
DROP PROCEDURE IF EXISTS cats_migrate_application_provider，再重新执行脚本。

## 网页验证

1. 管理员新建两家机构。
2. 员工新建申请：选择机构、类别、日期并提交。
3. 打开详情和编辑页，确认机构名称与下拉框选中项正确。
4. 把待审批申请改为另一家机构，确认保存后的详情和搜索结果更新。
5. 管理员为该机构改名，确认申请详情及员工、经理的机构名称搜索使用新名称。
6. 删除已被申请使用的机构，应出现不能删除的提示；未被使用的机构可以删除。
7. 从常用课程点击 Use for Application，确认机构自动选中。
8. 查看年度额度，确认天数和费用统计规则未改变。

## 测试

运行项目现有 Maven 测试。机构关联、提交/修改的无效 ID、
常用课程预填、机构改名后搜索、删除保护和真实 Thymeleaf 渲染
使用 Mockito 或独立 H2 测试库验证，不连接实际业务数据库。

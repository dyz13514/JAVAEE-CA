# Course 选择与已有数据库升级

Course 的一条记录代表名称、类别、培训机构和固定费用组成的具体课程。
CommonCourse 只关联已有 Course，由管理员挑选加入或移出；完整课程库没有页面增删改入口。
新申请必须选择 Course；服务器按 Course ID 读取名称、类别、机构、费用，忽略请求中的费用和机构文字。
名称下拉去重，类别和机构按上一级选择筛选。只有一个类别或机构时自动选中。
日期、半天、申请理由及知识分享计划仍由员工填写，原有日期、额度和审批规则继续使用。
INTERNAL 必须为零费用；新演示数据库初始化 18 条组合，其他类别从 200、300、500 三档中生成固定费用。

申请保存 Course 关联以及提交时的名称、类别名称、费用，并通过 provider_id 关联培训机构。
以后修改课程资料不会自动改写已保存的名称、类别名称和费用；机构改名后，已有申请的机构显示和搜索使用新名称。编辑待审批申请时按重新选定的课程资料提交。
移出常用目录只删除 CommonCourse 关联，不修改或停用 Course；课程仍在 All Course 和申请下拉框中。
常用目录与 All Course 在同一页上下展示。员工和经理只有查看及申请入口；只有管理员可以维护常用关联。
旧申请无法匹配 Course 时保留原记录，查看、审批、取消、完成仍可使用，编辑时需要选择一门可用课程。

## 当前直接保存课程资料的 MySQL 数据库

1. 停止项目，在 MySQL Workbench 导出完整数据库备份。
2. 核对 common_courses 包含 title、category_id、provider_id、fee、introduction，且尚未创建 courses。
3. 选中项目实际使用的数据库，完整执行 `database/migrate-course-selection.sql`，包括 DELIMITER 和 CALL。
4. 检查脚本输出的目录数量和申请数量前后一致。
5. 启动项目，检查常用目录、联动选择、固定费用、提交、编辑和经理审批。

脚本保留目录 ID；旧目录整体保存为 archive_common_courses_before_selection，旧申请另存为 archive_applications_before_selection。
内部目录课程费用改为零，历史申请中的费用、日期、状态和培训天数不变。
只有名称、类型、机构、金额全部相符的旧申请才关联课程，其他旧申请不会强行绑定。
原有目录费用会保留（内部课程除外），不会为了演示覆盖管理员数据。
空数据库直接启动，会生成 18 条演示课程，常用目录由管理员手动挑选，不执行升级 SQL。
已经完成 Course 升级的数据库不应重复执行这些脚本；已有常用关联保持原样，管理员可从页面逐项移出或加入。
这些历史升级脚本针对申请仍保存 provider 文字的旧结构。当前代码使用 provider_id；启动前须核对培训机构关联已建立且已有申请均有有效的 provider_id。不要把 Course 升级脚本当作机构关联迁移脚本。
若仍保留更早的 courses/category 文字结构，使用 `database/migrate-legacy-course-selection.sql`。
它也支持旧表与部分新字段并存的结构，会保留已有 category_id，归档旧课程和排期。
只有旧常用目录中发布的课程会保持可申请状态，其他旧课程保留为停用记录。
需要演示数据时，再执行 `database/seed-course-selection-demo.sql`，补充 18 条组合，不覆盖已有同组合课程，也不会自动加入常用目录。

Hibernate 的 ddl-auto=update 不会搬迁目录资料。升级 SQL 必须在新代码首次连接旧数据库前运行。
脚本为一次性迁移；已有迁移归档、Course 表或部分新增申请列时会停止，避免重复覆盖。
MySQL DDL 自动提交；中途失败请保留归档并检查数据库，不能依赖 ROLLBACK 恢复。

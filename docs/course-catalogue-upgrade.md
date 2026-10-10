# 已有 MySQL 数据库升级

本次由 Course + CommonCourse(course_id) 改为独立 Category 和直接保存课程资料的 CommonCourse。
Hibernate 的 ddl-auto=update 不会自动搬迁旧资料或删除旧的非空 course_id，
所以已有数据库不能仅靠重启完成升级。

1. 停止 Spring Boot。先在 MySQL Workbench 导出完整数据库备份。
2. 选中项目实际使用的数据库，核对当前还有 courses、common_courses.course_id、
   course_applications.category 三处旧结构。
3. 打开并完整执行 `database/migrate-category-and-catalogue.sql`，包括 DELIMITER、过程定义及 CALL。
4. 末尾查询的迁移前后目录数量、申请数量应分别相同。核对三种原始类别和半天设置。
5. 启动 Spring Boot。管理员检查类别、常用课程、培训机构、假日及额度页面；
   员工检查原申请、手动新申请、从目录预填，以及申请编辑；经理检查审批和报表。

脚本保留 common_courses 的原 ID，申请 ID、员工 ID、日期、trainingDays、实际费用和状态不变。
只有旧目录选中的课程进入新 CommonCourse；未选中的总 Course 不会自动变成常用课程。
全部旧 Course、目录、申请和排期还会复制到 archive_* 表中，归档表没有外键，不影响今后的维护。
不要在确认迁移正确前删除归档表。

脚本只适用于本项目旧结构。空数据库直接启动，不执行迁移脚本。
已经迁移过不能重复运行。中途中断时停止操作，检查报错和导出备份；
MySQL 的 DDL 会自动提交，不能依赖 ROLLBACK 恢复整个迁移。
脚本遇到空类别、失效机构或其他未知 Course 依赖会停止，避免把这些记录静默丢弃。
如果曾尝试新版本导致 category_id 已部分生成，脚本会重新按原类别文字映射该列。

旧 `migrate-common-course-catalogue.sql` 已由此次脚本替换，不再运行旧拆表脚本。
申请 INTERNAL 时实际费用会保留，但不占用预算；迁移不会把旧 INTERNAL 的费用强制改成零。

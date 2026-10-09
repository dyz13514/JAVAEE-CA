# 统一课程浏览、申请和 Course view

本轮在总 Course 功能基础上统一课程浏览、固定课期申请和按角色查看课程的流程。

## 使用流程

1. Employee 和 Manager 共用 `/employee/login`、`/employee/home` 和 `/employee/courses`，页面名称均为 **Apply for course**。
2. Admin 使用 `/admin/courses`，页面名称为 **Course management**，只管理课程，不申请课程。
3. 页面依次展示搜索栏、Commonly Attended Courses、All courses。搜索同时作用于两个课程区域。
4. 卡片进入各自角色前缀下的 `/courses/{id}` 详情页。
5. Employee 和 Manager 共用申请表 `/employee/apply`；Admin 访问或提交该申请入口均被拒绝。
6. Employee 和 Manager 的申请由指定主管审核；没有主管不能提交，也不能自我审批。
7. `/course-view` 为 Employee 显示个人申请，Manager 可切换个人/直属人员；Admin 默认进入人员列表，只查看管理范围内的培训情况。
8. 月历默认显示已批准课程，也可以选择显示待审核申请。

旧 `/courses` 和 `/courses/{id}` 链接自动转到当前角色的课程地址。
管理员从 Course management 新增课程，从详情页编辑或删除，从 Common course catalogue 选择常用课程。
Employee、Manager 没有管理按钮，管理接口也会检查管理员权限。

## 课期规则

- 管理员设置固定时长，当前支持 1–30 个完整培训日。两天只是测试数据的设置，并非所有培训都只能两天。
- 开始日期按 YYYY-MM-DD 输入，一行一个；也支持逗号分隔。重复日期或周末日期会被拒绝。
- 空日期列表表示课程仍可浏览，但暂不开放申请。
- 申请只能选择未来且不是公共假期的已发布日期。
- 结束日期按培训日计算，跳过周末和 `public_holidays` 中的日期。
- 旧申请保留原来的日期及课程信息。编辑同一申请、保留原开始日期时，不因管理员后来修改时长而移动结束日期。
- 新申请必须选择总 Course。之前没有关联总 Course 的历史申请，仍可用独立的兼容页面编辑。
- 参考费用与实际申请费用沿用已有规则；本轮没有修改额度或预算规则。

## 数据库变化

- `courses` 新增 `introduction` 和 `duration_days`。允许旧记录暂时为空，避免升级时覆盖原课程。
- 新增 `course_start_dates`：`course_id` 外键引用 `courses.id`，`start_date` 为开课日期，两列组合唯一。
- 使用 JPA `@ElementCollection` 保存每门课程的日期列表，不额外引入复杂的班次实体。
- `common_courses` 仍引用总 `courses`；`course_applications` 仍保存申请快照。
- 当前配置为 `spring.jpa.hibernate.ddl-auto=update`，重启时由 Hibernate 增加字段和日期表。
- 不要重新运行旧目录转换 SQL；它与本轮课期升级无关。

## 本轮文件清单

以下路径相对于 `E:\CA Project\CATS`。

| 文件 | 作用 |
|---|---|
| `src/main/java/com/group5/cats/model/Course.java` | 课程介绍、固定天数、可选日期列表及数据库映射 |
| `src/main/java/com/group5/cats/dto/CourseForm.java` | 接收管理员填写的介绍、天数及多行日期 |
| `src/main/java/com/group5/cats/service/CourseServiceImpl.java` | 新增/编辑时验证并保存课期，继续保留删除保护 |
| `src/main/java/com/group5/cats/service/CourseScheduleService.java`（新增） | 定义课期查询和结束日期计算方法 |
| `src/main/java/com/group5/cats/service/CourseScheduleServiceImpl.java`（新增） | 过滤可申请日期，按工作日计算结束日期 |
| `src/main/java/com/group5/cats/controller/AdminCourseController.java` | 旧列表入口跳转，编辑时回填课期信息 |
| `src/main/java/com/group5/cats/controller/CourseBrowseController.java` | 按角色地址提供课程浏览和详情页 |
| `src/main/java/com/group5/cats/controller/EmployeeController.java` | 开放 Employee、Manager 申请、提供课期选项、阻止无课程的新申请，以及保护个人申请详情/编辑入口 |
| `src/main/java/com/group5/cats/service/CourseApplicationServiceImpl.java` | 核验课期、重算结束日期、主管检查、禁止自审、保留申请快照 |
| `src/main/java/com/group5/cats/controller/CourseViewController.java`（新增） | 自己/他人的申请列表及角色范围检查 |
| `src/main/java/com/group5/cats/service/TrainingCalendarService.java` | 增加查询待审核申请的方法 |
| `src/main/java/com/group5/cats/service/TrainingCalendarServiceImpl.java` | 根据月份和查看权限筛选待审核申请 |
| `src/main/java/com/group5/cats/controller/TrainingCalendarController.java` | 接收是否显示待审核申请的选项 |
| `src/main/java/com/group5/cats/DataLoader.java` | 六门测试课程的介绍和两组课期；兼容补全之前生成的六门样例 |
| `src/main/resources/templates/courses.html` | 共用课程卡片页和管理员新增入口 |
| `src/main/resources/templates/fragments/course-card.html` | 整张卡片可进入详情，支持键盘访问 |
| `src/main/resources/templates/course-detail.html`（新增） | 课程介绍、机构、课期、Apply 和管理员操作 |
| `src/main/resources/templates/admin/course-form.html` | 新增课程的介绍、固定天数、日期输入 |
| `src/main/resources/templates/admin/course-edit.html` | 编辑课程和课期 |
| `src/main/resources/templates/apply-course.html` | 选择固定课期并填写申请理由 |
| `src/main/resources/templates/legacy-apply-course.html`（新增） | 无 Course 关联的历史申请兼容表单 |
| `src/main/resources/templates/course-view.html`（新增） | 个人/他人课程列表、完整状态、月历链接 |
| `src/main/resources/templates/training-calendar.html` | 返回课程列表、待审核开关及状态显示 |
| `src/main/resources/templates/fragments/layout.html` | 按角色区分 Apply for course / Course management，并提供 Course view 导航 |
| `src/main/resources/templates/admin/home.html` | 首页 Course management 指向管理员课程页 |
| `src/main/resources/static/css/cats.css` | 卡片悬停/焦点反馈、介绍换行、待审核日历颜色；继续使用原站点样式 |

## 测试文件

- 新增 `src/test/java/com/group5/cats/service/CourseScheduleServiceTests.java`：培训日、周末/假期、可选日期。
- 新增 `src/test/java/com/group5/cats/controller/CourseViewControllerTests.java`：三种角色的查看范围和直接改网址越权。
- 更新 `CourseApplicationServiceTests.java`：篡改结束日期和半天参数、非法开课日期、主管与自审保护。
- 更新 `EmployeeControllerTests.java`：新申请必须选课程、保护申请详情/编辑、绑定及错误回填。
- 更新 `CourseCatalogueIntegrationTests.java`：真实 H2 持久化介绍、天数和日期列表。
- 更新 `CourseCatalogueServiceTests.java`、`CourseCatalogueControllerTests.java`、`CourseBrowseControllerTests.java`：课程表单及共用入口变化。
- 更新 `TrainingCalendarServiceTests.java`：待审核状态和跨月范围。
- 更新 `PageRenderingTests.java`：详情、申请、Course view 和历史兼容页的真实 Thymeleaf 渲染。

## 重启后检查

在原来的 PowerShell 窗口停止旧服务后，进入项目执行 `./mvnw.cmd spring-boot:run`。
本轮没有停止用户正在运行的 8080，也没有开启 8081。

之前六门样例只有在“名称和机构匹配，且介绍与天数均未设置”时才补全一次。
管理员已经编辑的课程不会被覆盖，手动清空的开课日期也不会在下次启动时重新添加。

如果 Employee 或 Manager 提示缺少主管，请在员工管理里分配真实的上级 Manager；
如果提示缺少培训额度，请使用已有 Training allowances 功能配置，不能通过改角色跳过审批或额度。

验证使用模拟对象、真实 Thymeleaf 和独立 H2 数据库。没有向真实邮箱发送测试邮件，
也没有在用户 MySQL 中创建测试申请或改动主管关系。

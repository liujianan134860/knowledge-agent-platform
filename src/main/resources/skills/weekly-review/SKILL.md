# Weekly Review Skill

## 适用场景
用户需要进行一周工作复盘，回顾本周成就和规划下周重点。

## 输入信息
- 当前时间（datetime.now）
- 本周任务数据（task.list）
- 本周笔记数据（note.list with days=7）
- 个人偏好（memory.recall）
- 提醒数据（reminder.list）

## 执行步骤
1. datetime.now 获取当前时间，计算本周范围
2. task.list 获取所有任务，识别本周状态变化
3. note.list (days=7) 获取本周笔记
4. memory.recall 获取用户偏好信息
5. reminder.list 获取提醒列表
6. 数据聚合分析
7. note.create 保存周复盘报告

## 输出格式
```
## 周复盘：[YYYY-MM-DD ~ YYYY-MM-DD]

### 本周概览
- 完成 X / Y 个任务 (XX% 完成率)
- 创建 N 条笔记
- P 条提醒

### 已完成
- [x] 任务名称 (完成时间)
...

### 待处理
- [ ] 任务名称 (优先级: HIGH/MEDIUM/LOW)
...

### 本周笔记摘要
- 笔记标题: 简要内容
...

### 下周重点
1. [最优先事项]
2. [次优先事项]
3. [第三优先事项]

### 反思与建议
[简要分析与建议]
```

## 注意事项
- 突出成就和进展
- 未完成任务给出处理建议
- 语气积极鼓励
- 时间范围：过去 7 天

# Knowledge Capture Skill

## 适用场景
用户想要记录信息、保存想法、存储偏好或经验。

## 输入信息
- 用户要记录的内容
- 可能需要用户确认信息分类

## 执行步骤
1. 分析信息类型（偏好/事实 → memory, 知识/笔记 → note, 行动项 → task）
2. 如果分类不确定，询问用户
3. 提取核心要点
4. 使用对应工具保存：
   - memory.remember(key, value, category)
   - note.create(title, content, tags)
   - task.create(title, description, priority)
5. 确认保存结果

## 输出格式
```
已保存：
- [类型] [标题/Key] — [简要内容]
```

## 注意事项
- 优先使用最合适的存储方式
- 长期个人事实用 memory，知识记录用 note
- 不要同时存到多个地方（除非用户明确需要）
- 信息存之前，确保正确理解了内容

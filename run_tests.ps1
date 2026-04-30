#!/usr/bin/env pwsh
# Knowledge Agent Platform - 全自动测试脚本
$Global:Pass = 0; $Global:Fail = 0; $Global:Token = ""; $Global:SessionId = ""

function PassFail($desc, $cond) {
    if ($cond) { Write-Host "  [PASS] $desc" -ForegroundColor Green; $Global:Pass++ }
    else { Write-Host "  [FAIL] $desc" -ForegroundColor Red; $Global:Fail++ }
}

function ApiOk($desc, $resp) { PassFail "$desc (success=true)" ($resp -match '"success":true') }
function GetToken {
    if ($Global:Token) { return $Global:Token }
    $resp = curl.exe -s -X POST "http://localhost:8081/api/auth/login" -H "Content-Type: application/json" -d '{"username":"testuser","password":"test123456"}'
    if ($resp -match '"token":"([^"]+)"') { $Global:Token = $matches[1] }
    return $Global:Token
}

$BASE = "http://localhost:8081"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Knowledge Agent Platform 自动测试套件" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ===== 第0步: 启动应用 =====
Write-Host "`n=== 第0步: 启动应用 ===" -ForegroundColor Yellow
$proc = Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory "E:\简历\knowledge-agent-platform" -NoNewWindow -PassThru
Start-Sleep -Seconds 20

$ready = $false
for ($i = 0; $i -lt 12; $i++) {
    try { $r = curl.exe -s --max-time 3 "$BASE/"; if ($r -match "<!doctype html>") { $ready = $true; break } } catch {}
    Start-Sleep -Seconds 5
}
PassFail "应用启动成功" $ready
if (-not $ready) { Write-Host "应用未能启动，终止测试" -ForegroundColor Red; exit 1 }

# ===== 第1步: 注册 =====
Write-Host "`n=== 第1步: 注册 ===" -ForegroundColor Yellow
$resp = curl.exe -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"username":"testuser","password":"test123456"}'
ApiOk "注册新用户" $resp
if ($resp -match '"token":"([^"]+)"') { $Global:Token = $matches[1] }

$resp2 = curl.exe -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"username":"testuser","password":"test123456"}'
PassFail "重复注册被拒绝" ($resp2 -match '"success":false' -and $resp2 -match "already exists")

$resp3 = curl.exe -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"username":"ab","password":"12"}'
PassFail "短用户名/密码被拒绝" ($resp3 -match '"success":false')

$resp4 = curl.exe -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"username":"testuser","password":"test123456"}'
ApiOk "登录" $resp4

$resp5 = curl.exe -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"username":"testuser","password":"wrongpass"}'
PassFail "错误密码登录被拒绝" ($resp5 -match '"success":false')

$resp6 = curl.exe -s "$BASE/api/sessions"
PassFail "无Token访问API被拒绝" ($resp6 -match '"success":false')

# ===== 第2步: 知识库 =====
Write-Host "`n=== 第2步: 知识库 ===" -ForegroundColor Yellow
$token = GetToken
$resp = curl.exe -s -X POST "$BASE/api/documents" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"title":"RAG架构","content":"RAG（Retrieval-Augmented Generation）是一种将检索与生成相结合的架构，广泛用于知识问答系统。","tags":["RAG","AI","架构"]}'
ApiOk "添加知识片段" $resp

$resp = curl.exe -s -X POST "$BASE/api/documents" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"title":"Java Spring Boot","content":"Spring Boot是Java生态系统中最流行的微服务框架之一，提供了自动配置、嵌入式服务器等特性。","tags":["Java","Spring"]}'
ApiOk "添加第二个知识片段" $resp

$resp = curl.exe -s "$BASE/api/documents?query=RAG" -H "Authorization: Bearer $token"
PassFail "列出知识库包含RAG" ($resp -match '"title":"RAG架构"')

# 文件上传
Set-Content -Path "test_upload.txt" -Value "知识管理是对知识的获取、存储、共享和创新的全过程管理。"
$resp = curl.exe -s -X POST "$BASE/api/documents/upload" -H "Authorization: Bearer $token" -F "file=@test_upload.txt" -F "title=知识管理" -F "tags=知识管理,上传"
ApiOk "文件上传" $resp
Remove-Item "test_upload.txt" -ErrorAction SilentlyContinue

$resp = curl.exe -s "$BASE/api/documents" -H "Authorization: Bearer $token"
PassFail "列出知识片段" ($resp -match '"title":"RAG架构"')

# ===== 第3步: 工具调用 =====
Write-Host "`n=== 第3步: 工具调用 ===" -ForegroundColor Yellow
$resp = curl.exe -s "$BASE/api/tools" -H "Authorization: Bearer $token"
ApiOk "列出工具" $resp

$resp = curl.exe -s -X POST "$BASE/api/tools/calculator/invoke" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"input":"12 + 30"}'
PassFail "计算器工具(12+30)" ($resp -match '"output":"42"')

$resp = curl.exe -s -X POST "$BASE/api/tools/echo/invoke" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"input":"hello agent"}'
PassFail "Echo工具" ($resp -match '"output":"hello agent"')

$resp = curl.exe -s -X POST "$BASE/api/tools/nonexistent/invoke" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"input":"test"}'
PassFail "不存在工具返回错误" ($resp -match '"success":false')

# ===== 第4步: 会话与问答 =====
Write-Host "`n=== 第4步: 会话与问答 ===" -ForegroundColor Yellow
$resp = curl.exe -s -X POST "$BASE/api/chat" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"sessionId":"","question":"你好，请用中文介绍一下你自己"}'
ApiOk "非流式问答" $resp
if ($resp -match '"sessionId":"([^"]+)"') { $Global:SessionId = $matches[1] }

if ($Global:SessionId) {
    $resp = curl.exe -s -X POST "$BASE/api/chat" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "{`"sessionId`":`"$Global:SessionId`",`"question`":`"追问：RAG架构和Spring Boot有什么关系？`"}"
    ApiOk "追问（同一会话）" $resp
}

$resp = curl.exe -s "$BASE/api/sessions" -H "Authorization: Bearer $token"
ApiOk "列出会话" $resp

if ($Global:SessionId) {
    $resp = curl.exe -s "$BASE/api/runs?sessionId=$Global:SessionId" -H "Authorization: Bearer $token"
    ApiOk "获取会话运行记录" $resp
}

# ===== 第5步: 运行时间线 =====
Write-Host "`n=== 第5步: 运行时间线 ===" -ForegroundColor Yellow
if ($Global:SessionId) {
    $resp = curl.exe -s "$BASE/api/runs?sessionId=$Global:SessionId" -H "Authorization: Bearer $token"
    ApiOk "获取运行时间线" $resp
    PassFail "时间线包含执行阶段" ($resp -match '"stage"')
}

# ===== 第6步: Agent & Skills =====
Write-Host "`n=== 第6步: Agent & Skills ===" -ForegroundColor Yellow
$resp = curl.exe -s "$BASE/api/agents" -H "Authorization: Bearer $token"
ApiOk "Agent API" $resp

$resp = curl.exe -s "$BASE/api/skills" -H "Authorization: Bearer $token"
ApiOk "Skills API" $resp

# ===== 第7步: 评测 =====
Write-Host "`n=== 第7步: 评测 ===" -ForegroundColor Yellow
$resp = curl.exe -s -X POST "$BASE/api/evaluations" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"question":"RAG是什么？","expectedKeywords":["检索","生成","知识库"],"feedback":""}'
ApiOk "添加评测用例" $resp
$caseId = if ($resp -match '"id":(\d+)') { [int]$matches[1] } else { 0 }

$resp = curl.exe -s "$BASE/api/evaluations" -H "Authorization: Bearer $token"
ApiOk "列出评测用例" $resp

if ($caseId -gt 0) {
    $resp = curl.exe -s -X POST "$BASE/api/evaluations/run" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "{`"caseId`":$caseId,`"answer`":`"RAG（Retrieval-Augmented Generation）是一种结合检索[1]和生成[2]的技术，通过从知识库中检索相关内容来增强回答质量。`"}"
    PassFail "运行评测" ($resp -match '"score":')
    PassFail "评测指标正确" ($resp -match '"retrievalHit":true' -and $resp -match '"citationPresent":true')
}

# ===== 第8步: QA Review =====
Write-Host "`n=== 第8步: QA Review ===" -ForegroundColor Yellow
$resp = curl.exe -s -X POST "$BASE/api/qa/review" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"question":"RAG是什么？","answer":"RAG是一种结合检索[1]和生成[2]的架构。","sources":[{"title":"RAG论文","content":"RAG（Retrieval-Augmented Generation）"}],"expectedKeywords":["检索","生成","RAG"]}'
PassFail "QA Review API" ($resp -match '"score":')
PassFail "QA Review 指标" ($resp -match '"retrievalHit":true' -and $resp -match '"citationPresent":true' -and $resp -match '"answerContainsExpectedKeywords":true')

# ===== 第9步: Trace =====
Write-Host "`n=== 第9步: Trace ===" -ForegroundColor Yellow
$resp = curl.exe -s "$BASE/api/traces?limit=5" -H "Authorization: Bearer $token"
ApiOk "查看Trace" $resp
PassFail "Trace包含有效阶段" ($resp -match 'USER_INPUT|CONTEXT_BUILD|QA_REVIEW')

# ===== 第10步: JavaScript语法 =====
Write-Host "`n=== 第10步: JavaScript语法检查 ===" -ForegroundColor Yellow
$html = curl.exe -s "$BASE/"
$m = [regex]::Match($html, '<script>([\s\S]*?)</script>')
if ($m.Success) {
    $script = $m.Groups[1].Value
    $tmp = [System.IO.Path]::GetTempFileName() + ".js"
    [System.IO.File]::WriteAllText($tmp, $script)
    $jsOut = & node --check $tmp 2>&1
    Remove-Item $tmp -ErrorAction SilentlyContinue
    PassFail "JavaScript语法正确" ($LASTEXITCODE -eq 0)
} else { PassFail "找到script标签" $false }

# ===== 第11步: 删除操作 =====
Write-Host "`n=== 第11步: 清理操作 ===" -ForegroundColor Yellow
$docResp = curl.exe -s "$BASE/api/documents" -H "Authorization: Bearer $token"
$docMatch = [regex]::Match($docResp, '"id":(\d+)')
if ($docMatch.Success) {
    $did = $docMatch.Groups[1].Value
    $resp = curl.exe -s -X DELETE "$BASE/api/documents/$did" -H "Authorization: Bearer $token"
    ApiOk "删除知识片段" $resp
}

$sessResp = curl.exe -s "$BASE/api/sessions" -H "Authorization: Bearer $token"
$sessMatch = [regex]::Match($sessResp, '"id":"([^"]+)"')
if ($sessMatch.Success) {
    $sid = $sessMatch.Groups[1].Value
    $resp = curl.exe -s -X DELETE "$BASE/api/sessions/$sid" -H "Authorization: Bearer $token"
    ApiOk "删除会话" $resp
}

# ===== 汇总 =====
Write-Host "`n========================================" -ForegroundColor Cyan
$total = $Global:Pass + $Global:Fail
Write-Host " 测试完成!" -ForegroundColor Cyan
Write-Host " 总计: $total  通过: $Global:Pass  失败: $Global:Fail" -ForegroundColor Cyan
if ($Global:Fail -eq 0) { Write-Host " 结果: ✅ 全部通过" -ForegroundColor Green }
else { Write-Host " 通过率: $([math]::Round($Global:Pass/$total*100,1))%" -ForegroundColor Yellow }
Write-Host "========================================" -ForegroundColor Cyan

Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue

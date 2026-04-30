#!/bin/bash
BASE="http://localhost:8081"
PASS=0; FAIL=0; TOKEN=""; SESSION_ID=""

check() { local d="$1"; local c="$2"; if [ "$c" = "true" ]; then echo "  [PASS] $d"; ((PASS++)); else echo "  [FAIL] $d"; ((FAIL++)); fi }
api_ok() { local r=$(echo "$2" | grep -q '"success":true' && echo true || echo false); check "$1" "$r"; }

echo "========================================"
echo " Knowledge Agent Platform Test Suite"
echo "========================================"

echo ""; echo "=== 1. Registration & Auth ==="
RSP=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"username":"tester1","password":"test123456"}')
api_ok "Register" "$RSP"
TOKEN=$(echo "$RSP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

RSP=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"username":"tester1","password":"test123456"}')
check "Duplicate register" "$(echo "$RSP" | grep -q 'already exists' && echo true)"

RSP=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"username":"ab","password":"12"}')
check "Short username/pw rejected" "$(echo "$RSP" | grep -q '"success":false' && echo true)"

RSP=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"username":"tester1","password":"test123456"}')
api_ok "Login" "$RSP"

RSP=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"username":"tester1","password":"wrongpass"}')
check "Wrong password rejected" "$(echo "$RSP" | grep -q '"success":false' && echo true)"

RSP=$(curl -s "$BASE/api/sessions")
check "No token rejected" "$(echo "$RSP" | grep -q '"success":false' && echo true)"

echo ""; echo "=== 2. Knowledge Base ==="
RSP=$(curl -s -X POST "$BASE/api/documents" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"title":"RAG Architecture","content":"RAG is Retrieval-Augmented Generation.","tags":["RAG","AI"]}')
api_ok "Add document" "$RSP"

RSP=$(curl -s -X POST "$BASE/api/documents" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"title":"Spring Boot","content":"Spring Boot is a Java framework.","tags":["Java","Spring"]}')
api_ok "Add 2nd document" "$RSP"

# Knowledge base search is done via keyword matching in KnowledgeService
# The /api/documents endpoint lists all documents, search is internal"

echo "content" > /tmp/up.txt
RSP=$(curl -s -X POST "$BASE/api/documents/upload" -H "Authorization: Bearer $TOKEN" -F "file=@/tmp/up.txt" -F "title=TestFile" -F "tags=test")
api_ok "File upload" "$RSP"
rm -f /tmp/up.txt

RSP=$(curl -s "$BASE/api/documents" -H "Authorization: Bearer $TOKEN")
check "List documents" "$(echo "$RSP" | grep -q 'RAG Architecture' && echo true)"

echo ""; echo "=== 3. Tools ==="
RSP=$(curl -s "$BASE/api/tools" -H "Authorization: Bearer $TOKEN")
api_ok "List tools" "$RSP"

RSP=$(curl -s -X POST "$BASE/api/tools/echo/invoke" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"input":"Hello Echo"}')
check "Echo tool" "$(echo "$RSP" | grep -q 'Hello Echo' && echo true)"

RSP=$(curl -s -X POST "$BASE/api/tools/calculator/invoke" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"input":"12 + 30"}')
check "Calculator (12+30=42)" "$(echo "$RSP" | grep -q '42' && echo true)"

RSP=$(curl -s -X POST "$BASE/api/tools/http_mock/invoke" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"input":"GET /test"}')
api_ok "HTTP mock tool" "$RSP"

RSP=$(curl -s -X POST "$BASE/api/tools/nonexistent/invoke" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"input":"test"}')
check "Non-existent tool error" "$(echo "$RSP" | grep -q '"success":false' && echo true)"

echo ""; echo "=== 4. Chat ==="
RSP=$(curl -s --max-time 30 -X POST "$BASE/api/chat" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"sessionId":"","question":"Hello, say one sentence."}')
api_ok "Chat request" "$RSP"
SESSION_ID=$(echo "$RSP" | grep -o '"sessionId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$SESSION_ID" ]; then
  RSP=$(curl -s --max-time 30 -X POST "$BASE/api/chat" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"sessionId\":\"$SESSION_ID\",\"question\":\"What did you just say?\"}")
  api_ok "Follow-up question" "$RSP"
fi

RSP=$(curl -s "$BASE/api/sessions" -H "Authorization: Bearer $TOKEN")
check "List sessions" "$(echo "$RSP" | grep -q '"success":true' && echo true)"

echo ""; echo "=== 5. Run Timeline ==="
if [ -n "$SESSION_ID" ]; then
  RSP=$(curl -s "$BASE/api/runs?sessionId=$SESSION_ID" -H "Authorization: Bearer $TOKEN")
  api_ok "Run timeline" "$RSP"
  check "Timeline has stages" "$(echo "$RSP" | grep -q '"stage"' && echo true)"
fi

echo ""; echo "=== 6. Agents & Skills ==="
RSP=$(curl -s "$BASE/api/agents" -H "Authorization: Bearer $TOKEN"); api_ok "Agents API" "$RSP"
RSP=$(curl -s "$BASE/api/skills" -H "Authorization: Bearer $TOKEN"); api_ok "Skills API" "$RSP"

echo ""; echo "=== 7. Evaluation ==="
RSP=$(curl -s -X POST "$BASE/api/evaluations" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"question":"What is RAG?","expectedKeywords":["retrieval","generation"],"feedback":""}')
api_ok "Add evaluation case" "$RSP"
CASE_ID=$(echo "$RSP" | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
RSP=$(curl -s "$BASE/api/evaluations" -H "Authorization: Bearer $TOKEN"); api_ok "List evaluations" "$RSP"

if [ -n "$CASE_ID" ]; then
  RSP=$(curl -s -X POST "$BASE/api/evaluations/run" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"caseId\":$CASE_ID,\"answer\":\"RAG (Retrieval-Augmented Generation) combines retrieval [1] and generation [2].\"}")
  check "Run evaluation" "$(echo "$RSP" | grep -q '"score":' && echo true)"
  check "Eval metrics" "$(echo "$RSP" | grep -q '"retrievalHit":true' && echo "$RSP" | grep -q '"citationPresent":true' && echo true)"
fi

echo ""; echo "=== 8. QA Review ==="
RSP=$(curl -s -X POST "$BASE/api/qa/review" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"question":"What is RAG?","answer":"RAG combines retrieval [1] and generation [2].","sources":[{"title":"RAG Paper","content":"RAG (Retrieval-Augmented Generation)"}],"expectedKeywords":["retrieval","generation","RAG"]}')
check "QA Review API" "$(echo "$RSP" | grep -q '"score":' && echo true)"
check "QA Review metrics" "$(echo "$RSP" | grep -q '"retrievalHit":true' && echo "$RSP" | grep -q '"citationPresent":true' && echo "$RSP" | grep -q '"answerContainsExpectedKeywords":true' && echo true)"

echo ""; echo "=== 9. Trace ==="
RSP=$(curl -s "$BASE/api/traces?limit=5" -H "Authorization: Bearer $TOKEN")
api_ok "Traces" "$RSP"
check "Trace has valid stages" "$(echo "$RSP" | grep -q 'USER_INPUT\|CONTEXT_BUILD\|QA_REVIEW' && echo true)"

echo ""; echo "=== 10. JavaScript Syntax ==="
curl -s "$BASE/" > /tmp/pchk.html
grep -o '<script>.*</script>' /tmp/pchk.html | sed 's/<script>//;s/<\/script>//' > /tmp/chkjs.js
node --check /tmp/chkjs.js 2>/dev/null && check "JS syntax valid" "true" || check "JS syntax valid" "false"
rm -f /tmp/pchk.html /tmp/chkjs.js

echo ""; echo "=== 11. SSE Stream ==="
timeout 15 curl -s -N -X POST "$BASE/api/chat/stream" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Accept: text/event-stream" -d '{"sessionId":"","question":"Hi"}' > /tmp/sse_out.txt 2>/dev/null &
SPID=$!; sleep 12; kill $SPID 2>/dev/null; wait $SPID 2>/dev/null
SCNT=$(grep -c "event:" /tmp/sse_out.txt 2>/dev/null)
check "SSE events ($SCNT)" "$( [ $SCNT -gt 0 ] && echo true || echo false )"
rm -f /tmp/sse_out.txt

echo ""; echo "=== 12. Cleanup ==="
DID=$(curl -s "$BASE/api/documents" -H "Authorization: Bearer $TOKEN" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
[ -n "$DID" ] && RSP=$(curl -s -X DELETE "$BASE/api/documents/$DID" -H "Authorization: Bearer $TOKEN") && api_ok "Delete document" "$RSP"

echo ""; echo "========================================"
TOTAL=$((PASS+FAIL)); echo "  Total: $TOTAL  Pass: $PASS  Fail: $FAIL"
[ $FAIL -eq 0 ] && echo "  Result: ALL PASSED" || echo "  Result: $((PASS*100/TOTAL))%"
echo "========================================"
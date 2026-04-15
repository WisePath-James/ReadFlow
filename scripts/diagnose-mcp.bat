@echo off
echo ==========================================
echo ReadFlow MCP 快速诊断工具
echo ==========================================
echo.

echo [1/5] 检查环境变量...
if defined SUPABASE_PROJECT_ID (
    echo   OK: SUPABASE_PROJECT_ID 已设置
) else (
    echo   WARN: SUPABASE_PROJECT_ID 未设置
)

if defined SUPABASE_ACCESS_TOKEN (
    echo   OK: SUPABASE_ACCESS_TOKEN 已设置
) else (
    echo   WARN: SUPABASE_ACCESS_TOKEN 未设置
)

if defined OPENAI_API_KEY (
    echo   OK: OPENAI_API_KEY 已设置
) else (
    echo   WARN: OPENAI_API_KEY 未设置
)

echo.
echo [2/5] 检查 MCP 配置文件...
if exist ".cursor\mcp.json" (
    echo   OK: .cursor\mcp.json 存在
    type ".cursor\mcp.json" | findstr "mcpServers" > nul && (
        echo   OK: 配置文件格式正确
    ) || (
        echo   ERROR: 配置文件格式可能有问题
    )
) else (
    echo   ERROR: .cursor\mcp.json 未找到
)

echo.
echo [3/5] 检查项目结构...
for %%d in (backend ios supabase docs scripts) do (
    if exist "%%d" (
        echo   OK: %%d\
    ) else (
        echo   MISSING: %%d\
    )
)

echo.
echo [4/5] 测试 MCP 包...
echo   测试 Supabase MCP...
npx -y @supabase/mcp-server@latest --help >nul 2>&1
if errorlevel 1 (
    echo   WARN: Supabase MCP 包测试失败（可能需要网络）
) else (
    echo   OK: Supabase MCP 包可用
)

echo.
echo [5/5] Cursor 状态检查...
echo   请手动检查：
echo   - Cursor 左侧边栏是否有 MCP 图标
echo   - 图标是绿色（连接）还是红色（断开）
echo   - 按 Ctrl+Shift+P 运行 "Show MCP Logs"
echo.

echo ==========================================
echo 诊断完成！
echo ==========================================
echo.
echo 下一步：
echo 1. 如果环境变量未设置，请设置它们
echo 2. 完全重启 Cursor（任务管理器结束进程）
echo 3. 等待 30 秒观察 MCP 图标
echo 4. 查看详细文档：docs\mcp-troubleshooting.md
echo.
pause

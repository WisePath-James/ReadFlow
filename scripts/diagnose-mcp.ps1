# MCP 连接诊断脚本
Write-Host "=== ReadFlow MCP 诊断工具 ===" -ForegroundColor Cyan
Write-Host ""

# 检查环境变量
Write-Host "1) 检查环境变量..." -ForegroundColor Yellow
$envVars = @('SUPABASE_PROJECT_ID', 'SUPABASE_ACCESS_TOKEN', 'OPENAI_API_KEY', 'POSTGRES_CONNECTION_STRING')
foreach ($var in $envVars) {
    $value = [Environment]::GetEnvironmentVariable($var)
    if ($value) {
        Write-Host "  ✓ $var = $($value.Substring(0, [Math]::Min(20, $value.Length)))..." -ForegroundColor Green
    } else {
        Write-Host "  ⚠  $var 未设置" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "2) 检查配置文件..." -ForegroundColor Yellow
$mcpConfig = Join-Path $PSScriptRoot ".cursor\mcp.json"
if (Test-Path $mcpConfig) {
    Write-Host "  ✓ MCP 配置文件存在" -ForegroundColor Green
    try {
        $json = Get-Content $mcpConfig | ConvertFrom-Json
        Write-Host "  📋 配置的服务器："
        $json.mcpServers.PSObject.Properties.Name | ForEach-Object {
            Write-Host "    • $_"
        }
    } catch {
        Write-Host "  ❌ 配置文件 JSON 格式错误" -ForegroundColor Red
    }
} else {
    Write-Host "  ❌ MCP 配置文件未找到: $mcpConfig" -ForegroundColor Red
}

Write-Host ""
Write-Host "3) 检查项目结构..." -ForegroundColor Yellow
$dirs = @('backend', 'ios', 'supabase', 'docs', 'scripts')
foreach ($dir in $dirs) {
    $path = Join-Path $PSScriptRoot $dir
    if (Test-Path $path) {
        Write-Host "  ✓ $dir/" -ForegroundColor Green
    } else {
        Write-Host "  ❌ $dir/ 缺失" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "4) 测试 MCP 包可用性..." -ForegroundColor Yellow
$packages = @(
    '@supabase/mcp-server',
    '@modelcontextprotocol/server-filesystem',
    '@modelcontextprotocol/server-sqlite',
    '@modelcontextprotocol/server-postgres'
)

foreach ($pkg in $packages) {
    try {
        $null = npx --yes $pkg@latest --help 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ $pkg 可用" -ForegroundColor Green
        } else {
            Write-Host "  ⚠  $pkg 运行失败 (退出码: $LASTEXITCODE)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ❌ $pkg 错误: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "5) 建议操作：" -ForegroundColor Cyan
Write-Host "  1. 确保环境变量已设置"
Write-Host "     运行: [Environment]::GetEnvironmentVariable('SUPABASE_PROJECT_ID')"
Write-Host "  2. 完全重启 Cursor IDE（任务管理器结束进程）"
Write-Host "  3. 等待 30 秒观察 MCP 图标变绿"
Write-Host "  4. 如果仍失败，查看日志："
Write-Host "     - Cursor → Command Palette → 'Show MCP Logs'"
Write-Host "     - 或检查 %APPDATA%\Cursor\logs\"
Write-Host ""
Write-Host "详细故障排除：docs/mcp-troubleshooting.md" -ForegroundColor Cyan
Write-Host ""

#!/bin/bash
# ReadFlow 项目初始化脚本

set -e

echo "=========================================="
echo "ReadFlow 项目初始化"
echo "=========================================="

# 检查 Node.js
echo "检查 Node.js..."
if ! command -v node &> /dev/null; then
    echo "❌ 未安装 Node.js，请先安装 Node.js 18+"
    exit 1
fi
echo "✅ Node.js 版本: $(node -v)"

# 检查 npm
echo "检查 npm..."
if ! command -v npm &> /dev/null; then
    echo "❌ 未安装 npm"
    exit 1
fi
echo "✅ npm 版本: $(npm -v)"

# 创建环境配置文件
echo ""
echo "创建环境配置文件..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "✅ 已创建 .env 文件，请编辑填入真实配置"
else
    echo "⚠️  .env 文件已存在，跳过创建"
fi

# 安装后端依赖
echo ""
echo "安装后端依赖..."
cd backend
if [ ! -d node_modules ]; then
    npm install
    echo "✅ 依赖安装完成"
else
    echo "⚠️  依赖已安装"
fi
cd ..

# 检查 Supabase 配置
echo ""
echo "检查 Supabase 配置..."
if [ -z "$SUPABASE_PROJECT_ID" ]; then
    echo "⚠️  未设置 SUPABASE_PROJECT_ID 环境变量"
    echo "   请在 .env 文件中配置，或使用:"
    echo "   export SUPABASE_PROJECT_ID=your_project_id"
fi

if [ -z "$SUPABASE_ACCESS_TOKEN" ]; then
    echo "⚠️  未设置 SUPABASE_ACCESS_TOKEN 环境变量"
fi

# 检查 OpenAI 配置
echo ""
echo "检查 OpenAI 配置..."
if [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  未设置 OPENAI_API_KEY 环境变量"
fi

echo ""
echo "=========================================="
echo "初始化完成！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 编辑 .env 文件，填入真实配置"
echo "2. 初始化 Supabase 数据库："
echo "   supabase login"
echo "   supabase link --project-ref YOUR_PROJECT_ID"
echo "   supabase db push"
echo ""
echo "3. 启动开发服务器："
echo "   cd backend && npm run dev"
echo ""
echo "4. 打开 iOS 项目："
echo "   cd ios && pod install"
echo "   open ReadFlow.xcworkspace"
echo ""
echo "📚 详细文档请查看：docs/development.md"
echo ""

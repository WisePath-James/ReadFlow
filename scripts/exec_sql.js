const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

// 读取 SQL 文件
const fs = require('fs');
const path = require('path');

const sqlFilePath = path.join(__dirname, 'create_schema.sql');
const sql = fs.readFileSync(sqlFilePath, 'utf8');

// 分割 SQL 语句（按分号分割）
const statements = sql
  .split(';')
  .map(s => s.trim())
  .filter(s => s.length > 0 && !s.startsWith('--'));

console.log(`🚀 开始执行 ${statements.length} 条 SQL 语句...\n`);

let success = 0;
let failed = 0;

async function executeAll() {
  for (let i = 0; i < statements.length; i++) {
    const stmt = statements[i] + ';';
    try {
      // 使用 supabase-js 的 query 方法执行原始 SQL
      const { error } = await supabase.rpc('exec_sql', { sql: stmt });

      if (error) {
        // 如果 exec_sql 不存在，尝试使用 PostgREST 的 /rpc 端点
        console.log(`⚠️  语句 ${i + 1}: ${error.message.substring(0, 60)}... (可能需要手动执行)`);
        failed++;
      } else {
        console.log(`✅ [${i + 1}/${statements.length}] 执行成功`);
        success++;
      }
    } catch (error) {
      console.log(`❌ [${i + 1}/${statements.length}] 错误: ${error.message.substring(0, 60)}...`);
      failed++;
    }
  }

  console.log('\n' + '='.repeat(50));
  console.log('📊 执行结果统计:');
  console.log(`   ✅ 成功: ${success}`);
  console.log(`   ⚠️  需手动: ${failed}`);
  console.log('='.repeat(50));

  if (failed > 0) {
    console.log('\n💡 提示:');
    console.log('1. 打开 Supabase Dashboard → SQL Editor');
    console.log('2. 粘贴 scripts/create_schema.sql 全部内容');
    console.log('3. 点击 Run 执行');
    console.log('\n或手动执行失败的语句。');
  } else {
    console.log('\n🎉 数据库创建完成！');
    console.log('接下来运行: npm run gen:types 生成 TypeScript 类型');
  }
}

executeAll().catch(console.error);

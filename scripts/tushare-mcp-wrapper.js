/**
 * Tushare MCP stdio 包装器
 *
 * 问题：@hestudy/tushare-mcp 将日志输出到 stdout，违反了 MCP stdio 协议
 *      (stdout 只能包含 JSON-RPC 消息，每行一条)。
 *
 * 解决：此包装器过滤子进程的 stdout：
 *      - JSON-RPC 消息 → 透传到父进程 stdout
 *      - 非 JSON 文本（日志）→ 重定向到父进程 stderr
 */

const { spawn } = require('child_process');
const path = require('path');

const fs = require('fs');

// 查找 npx 路径
const NPX_PATHS = [
    path.join(process.env.USERPROFILE || process.env.HOME || '', '.workbuddy', 'binaries', 'node', 'versions', '22.22.2', 'npx.cmd'),
];

let npxCmd = null;
for (const p of NPX_PATHS) {
    if (fs.existsSync(p)) {
        npxCmd = p;
        break;
    }
}

if (!npxCmd) {
    // fallback to system npx
    npxCmd = process.platform === 'win32' ? 'npx.cmd' : 'npx';
}

process.stderr.write('[wrapper] using npx: ' + npxCmd + '\n');

// Windows 上 spawn .cmd 需要 shell: true
const isWin = process.platform === 'win32';
const child = spawn(npxCmd, ['-y', '@hestudy/tushare-mcp@latest'], {
    env: {
        ...process.env,
        LOG_LEVEL: process.env.TUSHARE_MCP_LOG_LEVEL || 'error',
    },
    stdio: ['pipe', 'pipe', 'pipe'],
    shell: isWin,
});

// 透传 stdin
process.stdin.pipe(child.stdin);

// 过滤 stdout：JSON-RPC 消息透传，其他行 → stderr
let buffer = '';
child.stdout.on('data', (chunk) => {
    buffer += chunk.toString();
    const lines = buffer.split('\n');
    // 最后一段可能不完整，保留在 buffer
    buffer = lines.pop() || '';

    for (const line of lines) {
        if (!line.trim()) continue;
        if (isJsonRpcMessage(line)) {
            process.stdout.write(line + '\n');
        } else {
            process.stderr.write('[tushare-mcp stdout→stderr] ' + line + '\n');
        }
    }
});

child.stdout.on('end', () => {
    // 处理残留
    if (buffer.trim()) {
        if (isJsonRpcMessage(buffer)) {
            process.stdout.write(buffer + '\n');
        } else {
            process.stderr.write('[tushare-mcp stdout→stderr] ' + buffer + '\n');
        }
    }
});

// 透传 stderr
child.stderr.pipe(process.stderr);

// 退出码传播
child.on('close', (code) => {
    process.exit(code || 0);
});

process.on('SIGINT', () => child.kill('SIGINT'));
process.on('SIGTERM', () => child.kill('SIGTERM'));

/**
 * 判断一行文本是否为有效的 JSON-RPC 消息
 */
function isJsonRpcMessage(line) {
    const trimmed = line.trim();
    if (!trimmed.startsWith('{')) return false;
    try {
        const obj = JSON.parse(trimmed);
        // JSON-RPC 2.0 消息必须有 jsonrpc 字段
        return obj && obj.jsonrpc === '2.0';
    } catch (_) {
        return false;
    }
}

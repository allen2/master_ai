/**
 * Mock Tushare MCP Server
 *
 * 实现 MCP stdio JSON-RPC 协议，返回测试数据。
 * 用于验证 Spring AI MCP Client 集成流程。
 */

const readline = require('readline');

const SERVER_INFO = {
    name: 'tushare-mock',
    version: '1.0.0',
};

// ---- 模拟的 MCP 工具列表 ----
const TOOLS = [
    {
        name: 'stock_basic',
        description: '获取A股股票列表，返回ts_code/symbol/name/area/industry/list_date等信息',
        inputSchema: {
            type: 'object',
            properties: {
                ts_code: { type: 'string', description: '股票代码，如 000001.SZ' },
                name:   { type: 'string', description: '股票名称，支持模糊查询' },
            },
        },
    },
    {
        name: 'daily',
        description: '获取A股日线行情，返回开高低收/成交量/成交额等',
        inputSchema: {
            type: 'object',
            properties: {
                ts_code:  { type: 'string', description: '股票代码' },
                start_date: { type: 'string', description: '开始日期 YYYYMMDD' },
                end_date:   { type: 'string', description: '结束日期 YYYYMMDD' },
            },
            required: ['ts_code'],
        },
    },
    {
        name: 'income',
        description: '获取A股利润表数据',
        inputSchema: {
            type: 'object',
            properties: {
                ts_code:  { type: 'string', description: '股票代码' },
                start_date: { type: 'string', description: '开始日期 YYYYMMDD' },
                end_date:   { type: 'string', description: '结束日期 YYYYMMDD' },
            },
            required: ['ts_code'],
        },
    },
    {
        name: 'balance_sheet',
        description: '获取A股资产负债表数据',
        inputSchema: {
            type: 'object',
            properties: {
                ts_code:  { type: 'string', description: '股票代码' },
                start_date: { type: 'string', description: '开始日期 YYYYMMDD' },
                end_date:   { type: 'string', description: '结束日期 YYYYMMDD' },
            },
            required: ['ts_code'],
        },
    },
];

// ---- 模拟数据 ----
const MOCK_DATA = {
    stock_basic: [
        { ts_code: '000001.SZ', symbol: '000001', name: '平安银行', area: '深圳', industry: '银行', list_date: '19910403' },
        { ts_code: '600519.SH', symbol: '600519', name: '贵州茅台', area: '贵州', industry: '白酒', list_date: '20010827' },
        { ts_code: '000858.SZ', symbol: '000858', name: '五粮液',   area: '四川', industry: '白酒', list_date: '19980427' },
    ],
    daily: [
        { ts_code: '000001.SZ', trade_date: '20260601', open: 10.50, high: 10.80, low: 10.30, close: 10.65, vol: 50000000, amount: 530000000 },
        { ts_code: '000001.SZ', trade_date: '20260602', open: 10.68, high: 10.95, low: 10.55, close: 10.88, vol: 62000000, amount: 665000000 },
        { ts_code: '000001.SZ', trade_date: '20260603', open: 10.90, high: 11.20, low: 10.80, close: 11.05, vol: 58000000, amount: 640000000 },
        { ts_code: '000001.SZ', trade_date: '20260604', open: 11.00, high: 11.15, low: 10.70, close: 10.72, vol: 45000000, amount: 495000000 },
    ],
    income: [
        {
            ts_code: '000001.SZ',
            end_date: '20251231',
            total_revenue: 1.8e11,
            revenue: 1.8e11,
            operating_profit: 5.2e10,
            total_profit: 5.1e10,
            n_income: 4.2e10,
            basic_eps: 2.15,
            report_type: '1',
        },
    ],
    balance_sheet: [
        {
            ts_code: '000001.SZ',
            end_date: '20251231',
            total_assets: 5.5e12,
            total_liab: 5.1e12,
            total_hldr_eqy_exc_min_int: 4.0e11,
            report_type: '1',
        },
    ],
};

// ---- JSON-RPC 处理器 ----
function success(id, result) {
    return JSON.stringify({ jsonrpc: '2.0', id, result }) + '\n';
}

function error(id, code, message) {
    return JSON.stringify({ jsonrpc: '2.0', id, error: { code, message } }) + '\n';
}

function handleRequest(msg) {
    if (!msg || typeof msg !== 'object') return;
    const { id, method, params } = msg;

    switch (method) {
        case 'initialize':
            return success(id, {
                protocolVersion: '2024-11-05',
                capabilities: { tools: {} },
                serverInfo: SERVER_INFO,
            });

        case 'notifications/initialized':
            // 无需回复
            return null;

        case 'tools/list':
            return success(id, { tools: TOOLS });

        case 'tools/call': {
            const toolName = params?.name;
            const args = params?.arguments || {};
            const data = MOCK_DATA[toolName];
            if (!data) {
                return error(id, -32601, `Unknown tool: ${toolName}`);
            }
            return success(id, {
                content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
            });
        }

        default:
            return error(id, -32601, `Unknown method: ${method}`);
    }
}

// ---- stdio 读写 ----
const rl = readline.createInterface({ input: process.stdin });

// 写入 stderr 的日志（不污染 stdout 协议通道）
function log(msg) {
    process.stderr.write('[mock-tushare] ' + msg + '\n');
}

log('Mock Tushare MCP Server started');

rl.on('line', (line) => {
    line = line.trim();
    if (!line) return;

    log('← ' + line.substring(0, 120));

    let msg;
    try {
        msg = JSON.parse(line);
    } catch (e) {
        log('Failed to parse JSON: ' + e.message);
        return;
    }

    const resp = handleRequest(msg);
    if (resp) {
        log('→ ' + resp.trim().substring(0, 120));
        process.stdout.write(resp);
    }
});

rl.on('close', () => {
    log('stdin closed, exiting');
    process.exit(0);
});

package com.aihedgefund.orchestrator;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 分析师画像注册表
 *
 * 为全部 17 个分析师提供五位一体结构化画像：
 *   1. 基础背景  2. 核心投资哲学  3. 实操体系  4. 业绩与特质  5. 局限性
 *
 * 按 displayName 排序，前端可直接渲染。
 */
@Component
public class AgentProfileRegistry {

    private final Map<String, AgentProfile> profiles = new LinkedHashMap<>();

    public AgentProfileRegistry() {
        // ===== 投资者型（按影响力排序） =====
        registerWarrenBuffett();
        registerBenGraham();
        registerCharlieMunger();
        registerPhilFisher();
        registerPeterLynch();
        registerAswathDamodaran();
        registerNassimTaleb();
        registerStanleyDruckenmiller();
        registerMichaelBurry();
        registerBillAckman();
        registerCathieWood();
        registerMohnishPabrai();
        registerRakeshJhunjhunwala();

        // ===== 量化/基本面分析师 =====
        registerFundamentals();
        registerGrowth();
        registerSentiment();
        registerTechnical();

        // ===== 专项分析师 =====
        registerValuation();
        registerNewsSentiment();
    }

    /** 获取全部画像列表（按 sortOrder 排序） */
    public List<AgentProfile> listAll() {
        List<AgentProfile> list = new ArrayList<>(profiles.values());
        list.sort(Comparator.comparingInt(AgentProfile::getSortOrder));
        return list;
    }

    /** 按 agentId 查找 */
    public Optional<AgentProfile> getByAgentId(String agentId) {
        return Optional.ofNullable(profiles.get(agentId));
    }

    /** 获取 investor 类画像 */
    public List<AgentProfile> listByCategory(String category) {
        List<AgentProfile> list = new ArrayList<>();
        for (AgentProfile p : profiles.values()) {
            if (category.equals(p.getCategory())) list.add(p);
        }
        return list;
    }

    // =========================================================
    //  画像定义
    // =========================================================

    // ---------- 投资者型 ----------

    private void registerWarrenBuffett() {
        profiles.put("warren_buffett", new AgentProfile(
                "warren_buffett", "沃伦·巴菲特", "investor", 1,
                background("价值投资集大成者，奥马哈先知，伯克希尔·哈撒韦 CEO",
                        "师从格雷厄姆，从业超 60 年，以保险浮存金构建商业帝国",
                        "核心流派：深度价值+护城河理论；代表作《巴菲特致股东的信》"),
                philosophy(
                        "以合理价格买入伟大企业，永远关注护城河的宽度",
                        "赚取企业长期内在价值增长——时间是好公司的朋友",
                        "安全边际+护城河+管理层质量三重过滤；绝不投资看不懂的东西",
                        "永久持有（最爱持有期限是永远）；不出售有持久竞争优势的企业"),
                methodology(
                        "偏好消费垄断型企业（可口可乐、苹果），要求 ROE>15%、持续 10 年以上盈利增长",
                        "市场恐慌时贪婪买入；别人贪婪时恐惧。不试图择时但利用市场情绪",
                        "高度集中持仓（前 5 大持仓占 70%+）；保留大量现金应对机会",
                        "绝不使用杠杆；永久资本损失为最大风险"),
                trackRecord(
                        "55 年复合年化收益 ~20%，伯克希尔从纺织厂转型为全球 Top10 市值公司",
                        "极度理性、极度耐心、极度专注；性格特征：简朴生活、热爱阅读"),
                limitations(
                        "对科技股长期回避（近年才少量持有苹果），错失互联网红利期",
                        "模型极度依赖企业管理层诚信，管理层欺诈时完全失效",
                        "不适合短线交易者参考；在快速变化行业中适配度较低")
        ));
    }

    private void registerBenGraham() {
        profiles.put("ben_graham", new AgentProfile(
                "ben_graham", "本杰明·格雷厄姆", "investor", 2,
                background("价值投资之父，哥伦比亚商学院教授，从业数十年",
                        "亲历 1929 美股大崩盘，由此塑造极度保守的投资体系",
                        "核心流派：深度价值+安全边际；代表作《聪明的投资者》《证券分析》"),
                philosophy(
                        "市场短期是投票机，长期是称重机，市场时常非理性",
                        "赚取企业内在价值与市场价格的差值（估值修复）",
                        "安全边际（40 美分买 1 美元的资产）为第一准则",
                        "中长期持有，拒绝短线投机"),
                methodology(
                        "优先低 PE（<15）、低 PB（<1.5）、高流动资产的冷门低估股；规避高估值题材股、高负债企业",
                        "市场恐慌、标的深度低估时分批买入，估值回归合理区间卖出",
                        "严格分散，单只个股仓位极低，不重仓、不使用杠杆",
                        "安全边际为第一防线，基本面恶化无条件离场"),
                trackRecord(
                        "穿越多轮牛熊，体系以稳健防守著称",
                        "极度理性，厌恶投机，擅长从风险角度审视标的"),
                limitations(
                        "偏向传统工业、蓝筹标的，对高成长科技股、轻资产企业适配度低",
                        "在纯题材牛市中收益偏弱，经常因低估陷阱提前离场")
        ));
    }

    private void registerCharlieMunger() {
        profiles.put("charlie_munger", new AgentProfile(
                "charlie_munger", "查理·芒格", "investor", 3,
                background("巴菲特 60 年合伙人，伯克希尔副主席",
                        "律师出身，自学跨学科知识体系，被誉为“行走的书架”",
                        "核心流派：跨学科思维+高质量价值；代表作《穷查理宝典》"),
                philosophy(
                        "以合理价格买入伟大企业，远胜以便宜价格买入平庸企业",
                        "赚取企业复利增长的红利，高投入资本回报率是核心",
                        "逆向思维——凡事反过来想：什么可能出错？避免愚蠢比追求聪明更重要",
                        "长期持有，几乎不卖出（卖出伟大企业是愚蠢的）"),
                methodology(
                        "偏好高 ROIC、低负债、诚实管理层、持久护城河的企业",
                        "极少择时——耐心等待最佳击球点出现时才出手",
                        "高度集中，极低换手率；一生只需少数几个绝佳投资",
                        "极度厌恶杠杆；能力圈原理——只投资真正理解的东西"),
                trackRecord(
                        "协助巴菲特将伯克希尔从价值投资转向高质量投资，铸就传奇",
                        "极度智慧、直言不讳、学贯中西；性格：理性到近乎冷酷"),
                limitations(
                        "对新商业模式接受慢（早年拒绝亚马逊），偏好有形资产型企业",
                        "风格极度集中，散户模仿风险极高")
        ));
    }

    private void registerPhilFisher() {
        profiles.put("phil_fisher", new AgentProfile(
                "phil_fisher", "菲利普·费雪", "investor", 4,
                background("成长股投资之父，斯坦福商学院出身",
                        "1931 年创办费雪公司，从业超 70 年",
                        "核心流派：成长股投资+闲聊法；代表作《怎样选择成长股》"),
                philosophy(
                        "长期持有卓越成长企业的股票是财富增长的最佳方式",
                        "赚取企业复利增长的红利——买对公司比买得便宜更重要",
                        "买入并持有数十年，只有三种情况卖出：当初判断错误、公司基本面恶化、发现更好的标的",
                        "超长期，10-30 年"),
                methodology(
                        "管理层卓越、产品持续创新、研发投入充足、销售体系完善、利润率高于行业均值",
                        "不择时——有足够安全边际就买入，分批建仓",
                        "集中持仓，重仓最佳标的；不追求分散",
                        "深度跟踪，通过行业人脉持续监控管理层变动"),
                trackRecord(
                        "1955 年买入摩托罗拉持有至逝世，收益超 20 倍",
                        "极度严谨、追求深度信息、偏执于质量"),
                limitations(
                        "极度依赖深度调研能力，普通投资者难以复制“闲聊法”",
                        "对低估值强周期股不关注，错过困境反转机会")
        ));
    }

    private void registerPeterLynch() {
        profiles.put("peter_lynch", new AgentProfile(
                "peter_lynch", "彼得·林奇", "investor", 5,
                background("传奇基金经理，1977-1990 管理麦哲伦基金，规模从 1800 万至 140 亿美元",
                        "早慧投资者，大学期间投资获利支付学费",
                        "核心流派：成长+价值混合（GARP）；代表作《彼得·林奇的成功投资》"),
                philosophy(
                        "投资你了解的领域——普通人比华尔街更早发现好公司",
                        "PEG 是核心指标：PEG<1 的中小盘公司就是十倍股候选",
                        "对股票分类投（高速成长/稳健增长/周期股/困境反转），不同类型不同策略",
                        "灵活调整，好公司持续持有，基本面变化即卖"),
                methodology(
                        "偏好 PEG<1、商业模式清晰、业务通俗易懂的中小市值公司",
                        "不择时——用投资组合管理替代择时判断，持续买入",
                        "极端分散（麦哲伦基金曾持有 1400+ 只股票），但重仓最佳标的",
                        "胜率不重要——10 只中 2 只十倍股足以补偿 8 只亏损"),
                trackRecord(
                        "13 年年化 29.2%，同期标普 500 年化 15.8%，无一年亏损",
                        "极度勤奋（每年拜访 500+ 家公司）；幽默感强；散户友好"),
                limitations(
                        "分散风格强化牛市中收益会被稀释",
                        "依赖大量实地调研，纯靠数据难以复制林奇体系")
        ));
    }

    private void registerAswathDamodaran() {
        profiles.put("aswath_damodaran", new AgentProfile(
                "aswath_damodaran", "阿斯沃斯·达摩达兰", "investor", 6,
                background("纽约大学斯特恩商学院估值学教授，号称“估值教父”",
                        "著有《估值》《投资哲学》等教科书级作品",
                        "核心流派：DCF 估值+叙事与数字融合"),
                philosophy(
                        "估值 = 故事 + 数字：故事赋予数字意义，数字验证故事可信度",
                        "赚取价格与内在价值的差距——但内在价值并非精确数字而是区间",
                        "核心驱动因素（Key Value Drivers）决定企业价值的 80%",
                        "估值合理即可买入，不追求绝对底部"),
                methodology(
                        "DCF 为主，结合 PE、EV/EBITDA 横向对比；重点关注增长假设的合理性",
                        "通过估值缺口（价格 vs 内在价值）判断，不依赖宏观择时",
                        "分散但不极端；对高不确定性标的要求更大安全边际",
                        "所有参数公开透明，可复盘验证，拒绝黑箱模型"),
                trackRecord(
                        "学术估值框架被全球投行广泛采用",
                        "极度严谨、学术化、追求可验证性"),
                limitations(
                        "DCF 模型对超高速成长/负利润的早期科技公司适配度低",
                        "依赖大量假设，不同分析师得出结果差异巨大")
        ));
    }

    private void registerNassimTaleb() {
        profiles.put("nassim_taleb", new AgentProfile(
                "nassim_taleb", "纳西姆·塔勒布", "investor", 7,
                background("黑天鹅理论开创者，前量化交易员，风险分析师",
                        "2008 金融危机前预警并获得巨大收益",
                        "核心流派：尾部风险对冲+反脆弱；代表作《黑天鹅》《反脆弱》"),
                philosophy(
                        "世界由罕见极端事件驱动——预测毫无意义，准备才是关键",
                        "赚取市场脆弱性定价错误——多数投资者低估尾部风险",
                        "哑铃策略：90% 极端安全+10% 极端投机，拒绝平庸的中等风险",
                        "长期持有反脆弱资产，极端事件后迅速调整"),
                methodology(
                        "偏好能从波动中受益的企业（凸性收益），如生物科技、期权类策略",
                        "不择时——永远持有尾部对冲保护",
                        "哑铃结构：极端保守+极端进攻，零中等风险暴露",
                        "极度厌恶杠杆和高负债企业，规避单点故障型脆弱系统"),
                trackRecord(
                        "2008 年危机中为客户获得超百亿美元收益",
                        "极度独立、厌恶共识、反主流；性格：尖锐、哲学化"),
                limitations(
                        "长期持有对冲保护有成本，牛市中跑输市场",
                        "偏学术哲学化，普通投资者难以执行哑铃策略和凸性分析")
        ));
    }

    private void registerStanleyDruckenmiller() {
        profiles.put("stanley_druckenmiller", new AgentProfile(
                "stanley_druckenmiller", "斯坦利·德鲁肯米勒", "investor", 8,
                background("宏观对冲传奇，索罗斯量子基金首席基金经理",
                        "1988-2000 连续 12 年盈利，30 年年化 ~30%",
                        "核心流派：宏观对冲+动量驱动"),
                philosophy(
                        "不对称机会——高上行潜力、有限下行风险才是好投资",
                        "赚取宏观趋势+盈利动能的共振收益",
                        "保持灵活，随时准备快速转变观点——投资世界里不存在永恒真理",
                        "持有至逻辑失效即走，不设固定期限"),
                methodology(
                        "宏观自上而下+个股自下而上双重过滤；跟随盈利动能和价格动量",
                        "趋势确认后重仓进入，动能衰减时果断离场",
                        "敢于重仓高确信度机会，对一般机会快速砍仓",
                        "严格止损，截断亏损让利润奔跑"),
                trackRecord(
                        "1992 年协助索罗斯做空英镑获利 10 亿美元",
                        "极度灵活、杀伐果断、大局观强"),
                limitations(
                        "高度依赖宏观判断准确性，宏观失误时损失严重",
                        "快速换仓风格不适合被动投资者模仿")
        ));
    }

    private void registerMichaelBurry() {
        profiles.put("michael_burry", new AgentProfile(
                "michael_burry", "迈克尔·伯里", "investor", 9,
                background("《大空头》原型，Scion Capital 创始人",
                        "医生出身，自学投资，以 2008 次贷做空闻名",
                        "核心流派：深度逆向价值+非对称做空"),
                philosophy(
                        "市场大多数时候是错的——共识往往是泡沫的温床",
                        "在无人问津处发现低估价值，在众人狂欢时识别泡沫",
                        "深入研究，独立判断，绝不从众",
                        "耐心等待催化剂出现，不设时间表"),
                methodology(
                        "偏好极低 PE/自由现金流比、存在隐藏资产或业务催化剂的逆向标的",
                        "基金持仓无流动限制时敢于重仓等待",
                        "高度集中，少量标的，深度研究",
                        "只看绝对价值和安全边际，不参考相对估值"),
                trackRecord(
                        "2000-2008 年化超 20%，2007-2008 做空 CDS 获利超 7 亿美元",
                        "极度逆向、深度钻研、孤僻；社交恐惧症但分析能力超群"),
                limitations(
                        "极度逆向风格可能在泡沫延续时长期亏损",
                        "独狼风格不适合团队协作的投资场景")
        ));
    }

    private void registerBillAckman() {
        profiles.put("bill_ackman", new AgentProfile(
                "bill_ackman", "比尔·阿克曼", "investor", 10,
                background("激进投资者，潘兴广场资本创始人",
                        "哈佛 MBA，从业超 25 年",
                        "核心流派：激进主义+股东积极主义"),
                philosophy(
                        "优质企业若管理不善就像蒙尘明珠——更换管理层即可释放价值",
                        "赚取企业治理改善带来的价值重估收益",
                        "深入研究一家公司胜过浅尝百家公司",
                        "中长线，持有至管理层改善到位"),
                methodology(
                        "寻找商业模式强劲但资本配置低效、管理层表现不佳的企业",
                        "通过股东积极主义推动变革——进入董事会或公开施压",
                        "极度集中（通常 5-8 只股票），每只深入研究",
                        "做空也分散，避免裸空；失败时认错立场"),
                trackRecord(
                        "疫情期间信用 CDS 对冲获利超 26 亿美元",
                        "极度自信、善于公开辩论、有争议但有战绩"),
                limitations(
                        "激进主义偶有翻车（康宝莱做空惨败，亏损近 10 亿美元）",
                        "极度集中风险不适合普通投资者，公开对抗管理层操作门槛高")
        ));
    }

    private void registerCathieWood() {
        profiles.put("cathie_wood", new AgentProfile(
                "cathie_wood", "凯西·伍德", "investor", 11,
                background("Ark Invest 创始人兼 CEO，颠覆性创新投资旗帜人物",
                        "从业 40 年，曾在 AllianceBernstein 管理超 50 亿美元科技基金",
                        "核心流派：颠覆性创新+主题投资"),
                philosophy(
                        "颠覆性创新公司将在 5-10 年获得指数级增长——传统估值模型无法捕捉",
                        "赚取技术革命带来的非线性增长红利",
                        "承认短期波动，相信长期趋势——波动不是风险，错失颠覆性变革才是",
                        "超长期（5-10 年），无视季度波动"),
                methodology(
                        "聚焦 AI、基因组学、金融科技、自动驾驶、航天等颠覆性技术赛道",
                        "逢低加仓、越跌越买——坚信短期回调是买入机会",
                        "绝对重仓最佳标的（特斯拉占 ARKK 最高曾达 10%+），换手率较高",
                        "通过公开研究（博客、白皮书、邮件）增强持仓透明度"),
                trackRecord(
                        "2020 年 ARKK ETF 年收益 152.8% 一战封神",
                        "极具争议——狂热信徒与严厉批评者并存"),
                limitations(
                        "2021-2022 ARKK 回撤超 75%，高波动、大回撤是常态",
                        "对利率极度敏感——加息周期中颠覆性成长股首要冲击对象",
                        "科技叙事驱动的风格在泡沫期容易被故事蒙蔽")
        ));
    }

    private void registerMohnishPabrai() {
        profiles.put("mohnish_pabrai", new AgentProfile(
                "mohnish_pabrai", "莫尼什·帕布莱", "investor", 12,
                background("Dhandho 投资者，Pabrai 投资基金创始人",
                        "印度裔美国企业家转型投资者，受巴菲特、芒格影响深刻",
                        "核心流派：Dhandho 价值投资+克隆策略；代表作《Dhandho 投资者》"),
                philosophy(
                        "正面我赢，反面我亏得少——寻求不对称的风险收益比",
                        "赚取被市场错误定价的优质企业的价值回归收益",
                        "克隆已被验证的成功投资理念是可行且高效的策略",
                        "耐心持有，直到价值被发现"),
                methodology(
                        "自由现金流强劲、低负债、具备定价权的企业，且折价交易",
                        "不择时——有足够的低估即分批买入",
                        "集中但不极端（通常 10-20 只），保留现金等低估机会",
                        "只投自己理解的简单商业模式，不做预测"),
                trackRecord(
                        "自 1999 年起年化 ~15-20%",
                        "极度务实、善于简化、师从芒格"),
                limitations(
                        "克隆策略在熊市中与模仿对象同跌",
                        "偏好传统行业，错过科技成长红利")
        ));
    }

    private void registerRakeshJhunjhunwala() {
        profiles.put("rakesh_jhunjhunwala", new AgentProfile(
                "rakesh_jhunjhunwala", "拉克什·胡杰", "investor", 13,
                background("印度股市大牛，Rare Enterprises 创始人",
                        "1990 年代起投资印度股市，从 5000 卢比起步创造数十亿美元财富",
                        "核心流派：宏观洞察+成长价值"),
                philosophy(
                        "印度经济增长是最大的做多逻辑——赌国运",
                        "赚取新兴市场高增长企业的复利红利",
                        "盈利动能强劲是第一准则，管理层诚信优秀是第二准则",
                        "对有信心的标的要敢于重仓并长期持有"),
                methodology(
                        "宏观选行业+自下而上选股；聚焦高增长行业中的商业模式可扩展企业",
                        "宏观拐点处重仓买入，不轻易动摇",
                        "敢于重仓高确信度机会；把资金集中在最佳标的上",
                        "认错场，基本面变化立刻退出"),
                trackRecord(
                        "印度最大的个人投资者之一，多只持仓获得百倍以上回报",
                        "极度乐观+宏观视野+敢于重仓"),
                limitations(
                        "高度依赖印度经济持续增长，对印度以外市场经验有限",
                        "过于乐观在系统性危机中容易巨亏")
        ));
    }

    // ---------- 量化/基本面分析师 ----------

    private void registerFundamentals() {
        profiles.put("fundamentals_analyst", new AgentProfile(
                "fundamentals_analyst", "基本面分析师", "analyst", 20,
                background("纪律严明的财务分析师，基于四大财务指标进行量化评分",
                        "不依赖市场情绪和价格走势，只看实实在在的财务数据",
                        "核心方法：ROE/负债/毛利率/净利率 四维评分模型"),
                philosophy(
                        "财务数据是企业最诚实的语言——不会说谎",
                        "赚取企业盈利质量优于市场预期的认知差",
                        "ROE>15%+低负债+高利润率=可投; 任何一项不过关即回避",
                        "价值发现周期，至少持有到下一季财报验证"),
                methodology(
                        "ROE>15%（+1），负债权益比 <0.5（+1），毛利率>40%（+1），净利率>10%（+1）",
                        "不择时——财务数据更新即重新评估",
                        "单维度评分，无仓位建议（由 PortfolioManager 统一管理）",
                        "评分 0-1 bearish，2 neutral，3-4 bullish——简单透明的规则"),
                trackRecord(
                        "在 A 股白马股行情中表现优秀（如 2017-2020 茅台行情）",
                        "极度客观、不情绪化、可解释"),
                limitations(
                        "无法捕捉非财务因素（管理层变动、行业政策、技术变革）",
                        "依赖季度财报频率，在信息真空期信号可能滞后")
        ));
    }

    private void registerGrowth() {
        profiles.put("growth_analyst", new AgentProfile(
                "growth_analyst", "成长分析师", "analyst", 21,
                background("专注业绩增长的硬核分析师，以营收和净利同比增速为唯一指标",
                        "不关心价值高低，不关心市场情绪，只关心公司在变好还是变差",
                        "核心方法：营收增速+净利增速 双维动量评分"),
                philosophy(
                        "成长是股票价值的第一驱动力——其他都是次要的",
                        "赚取企业业绩超预期加速的动量收益",
                        "营收增速>20%+净利增速>20%=好公司; 任何一项负增长即回避",
                        "跟随业绩趋势，业绩反转即调整信号"),
                methodology(
                        "营收同比增速>20%（+1）/ <0%（-1），净利增速>20%（+1）/ <0%（-1）；score>=1 bullish，<= -1 bearish",
                        "财报发布日即更新，跟踪最近数据",
                        "单维度评分，无仓位建议",
                        "简单透明，暴力有效——只抓趋势方向"),
                trackRecord(
                        "在业绩驱动的趋势市中表现突出",
                        "极度专注、简单直接"),
                limitations(
                        "对一次性的非经常性损益过于敏感，容易误判",
                        "在高增长转负增长拐点处容易滞后离场")
        ));
    }

    private void registerSentiment() {
        profiles.put("sentiment_analyst", new AgentProfile(
                "sentiment_analyst", "情绪分析师", "analyst", 22,
                background("内部人行为分析师，以高管/大股东的买卖行为为信号源",
                        "不分析基本面，不关心价格走势，只看内部人用真金白银投票的方向",
                        "核心方法：90 天内部人买卖笔数统计"),
                philosophy(
                        "内部人最了解公司——他们的买卖行为是最诚实的信号",
                        "赚取内部人信息优势的市场滞后定价",
                        "多人买入=看多，多人卖出=看空，混合=方向不明",
                        "90 天窗口——平衡信号稳定性与时效性"),
                methodology(
                        "统计 90 天内内部人买入/卖出总笔数比率；buyRatio>0.7 bullish, <0.3 bearish",
                        "每日更新，跟随内部人动向",
                        "单维度评分，无仓位建议",
                        "当内部人信号与市场走势背离时优先信任内部人信号"),
                trackRecord(
                        "在中国市场内部人增持信号有较好预测力",
                        "简洁、独立、反共识"),
                limitations(
                        "内部人卖出可能因个人财务需求而非看空，有噪音",
                        "中国 A 股内部人交易数据披露有延迟和缺失")
        ));
    }

    private void registerTechnical() {
        profiles.put("technical_analyst", new AgentProfile(
                "technical_analyst", "技术分析师", "analyst", 23,
                background("基于量价数据的规则型技术分析师",
                        "以均线、RSI、52周位置为核心指标，完全量化规则",
                        "核心方法：MA5/MA20 金叉死叉 + RSI 超买超卖 + 52周位置三因子评分"),
                philosophy(
                        "价格包含一切信息——技术分析是市场心理的直接映射",
                        "赚取趋势延续和均值回归的动量收益",
                        "金叉看多、超卖看多、高位看多；死叉看空、超买看空、低位看空",
                        "跟随中期趋势，周度重新评估"),
                methodology(
                        "MA5>MA20 金叉（+1）；RSI<30 超卖（+1）/RSI>70 超买（-1）；52周位置>80%（+1）/<30%（-1）",
                        "实时计算，不依赖日历事件",
                        "单维度评分，无仓位建议",
                        "三因子独立投票，信号清晰无歧义"),
                trackRecord(
                        "趋势市中稳定性高，震荡市也有超买超卖信号",
                        "规则驱动、可回溯、无情绪"),
                limitations(
                        "震荡市中金叉死叉频繁切换，产生大量噪音",
                        "无法识别基本面驱动的结构性牛市或熊市")
        ));
    }

    // ---------- 专项分析师 ----------

    private void registerValuation() {
        profiles.put("valuation_analyst", new AgentProfile(
                "valuation_analyst", "估值分析师", "specialist", 30,
                background("定量估值专家，以多种估值方法交叉验证内在价值",
                        "DCF/EV-EBITDA/PE 三维估值框架，拒绝单一模型偏差",
                        "核心方法：DCF 5年现金流预测 + EV/EBITDA/PE 行业对比"),
                philosophy(
                        "估值是科学不是艺术——多模型交叉验证降低主观偏差",
                        "赚取市场价格向内在价值回归的收敛收益",
                        "高于内在价值 20% = 看空，低于 20% = 看多，范围内 = 合理",
                        "估值修复周期，通常 3-12 个月"),
                methodology(
                        "DCF 预测 5 年自由现金流+终值折现；EV/EBITDA/PE 与行业均值比较",
                        "内在价值更新即重新评估信号",
                        "估值偏差幅度决定信号强度，偏差越大置信度越高",
                        "拒绝单一模型，三模型不一致时降低置信度"),
                trackRecord(
                        "学术严谨，信号可复盘，在价值风格市场中表现突出",
                        "严谨、定量、可验证"),
                limitations(
                        "对快速成长期无利润公司难以合理估值",
                        "DCF 假设敏感性极高——终值增长假设 1% 变动可导致估值偏差 20%+")
        ));
    }

    private void registerNewsSentiment() {
        profiles.put("news_sentiment_analyst", new AgentProfile(
                "news_sentiment_analyst", "新闻情绪分析师", "specialist", 31,
                background("新闻事件驱动的情绪分析专家",
                        "实时跟踪新闻、公告、社交舆情，捕捉市场情绪变化",
                        "核心方法：利多/利空事件分类 + 内部人行为交叉验证"),
                philosophy(
                        "新闻驱动短期价格——市场对信息的反应存在时滞",
                        "利用信息扩散速度差获取超额收益",
                        "利好消息+内部人买入=强看多；利空+内部人卖出=强看空",
                        "以天为单位跟踪，近期事件权重更高"),
                methodology(
                        "利好事件（业绩超预期、新品发布、战略合作）→ 看多；利空（丑闻、诉讼、不及预期）→ 看空",
                        "事件驱动，事件发生即评估",
                        "事件类型+内部人信号交叉验证",
                        "权重随时间衰减——昨天的事件比一周前重要 3 倍"),
                trackRecord(
                        "在事件驱动行情的 T+1/T+2 有较好捕捉力",
                        "高敏感度、实时、善于发现市场忽视的信号"),
                limitations(
                        "噪音多——很多新闻不具备市场影响力但会被误判",
                        "中文 NLP 情感分析准确率约 80%，仍有误分类风险")
        ));
    }

    // =========================================================
    //  画像构建辅助方法
    // =========================================================

    private AgentProfile.Section background(String... points) {
        return new AgentProfile.Section("基础背景", points);
    }

    private AgentProfile.Section philosophy(String... points) {
        return new AgentProfile.Section("核心投资哲学", points);
    }

    private AgentProfile.Section methodology(String... points) {
        return new AgentProfile.Section("实操体系", points);
    }

    private AgentProfile.Section trackRecord(String... points) {
        return new AgentProfile.Section("业绩与特质", points);
    }

    private AgentProfile.Section limitations(String... points) {
        return new AgentProfile.Section("局限性", points);
    }
}

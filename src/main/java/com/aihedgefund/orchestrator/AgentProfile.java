package com.aihedgefund.orchestrator;

/**
 * 分析师结构化画像
 *
 * 参考格雷厄姆画像模板，五位一体描述每位分析师的：
 *   1. 基础背景（身份、流派、代表作）
 *   2. 核心投资哲学（市场认知、赚钱逻辑、核心原则、持仓周期）
 *   3. 实操体系（选股偏好、择时策略、仓位管理、风控纪律）
 *   4. 业绩与特质（历史表现、性格特征、独特标签）
 *   5. 局限性（不擅长领域、适用盲区）
 */
public class AgentProfile {

    private String agentId;
    private String displayName;
    private String category;       // analyst / investor / specialist
    private int sortOrder;         // 排序权重，数字越小越靠前

    // 五位一体画像
    private Section background;
    private Section philosophy;
    private Section methodology;
    private Section trackRecord;
    private Section limitations;

    public AgentProfile() {}

    public AgentProfile(String agentId, String displayName, String category, int sortOrder,
                        Section background, Section philosophy,
                        Section methodology, Section trackRecord, Section limitations) {
        this.agentId = agentId;
        this.displayName = displayName;
        this.category = category;
        this.sortOrder = sortOrder;
        this.background = background;
        this.philosophy = philosophy;
        this.methodology = methodology;
        this.trackRecord = trackRecord;
        this.limitations = limitations;
    }

    // ---- getters / setters ----

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Section getBackground() { return background; }
    public void setBackground(Section background) { this.background = background; }
    public Section getPhilosophy() { return philosophy; }
    public void setPhilosophy(Section philosophy) { this.philosophy = philosophy; }
    public Section getMethodology() { return methodology; }
    public void setMethodology(Section methodology) { this.methodology = methodology; }
    public Section getTrackRecord() { return trackRecord; }
    public void setTrackRecord(Section trackRecord) { this.trackRecord = trackRecord; }
    public Section getLimitations() { return limitations; }
    public void setLimitations(Section limitations) { this.limitations = limitations; }

    /**
     * 画像子模块：标题 + 内容要点列表
     */
    public static class Section {
        private String title;
        private String[] points;

        public Section() {}

        public Section(String title, String... points) {
            this.title = title;
            this.points = points;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String[] getPoints() { return points; }
        public void setPoints(String[] points) { this.points = points; }
    }
}

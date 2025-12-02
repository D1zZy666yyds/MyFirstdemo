class DashboardManager {
    constructor() {
        this.stats = {};
    }

    async loadDashboardData() {
        console.log('加载仪表盘数据...');

        try {
            await this.loadStats();
            await this.loadRecentDocuments();
            await this.loadRecentActivity();
            console.log('仪表盘数据加载完成');
        } catch (error) {
            console.error('加载仪表盘数据失败:', error);
            this.showError('加载仪表盘数据失败: ' + error.message);
        }
    }

    async loadStats() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载统计数据，用户ID:', userId);

            const response = await axios.get('/api/dashboard/stats', {
                params: {
                    userId: userId
                }
            });

            console.log('统计响应:', response.data);

            if (response.data.success) {
                this.stats = response.data.data || {};
                this.updateStatsDisplay();
                console.log('统计数据加载完成:', this.stats);
            } else {
                console.error('加载统计数据失败:', response.data.message);
            }
        } catch (error) {
            console.error('加载统计数据失败:', error);
            throw new Error('获取统计信息失败: ' + error.message);
        }
    }

    updateStatsDisplay() {
        const stats = this.stats;

        // 安全地更新统计卡片
        const updateStat = (elementId, value) => {
            const element = document.getElementById(elementId);
            if (element) {
                element.textContent = value !== undefined && value !== null ? value : 0;
            }
        };

        updateStat('total-documents', stats.totalDocuments);
        updateStat('today-documents', stats.todayDocuments);
        updateStat('total-categories', stats.totalCategories);
        updateStat('total-tags', stats.totalTags);
    }

    async loadRecentDocuments() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载最近文档，用户ID:', userId);

            const response = await axios.get('/api/document/user/' + userId + '/recent', {
                params: { limit: 5 }
            });

            console.log('最近文档响应:', response.data);

            if (response.data.success) {
                this.displayRecentDocuments(response.data.data);
            } else {
                console.error('加载最近文档失败:', response.data.message);
            }
        } catch (error) {
            console.error('加载最近文档失败:', error);
            throw new Error('获取最近文档失败: ' + error.message);
        }
    }

    displayRecentDocuments(documents) {
        const container = document.getElementById('recent-docs-list');
        if (!container) return;

        if (!documents || documents.length === 0) {
            container.innerHTML = '<div class="empty-state">暂无文档</div>';
            return;
        }

        container.innerHTML = documents.map(doc => `
            <div class="recent-doc-item">
                <div class="doc-title">${doc.title || '无标题'}</div>
                <div class="doc-time">${doc.updateTime ? new Date(doc.updateTime).toLocaleDateString() : '未知'}</div>
            </div>
        `).join('');
    }

    async loadRecentActivity() {
        try {
            console.log('加载最近活动');

            const response = await axios.get('/api/operation-logs/recent', {
                params: { limit: 10 }
            });

            console.log('最近活动响应:', response.data);

            if (response.data.success) {
                this.displayRecentActivity(response.data.data);
            } else {
                console.error('加载活动记录失败:', response.data.message);
            }
        } catch (error) {
            console.error('加载活动记录失败:', error);
            throw new Error('获取活动记录失败: ' + error.message);
        }
    }

    displayRecentActivity(activities) {
        const container = document.getElementById('activity-list');
        if (!container) return;

        if (!activities || activities.length === 0) {
            container.innerHTML = '<div class="empty-state">暂无活动记录</div>';
            return;
        }

        container.innerHTML = activities.map(activity => `
            <div class="activity-item">
                <div class="activity-type">${activity.operationType || '未知操作'}</div>
                <div class="activity-desc">${activity.description || '无描述'}</div>
                <div class="activity-time">${activity.operationTime ? new Date(activity.operationTime).toLocaleString() : '未知时间'}</div>
            </div>
        `).join('');
    }

    showError(message) {
        console.error('仪表盘错误:', message);
        alert('错误: ' + message);
    }
}

// 仪表盘管理器实例
const dashboardManager = new DashboardManager();
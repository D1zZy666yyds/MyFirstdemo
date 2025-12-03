// dashboard.js - 修复版本

class DashboardManager {
    constructor() {
        this.stats = {};
        this.recentDocuments = [];
        this.recentActivities = [];
        this.isLoading = false;
        this.lastLoaded = null;
        this.autoRefreshEnabled = false;
        this.refreshInterval = null;
        this.init();
    }

    init() {
        console.log('仪表盘管理器初始化...');
        this.initEventListeners();
        this.checkAutoRefresh();
    }

    initEventListeners() {
        // 刷新按钮
        const refreshBtn = document.getElementById('refresh-dashboard');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.refreshDashboard());
        }

        // 自动刷新开关
        const autoRefreshToggle = document.getElementById('auto-refresh-toggle');
        if (autoRefreshToggle) {
            autoRefreshToggle.addEventListener('change', (e) => {
                this.toggleAutoRefresh(e.target.checked);
                localStorage.setItem('dashboardAutoRefresh', e.target.checked);
            });
        }

        // 导出按钮
        const exportBtn = document.getElementById('export-dashboard');
        if (exportBtn) {
            exportBtn.addEventListener('click', () => this.exportDashboard());
        }
    }

    async loadDashboardData() {
        if (this.isLoading) return;

        console.log('加载仪表盘数据...');
        this.showLoadingState(true);

        try {
            this.isLoading = true;

            // 加载统计数据
            await this.loadStats();

            // 如果有其他可用的API，可以在这里添加
            // await this.loadRecentDocuments();
            // await this.loadRecentActivity();

            this.lastLoaded = new Date();
            this.updateLastLoadedTime();
            console.log('仪表盘数据加载完成');
            this.showSuccessMessage('仪表盘数据已更新');

        } catch (error) {
            console.error('加载仪表盘数据失败:', error);
            this.showError('加载仪表盘数据失败: ' + error.message);
        } finally {
            this.isLoading = false;
            this.showLoadingState(false);
        }
    }

    async loadStats() {
        try {
            // 检查 authManager
            if (typeof authManager === 'undefined') {
                console.error('authManager 未定义');
                throw new Error('认证服务不可用');
            }

            // 获取用户信息，不仅仅是ID
            console.log('authManager 对象:', authManager);
            console.log('当前登录状态:', authManager.isLoggedIn());

            const userId = authManager.getCurrentUserId();
            console.log('获取的用户ID (类型:', typeof userId, '):', userId); // 调试日志

            if (!userId) {
                console.error('用户ID为空，检查登录状态');
                // 尝试其他方式获取用户ID
                const currentUser = authManager.getCurrentUser();
                console.log('当前用户对象:', currentUser);

                if (currentUser && currentUser.id) {
                    userId = currentUser.id;
                    console.log('从用户对象获取的ID:', userId);
                } else {
                    throw new Error('用户未登录或用户ID无效');
                }
            }

            // 确保userId是数字类型
            const numericUserId = Number(userId);
            if (isNaN(numericUserId) || numericUserId <= 0) {
                console.error('无效的用户ID格式:', userId);
                throw new Error('无效的用户ID');
            }

            console.log('加载统计数据，用户ID:', numericUserId);

            // 调用API
            const response = await axios.get('/api/dashboard/stats', {
                params: { userId: numericUserId },
                timeout: 10000
            });

            console.log('API响应状态:', response.status);
            console.log('API响应数据:', response.data);

            // 查看返回的数据结构
            if (response.data) {
                console.log('response.data.success:', response.data.success);
                console.log('response.data.message:', response.data.message);
                console.log('response.data.data:', response.data.data);
            }

            if (response.data && response.data.success) {
                this.stats = response.data.data || {};
                console.log('解析后的统计数据:', this.stats); // 调试日志

                // 应急处理：如果统计数据没有值，检查是否有不同的字段名
                if (this.stats.totalDocuments === undefined) {
                    console.warn('API返回的数据结构可能不同:', this.stats);
                    // 遍历所有键查看实际返回的内容
                    Object.keys(this.stats).forEach(key => {
                        console.log(`统计字段 ${key}:`, this.stats[key]);
                    });
                }

                this.updateStatsDisplay();
                this.updateStatsCards();
                console.log('统计数据加载完成:', this.stats);
            } else {
                const errorMsg = response.data?.message || '加载统计数据失败';
                console.error('API返回失败:', errorMsg);
                this.showError('获取统计信息失败: ' + errorMsg);
            }
        } catch (error) {
            console.error('加载统计数据失败:', error);

            // 更详细的错误信息
            if (error.response) {
                console.error('错误响应状态:', error.response.status);
                console.error('错误响应数据:', error.response.data);
            } else if (error.request) {
                console.error('无响应:', error.request);
            }

            throw error;
        }
    }

    updateStatsDisplay() {
        const stats = this.stats;

        console.log('开始更新统计显示'); // 调试日志
        console.log('stats对象:', stats); // 调试日志
        console.log('totalDocuments:', stats.totalDocuments); // 调试日志
        console.log('todayDocuments:', stats.todayDocuments); // 调试日志
        console.log('totalCategories:', stats.totalCategories); // 调试日志
        console.log('totalTags:', stats.totalTags); // 调试日志

        const updateStat = (elementId, value, suffix = '') => {
            const element = document.getElementById(elementId);
            if (element) {
                // 确保value是有效的数字
                let displayValue;
                if (value === undefined || value === null) {
                    displayValue = 0;
                    console.warn(`${elementId}: 值为undefined或null，使用0`);
                } else {
                    displayValue = Number(value);
                    if (isNaN(displayValue)) {
                        displayValue = 0;
                        console.warn(`${elementId}: 值不是有效数字，使用0`);
                    }
                }

                element.textContent = displayValue + suffix;
                console.log(`更新 ${elementId}: ${displayValue}${suffix}`); // 调试日志

                // 添加动画效果
                element.classList.remove('updated');
                setTimeout(() => {
                    element.classList.add('updated');
                }, 10);
            } else {
                console.error(`元素 ${elementId} 未找到`);
            }
        };

        // 更新主要统计卡片
        // 注意：使用驼峰式属性名，因为后端返回的是驼峰式
        updateStat('total-documents', stats.totalDocuments);
        updateStat('today-documents', stats.todayDocuments);
        updateStat('total-categories', stats.totalCategories);
        updateStat('total-tags', stats.totalTags);

        // 如果有额外统计，也更新
        if (stats.weekDocuments !== undefined) {
            updateStat('week-documents', stats.weekDocuments);
        }
        if (stats.totalFavorites !== undefined) {
            updateStat('total-favorites', stats.totalFavorites);
        }

        console.log('统计显示更新完成'); // 调试日志
    }

    updateStatsCards() {
        const stats = this.stats;
        const cards = document.querySelectorAll('.stat-card');

        cards.forEach(card => {
            const valueElement = card.querySelector('.stat-value');
            const trendElement = card.querySelector('.stat-trend');

            if (valueElement && trendElement) {
                const statId = card.dataset.stat;
                const value = stats[statId] || 0;

                // 如果有昨天数据对比，显示趋势
                const yesterdayKey = `yesterday${statId.charAt(0).toUpperCase() + statId.slice(1)}`;
                if (stats[yesterdayKey] !== undefined) {
                    const yesterdayValue = stats[yesterdayKey] || 0;
                    const trend = value - yesterdayValue;

                    trendElement.textContent = trend >= 0 ? `+${trend}` : trend;
                    trendElement.className = `stat-trend ${trend >= 0 ? 'positive' : 'negative'}`;
                }
            }
        });
    }

    refreshDashboard() {
        console.log('手动刷新仪表盘...');
        this.loadDashboardData();
    }

    toggleAutoRefresh(enabled) {
        this.autoRefreshEnabled = enabled;

        if (enabled) {
            this.refreshInterval = setInterval(() => {
                this.refreshDashboard();
            }, 60000);
            console.log('自动刷新已开启');
            this.showSuccessMessage('自动刷新已开启');
        } else {
            if (this.refreshInterval) {
                clearInterval(this.refreshInterval);
                this.refreshInterval = null;
            }
            console.log('自动刷新已关闭');
            this.showSuccessMessage('自动刷新已关闭');
        }
    }

    checkAutoRefresh() {
        const autoRefreshSetting = localStorage.getItem('dashboardAutoRefresh');
        if (autoRefreshSetting === 'true') {
            const toggle = document.getElementById('auto-refresh-toggle');
            if (toggle) {
                toggle.checked = true;
                this.toggleAutoRefresh(true);
            }
        }
    }

    showLoadingState(show) {
        const loadingOverlay = document.getElementById('dashboard-loading');
        const refreshBtn = document.getElementById('refresh-dashboard');

        if (loadingOverlay) {
            loadingOverlay.style.display = show ? 'flex' : 'none';
        }

        if (refreshBtn) {
            refreshBtn.disabled = show;
            refreshBtn.innerHTML = show ?
                '<span class="loading-spinner-sm"></span> 刷新中...' :
                '🔄 刷新';
        }
    }

    showEmptyState(containerId, message = '暂无数据') {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📊</div>
                    <div class="empty-text">${this.escapeHtml(message)}</div>
                </div>
            `;
        }
    }

    showSuccessMessage(message) {
        const messageContainer = document.getElementById('dashboard-message');
        if (messageContainer) {
            messageContainer.innerHTML = `
                <div class="message success">
                    <span>✅ ${this.escapeHtml(message)}</span>
                </div>
            `;
            setTimeout(() => {
                messageContainer.innerHTML = '';
            }, 3000);
        }
    }

    showError(message) {
        console.error('仪表盘错误:', message);
        const messageContainer = document.getElementById('dashboard-message');
        if (messageContainer) {
            messageContainer.innerHTML = `
                <div class="message error">
                    <span>❌ ${this.escapeHtml(message)}</span>
                    <button onclick="dashboardManager.retryLoad()" class="retry-btn">重试</button>
                </div>
            `;
        }
    }

    retryLoad() {
        console.log('重试加载仪表盘数据...');
        this.loadDashboardData();
    }

    updateLastLoadedTime() {
        const timeElement = document.getElementById('last-loaded-time');
        if (timeElement && this.lastLoaded) {
            timeElement.textContent = `最后更新: ${this.formatTime(this.lastLoaded)}`;
        }
    }

    // 工具方法
    formatTime(date) {
        return date.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }

    escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    exportDashboard() {
        try {
            const dashboardData = {
                stats: this.stats,
                recentDocuments: this.recentDocuments,
                recentActivities: this.recentActivities,
                exportedAt: new Date().toISOString()
            };

            const dataStr = JSON.stringify(dashboardData, null, 2);
            const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
            const exportFileDefaultName = `dashboard-export-${new Date().toISOString().split('T')[0]}.json`;

            const linkElement = document.createElement('a');
            linkElement.setAttribute('href', dataUri);
            linkElement.setAttribute('download', exportFileDefaultName);
            linkElement.click();

            this.showSuccessMessage('仪表盘数据已导出');
        } catch (error) {
            console.error('导出仪表盘失败:', error);
            this.showError('导出失败: ' + error.message);
        }
    }
}

// 页面加载完成后初始化仪表盘
document.addEventListener('DOMContentLoaded', function() {
    console.log('仪表盘页面加载完成');

    // 检查用户是否已登录
    if (typeof authManager !== 'undefined' && authManager.isLoggedIn()) {
        window.dashboardManager = new DashboardManager();
        dashboardManager.loadDashboardData();

        const exportBtn = document.getElementById('export-dashboard');
        if (exportBtn) {
            exportBtn.addEventListener('click', () => dashboardManager.exportDashboard());
        }
    } else {
        console.error('用户未登录，重定向到登录页');
        window.location.href = 'login.html';
    }
});
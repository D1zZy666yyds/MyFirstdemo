// dashboard.js - 简化版本，只显示核心功能
class DashboardManager {
    constructor() {
        this.stats = {
            totalDocuments: 0,
            todayDocuments: 0,
            totalCategories: 0,
            totalTags: 0
        };
        this.recentDocuments = [];
        this.recentActivities = [];
        this.isLoading = false;
        this.init();
    }

    init() {
        console.log('仪表盘管理器初始化...');
        this.initEventListeners();
        this.loadDashboardData();
    }

    initEventListeners() {
        // 导航到仪表盘时自动刷新
        const dashboardLink = document.querySelector('a[href="#dashboard"]');
        if (dashboardLink) {
            dashboardLink.addEventListener('click', () => {
                this.loadDashboardData();
            });
        }
    }

    // 在 loadDashboardData 方法中，修改加载状态处理
    async loadDashboardData() {
        if (this.isLoading) return;

        console.log('加载仪表盘数据...');
        this.isLoading = true;

        // 只在仪表盘工具栏显示加载状态
        const refreshBtn = document.getElementById('refresh-dashboard');
        if (refreshBtn) {
            refreshBtn.disabled = true;
            refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 加载中...';
        }

        try {
            // 修复：使用 isAuthenticated() 而不是 isLoggedIn()
            if (typeof authManager === 'undefined' || !authManager.isAuthenticated()) {
                console.error('用户未登录或认证管理器未初始化');
                this.showError('用户未登录，请重新登录');

                // 延迟重定向，让用户看到错误消息
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }

            const userId = authManager.getCurrentUserId();
            if (!userId) {
                throw new Error('用户未登录或用户ID无效');
            }

            // 并行加载所有数据
            await Promise.all([
                this.loadStats(userId),
                this.loadRecentDocuments(userId),
                this.loadRecentActivities(userId)
            ]);

            console.log('仪表盘数据加载完成');

        } catch (error) {
            console.error('加载仪表盘数据失败:', error);
            this.showError('加载数据失败: ' + error.message);
        } finally {
            this.isLoading = false;

            // 恢复刷新按钮状态
            if (refreshBtn) {
                refreshBtn.disabled = false;
                refreshBtn.innerHTML = '<i class="fas fa-sync-alt"></i> 刷新';
            }
        }
    }

    // 修改 showEmptyRecentDocuments 和 showEmptyRecentActivities 方法
    showEmptyRecentDocuments() {
        const container = document.getElementById('recent-docs-list');
        if (container) {
            container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📄</div>
                <div class="empty-text">暂无最近文档</div>
            </div>
        `;
        }
    }

    showEmptyRecentActivities() {
        const container = document.getElementById('activity-list');
        if (container) {
            container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📝</div>
                <div class="empty-text">暂无最近活动</div>
            </div>
        `;
        }
    }

    async loadStats(userId) {
        try {
            console.log('加载统计数据，用户ID:', userId);

            const response = await axios.get('/api/dashboard/stats', {
                params: { userId: userId },
                timeout: 10000
            });

            console.log('统计数据响应:', response.data);

            if (response.data && response.data.success) {
                this.stats = response.data.data || {};

                // 确保有默认值
                this.stats = {
                    totalDocuments: this.stats.totalDocuments || 0,
                    todayDocuments: this.stats.todayDocuments || 0,
                    totalCategories: this.stats.totalCategories || 0,
                    totalTags: this.stats.totalTags || 0
                };

                this.updateStatsDisplay();
                console.log('统计数据已更新:', this.stats);
            } else {
                throw new Error(response.data?.message || '加载统计数据失败');
            }
        } catch (error) {
            console.error('加载统计数据失败:', error);
            throw error;
        }
    }

    async loadRecentDocuments(userId) {
        try {
            console.log('加载最近文档，用户ID:', userId);

            // 如果没有专门的API，可以从文档列表获取
            const response = await axios.get('/api/documents/recent', {
                params: {
                    userId: userId,
                    limit: 10
                },
                timeout: 10000
            });

            if (response.data && response.data.success) {
                this.recentDocuments = response.data.data || [];
                this.updateRecentDocumentsDisplay();
                console.log('最近文档已更新:', this.recentDocuments.length);
            } else {
                // 如果API不存在，显示空状态
                this.showEmptyRecentDocuments();
            }
        } catch (error) {
            console.warn('加载最近文档失败，显示空状态:', error.message);
            this.showEmptyRecentDocuments();
        }
    }

    async loadRecentActivities(userId) {
        try {
            console.log('加载最近活动，用户ID:', userId);

            const response = await axios.get('/api/operation-logs/recent', {
                params: {
                    userId: userId,
                    limit: 10
                },
                timeout: 10000
            });

            if (response.data && response.data.success) {
                this.recentActivities = response.data.data || [];
                this.updateRecentActivitiesDisplay();
                console.log('最近活动已更新:', this.recentActivities.length);
            } else {
                // 如果API不存在，显示空状态
                this.showEmptyRecentActivities();
            }
        } catch (error) {
            console.warn('加载最近活动失败，显示空状态:', error.message);
            this.showEmptyRecentActivities();
        }
    }

    updateStatsDisplay() {
        console.log('更新统计显示:', this.stats);

        // 更新统计数字
        this.updateElementText('total-documents', this.stats.totalDocuments || 0);
        this.updateElementText('today-documents', this.stats.todayDocuments || 0);
        this.updateElementText('total-categories', this.stats.totalCategories || 0);
        this.updateElementText('total-tags', this.stats.totalTags || 0);

        // 添加动画效果
        this.animateStats();
    }

    updateElementText(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            // 如果是数字，确保是整数
            if (typeof value === 'number') {
                element.textContent = Math.round(value);
            } else {
                element.textContent = value || '0';
            }
        } else {
            console.warn(`元素 ${elementId} 未找到`);
        }
    }

    animateStats() {
        // 为统计数字添加简单的动画
        const statNumbers = document.querySelectorAll('.stat-number');
        statNumbers.forEach((element, index) => {
            element.style.opacity = '0.7';
            setTimeout(() => {
                element.style.transition = 'opacity 0.3s ease';
                element.style.opacity = '1';
            }, index * 100);
        });
    }

    updateRecentDocumentsDisplay() {
        const container = document.getElementById('recent-docs-list');
        if (!container) return;

        if (this.recentDocuments.length === 0) {
            this.showEmptyRecentDocuments();
            return;
        }

        let html = '<ul class="doc-list-items">';
        this.recentDocuments.forEach(doc => {
            html += `
                <li class="doc-list-item">
                    <div class="doc-info">
                        <div class="doc-title">${this.escapeHtml(doc.title || '无标题')}</div>
                        <div class="doc-meta">
                            <span class="doc-time">${this.formatDate(doc.createdTime || doc.created_time)}</span>
                            ${doc.categoryName ? `<span class="doc-category">${this.escapeHtml(doc.categoryName)}</span>` : ''}
                        </div>
                    </div>
                    <button onclick="window.documentManager.viewDocument(${doc.id})" class="btn-small">查看</button>
                </li>
            `;
        });
        html += '</ul>';

        container.innerHTML = html;
    }

    updateRecentActivitiesDisplay() {
        const container = document.getElementById('activity-list');
        if (!container) return;

        if (this.recentActivities.length === 0) {
            this.showEmptyRecentActivities();
            return;
        }

        let html = '<ul class="activity-items">';
        this.recentActivities.forEach(activity => {
            html += `
                <li class="activity-item">
                    <div class="activity-icon">${this.getActivityIcon(activity.operationType)}</div>
                    <div class="activity-content">
                        <div class="activity-text">${this.escapeHtml(activity.description || '未知操作')}</div>
                        <div class="activity-time">${this.formatDateTime(activity.createdTime || activity.created_time)}</div>
                    </div>
                </li>
            `;
        });
        html += '</ul>';

        container.innerHTML = html;
    }

    showEmptyRecentDocuments() {
        const container = document.getElementById('recent-docs-list');
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📄</div>
                    <div class="empty-text">暂无最近文档</div>
                </div>
            `;
        }
    }

    showEmptyRecentActivities() {
        const container = document.getElementById('activity-list');
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📝</div>
                    <div class="empty-text">暂无最近活动</div>
                </div>
            `;
        }
    }

    showLoadingState(show) {
        const dashboardPage = document.getElementById('dashboard-page');
        if (dashboardPage) {
            if (show) {
                dashboardPage.classList.add('loading');
            } else {
                dashboardPage.classList.remove('loading');
            }
        }
    }

    showError(message) {
        console.error('仪表盘错误:', message);

        // 可以在顶部显示错误消息
        const errorDiv = document.createElement('div');
        errorDiv.className = 'global-error';
        errorDiv.innerHTML = `
            <div class="error-content">
                <span class="error-icon">❌</span>
                <span class="error-text">${this.escapeHtml(message)}</span>
                <button onclick="this.parentElement.parentElement.remove()" class="error-close">×</button>
            </div>
        `;

        // 插入到页面顶部
        const app = document.getElementById('app');
        if (app && app.firstChild) {
            app.insertBefore(errorDiv, app.firstChild);

            // 5秒后自动移除
            setTimeout(() => {
                if (errorDiv.parentNode) {
                    errorDiv.remove();
                }
            }, 5000);
        }
    }

    updateLastLoadedTime() {
        const now = new Date();
        console.log('最后更新时间:', now.toLocaleString());
        // 可以添加一个显示最后更新时间的小元素
    }

    // 工具方法
    getActivityIcon(operationType) {
        const icons = {
            'CREATE': '📝',
            'UPDATE': '✏️',
            'DELETE': '🗑️',
            'LOGIN': '🔐',
            'LOGOUT': '🚪',
            'VIEW': '👁️',
            'SHARE': '🔗',
            'FAVORITE': '❤️'
        };
        return icons[operationType] || '📌';
    }

    formatDate(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        const now = new Date();
        const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));

        if (diffDays === 0) {
            return '今天';
        } else if (diffDays === 1) {
            return '昨天';
        } else if (diffDays < 7) {
            return `${diffDays}天前`;
        } else {
            return date.toLocaleDateString('zh-CN');
        }
    }

    formatDateTime(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleString('zh-CN', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    // 刷新方法（公开）
    refresh() {
        console.log('手动刷新仪表盘...');
        this.loadDashboardData();
    }
}

// 页面加载完成后初始化仪表盘
document.addEventListener('DOMContentLoaded', function() {
    console.log('仪表盘页面加载完成');

    // 修复：使用 isAuthenticated() 而不是 isLoggedIn()
    // 添加延迟确保 authManager 已完全初始化
    setTimeout(() => {
        if (typeof authManager !== 'undefined' && authManager.isAuthenticated && authManager.isAuthenticated()) {
            console.log('用户已认证，初始化仪表盘管理器');
            window.dashboardManager = new DashboardManager();

            // 监听页面切换
            const navItems = document.querySelectorAll('.nav-item');
            navItems.forEach(item => {
                item.addEventListener('click', function(e) {
                    if (this.getAttribute('href') === '#dashboard') {
                        // 切换到仪表盘时刷新数据
                        setTimeout(() => {
                            if (window.dashboardManager) {
                                window.dashboardManager.refresh();
                            }
                        }, 100);
                    }
                });
            });
        } else {
            console.error('用户未登录或认证管理器未正确初始化');

            // 检查当前页面，如果不是登录页则重定向
            const currentPage = window.location.pathname.split('/').pop();
            if (currentPage !== 'login.html') {
                console.log('用户未登录，准备重定向到登录页');
                // 延迟重定向，以便显示错误消息
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 1500);
            }
        }
    }, 300); // 延迟300ms确保所有脚本加载完成
});
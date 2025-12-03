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

    async loadStats(userId) {
        try {
            console.log('加载统计数据，用户ID:', userId);

            // 使用现有的API获取数据并计算统计
            const [documentsRes, categoriesRes, tagsRes] = await Promise.all([
                axios.get(`/api/document/user/${userId}`),  // 获取用户所有文档
                axios.get(`/api/category/user/${userId}`),  // 获取用户所有分类
                axios.get(`/api/tag/user/${userId}`)        // 获取用户所有标签
            ]);

            console.log('统计数据API响应:', {
                documents: documentsRes.data,
                categories: categoriesRes.data,
                tags: tagsRes.data
            });

            // 计算统计数据
            const documents = documentsRes.data.data || [];
            const categories = categoriesRes.data.data || [];
            const tags = tagsRes.data.data || [];

            // 计算今日新增文档
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const todayDocuments = documents.filter(doc => {
                const docDate = new Date(doc.createdTime || doc.created_time);
                docDate.setHours(0, 0, 0, 0);
                return docDate.getTime() === today.getTime();
            }).length;

            // 更新统计对象
            this.stats = {
                totalDocuments: documents.length,
                todayDocuments: todayDocuments,
                totalCategories: categories.length,
                totalTags: tags.length
            };

            this.updateStatsDisplay();
            console.log('统计数据已更新:', this.stats);

        } catch (error) {
            console.error('加载统计数据失败:', error);

            // 如果API不存在，使用备用方案
            this.loadStatsFallback();
        }
    }

    // 备用方案：使用模拟数据
    loadStatsFallback() {
        console.log('使用备用方案加载统计数据');

        // 这里可以使用localStorage或默认值
        this.stats = {
            totalDocuments: 0,
            todayDocuments: 0,
            totalCategories: 0,
            totalTags: 0
        };

        this.updateStatsDisplay();
    }

    async loadRecentDocuments(userId) {
        try {
            console.log('加载最近文档，用户ID:', userId);

            // 使用正确的API端点
            const response = await axios.get(`/api/document/user/${userId}/recent`, {
                params: {
                    limit: 10
                },
                timeout: 10000
            });

            console.log('最近文档响应:', response.data);

            if (response.data && response.data.success) {
                this.recentDocuments = response.data.data || [];

                // 确保按时间倒序排列（最新的在最前面）
                this.recentDocuments.sort((a, b) => {
                    const timeA = new Date(a.createdTime || a.created_time || a.updatedTime || a.updated_time).getTime();
                    const timeB = new Date(b.createdTime || b.created_time || b.updatedTime || b.updated_time).getTime();
                    return timeB - timeA; // 最新的在前面
                });

                this.updateRecentDocumentsDisplay();
                console.log('最近文档已更新:', this.recentDocuments.length);
            } else {
                console.warn('最近文档API返回格式不匹配:', response.data);
                // 备用方案：从所有文档中获取最近10个
                await this.loadRecentDocumentsFallback(userId);
            }
        } catch (error) {
            console.warn('加载最近文档失败:', error.message);

            // 备用方案：从所有文档中获取最近10个
            await this.loadRecentDocumentsFallback(userId);
        }
    }

    // 备用方案：从用户所有文档中获取最近文档
    async loadRecentDocumentsFallback(userId) {
        try {
            const response = await axios.get(`/api/document/user/${userId}`);
            if (response.data && response.data.success) {
                const allDocuments = response.data.data || [];
                // 按创建时间倒序排序，取最近10个（最新的在前面）
                this.recentDocuments = allDocuments
                    .filter(doc => !doc.deleted) // 排除已删除的文档
                    .sort((a, b) => {
                        const timeA = new Date(a.createdTime || a.created_time || a.updatedTime || a.updated_time).getTime();
                        const timeB = new Date(b.createdTime || b.created_time || b.updatedTime || b.updated_time).getTime();
                        return timeB - timeA; // 最新的在前面
                    })
                    .slice(0, 10);

                this.updateRecentDocumentsDisplay();
            } else {
                this.showEmptyRecentDocuments();
            }
        } catch (error) {
            console.warn('备用方案也失败:', error.message);
            this.showEmptyRecentDocuments();
        }
    }

    async loadRecentActivities(userId) {
        try {
            console.log('加载最近活动，用户ID:', userId);

            // 使用正确的API端点，只获取当前用户的操作日志
            const response = await axios.get(`/api/operation-logs/user/${userId}`, {
                timeout: 10000
            });

            if (response.data && response.data.success) {
                let activities = response.data.data || [];

                // 筛选只显示登录登出活动（支持多种操作类型）
                this.recentActivities = activities
                    .filter(activity => {
                        const opType = activity.operationType || '';
                        // 匹配登录相关的操作类型
                        return opType === 'USER_LOGIN' ||
                            opType === 'USER_LOGOUT' ||
                            opType === 'LOGIN' ||
                            opType === 'LOGOUT' ||
                            opType === 'USER_REGISTER' ||
                            opType.includes('LOGIN') ||
                            opType.includes('LOGOUT');
                    })
                    .sort((a, b) => {
                        const timeA = new Date(a.createdTime || a.created_time).getTime();
                        const timeB = new Date(b.createdTime || b.created_time).getTime();
                        return timeB - timeA; // 最新的在前面
                    })
                    .slice(0, 10); // 只显示最近10条

                this.updateRecentActivitiesDisplay();
                console.log('最近活动已更新:', this.recentActivities.length);
            } else {
                console.warn('用户活动API返回格式不匹配');
                this.showEmptyRecentActivities();
            }
        } catch (error) {
            console.warn('加载用户活动失败:', error.message);
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
            // 确保文档对象有必要的字段
            const title = doc.title || '无标题文档';
            const createdTime = doc.createdTime || doc.created_time || doc.updatedTime || doc.updated_time;
            const categoryName = doc.categoryName || doc.category?.name || '';

            html += `
                <li class="doc-list-item">
                    <div class="doc-info">
                        <div class="doc-title">${this.escapeHtml(title)}</div>
                        <div class="doc-meta">
                            <span class="doc-time">${this.formatDate(createdTime)}</span>
                            ${categoryName ? `<span class="doc-category">${this.escapeHtml(categoryName)}</span>` : ''}
                        </div>
                    </div>
                    <button onclick="if(window.documentManager) { window.documentManager.viewDocument(${doc.id}) } else { alert('文档管理器未初始化') }" 
                            class="btn-small">查看</button>
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
            // 确保活动对象有必要的字段
            const description = activity.description || activity.operationType || '未知操作';
            const operationType = activity.operationType || 'UNKNOWN';
            const createdTime = activity.createdTime || activity.created_time;

            html += `
                <li class="activity-item">
                    <div class="activity-icon">${this.getActivityIcon(operationType)}</div>
                    <div class="activity-content">
                        <div class="activity-text">${this.escapeHtml(description)}</div>
                        <div class="activity-meta">
                            <span class="activity-type">${this.getOperationTypeText(operationType)}</span>
                            <span class="activity-time">${this.formatDateTime(createdTime)}</span>
                        </div>
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
                    <div class="empty-hint">点击"新建文档"开始创建</div>
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
                    <div class="empty-hint">您的操作记录将显示在这里</div>
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
            'USER_LOGIN': '🔐',
            'USER_LOGOUT': '🚪',
            'LOGIN': '🔐',
            'LOGOUT': '🚪',
            'USER_REGISTER': '📝',
            'CREATE': '📝',
            'UPDATE': '✏️',
            'DELETE': '🗑️',
            'VIEW': '👁️',
            'SHARE': '🔗',
            'FAVORITE': '❤️'
        };
        return icons[operationType] || '📌';
    }

    // 操作类型文本映射
    getOperationTypeText(operationType) {
        const typeMap = {
            'USER_LOGIN': '登录',
            'USER_LOGOUT': '登出',
            'LOGIN': '登录',
            'LOGOUT': '登出',
            'USER_REGISTER': '注册',
            'CREATE': '创建',
            'UPDATE': '更新',
            'DELETE': '删除',
            'VIEW': '查看',
            'SHARE': '分享',
            'FAVORITE': '收藏'
        };
        return typeMap[operationType] || operationType;
    }

    formatDate(dateString) {
        if (!dateString) return '未知时间';

        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) {
                return '未知时间';
            }

            const now = new Date();
            const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));

            if (diffDays === 0) {
                // 今天，显示具体时间
                return date.toLocaleString('zh-CN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                });
            } else if (diffDays === 1) {
                // 昨天，显示具体时间
                return `昨天 ${date.toLocaleString('zh-CN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                })}`;
            } else if (diffDays < 7) {
                // 一周内，显示星期几和具体时间
                const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
                const weekday = weekdays[date.getDay()];
                return `${weekday} ${date.toLocaleString('zh-CN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                })}`;
            } else {
                // 一周以上，显示完整日期和时间
                return date.toLocaleString('zh-CN', {
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                }).replace(/\//g, '-');
            }
        } catch (error) {
            return '未知时间';
        }
    }

    formatDateTime(dateString) {
        if (!dateString) return '未知时间';

        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) {
                return '未知时间';
            }

            // 显示具体的日期和时间（年月日 时:分）
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            }).replace(/\//g, '-');

            // 或者使用更简洁的格式：MM-DD HH:mm
            // return date.toLocaleString('zh-CN', {
            //     month: '2-digit',
            //     day: '2-digit',
            //     hour: '2-digit',
            //     minute: '2-digit',
            //     hour12: false
            // }).replace(/\//g, '-');

        } catch (error) {
            console.warn('格式化时间失败:', error);
            return '未知时间';
        }
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
// favorite-manager.js - 完整修复版

class FavoriteManager {
    constructor() {
        this.favoriteCache = new Map(); // 缓存收藏状态: {docId: {isFavorite: boolean}}
        this.isInitialized = false;
        this.userId = null;
        this.favoritesCount = 0;
    }

    async initialize() {
        if (this.isInitialized) return;

        try {
            // 获取当前用户ID
            this.userId = await this.getCurrentUserId();
            if (!this.userId) {
                console.warn('用户未登录，收藏功能暂不可用');
                return;
            }

            console.log('收藏管理器初始化，用户ID:', this.userId);

            this.bindEvents();
            this.isInitialized = true;

            // 如果当前在收藏页面，直接加载数据
            if (window.location.hash === '#favorites') {
                await this.loadFavorites();
            }

        } catch (error) {
            console.error('收藏管理器初始化失败:', error);
        }
    }

    bindEvents() {
        // 监听页面切换事件
        document.addEventListener('click', (e) => {
            const favoritesLink = e.target.closest('a[href="#favorites"]');
            if (favoritesLink) {
                e.preventDefault();
                this.showFavoritesPage();
            }
        });

        // 监听刷新按钮
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-refresh-favorites')) {
                this.loadFavorites();
            }
        });

        // 监听文档列表加载事件
        document.addEventListener('documentListLoaded', (e) => {
            if (e.detail && e.detail.documents) {
                this.addFavoriteButtonsToList(e.detail.documents);
            }
        });

        // 监听文档加载事件
        document.addEventListener('documentLoaded', (e) => {
            if (e.detail && e.detail.document) {
                this.addFavoriteButtonToDocument(e.detail.document);
            }
        });
    }

    async getCurrentUserId() {
        try {
            // 优先从authManager获取
            if (typeof authManager !== 'undefined' && authManager.getCurrentUserId) {
                return authManager.getCurrentUserId();
            }

            // 从localStorage获取
            const userId = localStorage.getItem('userId');
            if (userId) {
                return parseInt(userId);
            }

            return null;
        } catch (error) {
            console.error('获取用户ID失败:', error);
            return null;
        }
    }

    // 显示收藏页面
    async showFavoritesPage() {
        try {
            // 隐藏所有页面
            this.hideAllPages();

            // 显示收藏页面
            const page = document.getElementById('favorites-page');
            if (page) {
                page.classList.add('active');
                this.updateActiveNav('#favorites');
                await this.loadFavorites();
            }
        } catch (error) {
            console.error('显示收藏页面失败:', error);
            this.showNotification('打开收藏页面失败: ' + error.message, 'error');
        }
    }

    hideAllPages() {
        document.querySelectorAll('.page').forEach(page => {
            page.classList.remove('active');
        });
    }

    updateActiveNav(hash) {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });

        const navItem = document.querySelector(`a[href="${hash}"]`);
        if (navItem) {
            navItem.closest('.nav-item')?.classList.add('active');
        }
    }

    // 加载收藏列表
    async loadFavorites() {
        try {
            const container = document.getElementById('favorites-content');
            if (!container) {
                console.error('收藏内容容器未找到');
                return;
            }

            // 显示加载状态
            container.innerHTML = `
                <div class="loading-state">
                    <div class="spinner"></div>
                    <p>正在加载收藏文档...</p>
                </div>
            `;

            const userId = this.userId;
            if (!userId) {
                this.showNotification('请先登录', 'error');
                container.innerHTML = `
                    <div class="favorites-empty">
                        <div class="empty-icon">🔒</div>
                        <h3>请先登录</h3>
                        <p>登录后才能查看收藏</p>
                    </div>
                `;
                return;
            }

            console.log('正在加载收藏，用户ID:', userId);

            // 调用收藏API - 根据你的后端代码
            const response = await axios.get(`/api/favorite/user/${userId}`);

            console.log('收藏API响应:', response.data);

            if (response.data && response.data.success) {
                const favorites = response.data.data || [];
                this.favoritesCount = favorites.length;

                if (favorites.length === 0) {
                    container.innerHTML = `
                        <div class="favorites-empty">
                            <div class="empty-icon">❤️</div>
                            <h3>暂无收藏文档</h3>
                            <p>快去收藏你喜欢的文档吧</p>
                        </div>
                    `;
                    this.updateFavoritesBadge(0);
                    return;
                }

                let html = '<div class="favorites-grid">';

                favorites.forEach(doc => {
                    const categoryName = doc.categoryName || this.getCategoryName(doc.categoryId) || '未分类';

                    html += `
                        <div class="favorite-doc-card" data-document-id="${doc.id}">
                            <div class="doc-header">
                                <h4 class="doc-title" style="cursor: pointer;">
                                    ${this.escapeHtml(doc.title || '无标题')}
                                </h4>
                                <button class="favorite-button favorited" data-doc-id="${doc.id}" title="取消收藏">❤️</button>
                            </div>
                            <div class="doc-meta">
                                <span>📁 ${categoryName}</span>
                                <span>📅 ${this.formatTime(doc.createdTime)}</span>
                            </div>
                            ${this.renderDocumentTags(doc.tags)}
                            <div class="doc-preview" style="cursor: pointer;">
                                ${this.truncateText(doc.content || '', 100)}
                            </div>
                            <div class="doc-actions">
                                <button class="btn-small btn-view" data-doc-id="${doc.id}">查看</button>
                                <button class="btn-small btn-edit" data-doc-id="${doc.id}">编辑</button>
                                <button class="btn-small btn-danger btn-delete" data-doc-id="${doc.id}">删除</button>
                            </div>
                        </div>
                    `;
                });

                html += '</div>';
                container.innerHTML = html;

                // 绑定事件
                this.bindFavoritesCardEvents();

                // 更新徽章
                this.updateFavoritesBadge(favorites.length);

            } else {
                throw new Error(response.data?.message || '加载收藏失败');
            }

        } catch (error) {
            console.error('加载收藏列表失败:', error);

            const container = document.getElementById('favorites-content');
            if (container) {
                container.innerHTML = `
                    <div class="error-state">
                        <div class="error-icon">❌</div>
                        <h3>加载失败</h3>
                        <p>${this.getErrorMessage(error)}</p>
                        <button class="btn-secondary btn-retry" style="margin-top: 15px;">重试</button>
                    </div>
                `;

                // 绑定重试按钮事件
                container.querySelector('.btn-retry')?.addEventListener('click', () => {
                    this.loadFavorites();
                });
            }
        }
    }

    // 绑定收藏卡片事件
    bindFavoritesCardEvents() {
        // 绑定查看按钮
        document.querySelectorAll('.btn-view').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const docId = parseInt(e.target.dataset.docId);
                if (docId && typeof documentManager !== 'undefined') {
                    documentManager.viewDocument(docId);
                }
            });
        });

        // 绑定编辑按钮
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const docId = parseInt(e.target.dataset.docId);
                if (docId && typeof documentManager !== 'undefined') {
                    documentManager.editDocument(docId);
                }
            });
        });

        // 绑定删除按钮
        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const docId = parseInt(e.target.dataset.docId);
                if (docId && typeof documentManager !== 'undefined') {
                    documentManager.deleteDocument(docId);
                }
            });
        });

        // 绑定收藏按钮
        document.querySelectorAll('.favorite-doc-card .favorite-button').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const docId = parseInt(e.target.dataset.docId);
                await this.toggleFavorite(docId, e.target);

                // 重新加载收藏列表
                setTimeout(() => this.loadFavorites(), 300);
            });
        });

        // 绑定标题和预览点击事件
        document.querySelectorAll('.favorite-doc-card .doc-title, .favorite-doc-card .doc-preview').forEach(element => {
            element.addEventListener('click', (e) => {
                const card = e.target.closest('.favorite-doc-card');
                if (card) {
                    const docId = parseInt(card.dataset.documentId);
                    if (docId && typeof documentManager !== 'undefined') {
                        documentManager.viewDocument(docId);
                    }
                }
            });
        });
    }

    // 为文档列表添加收藏按钮
    async addFavoriteButtonsToList(documents) {
        if (!Array.isArray(documents)) return;

        // 批量获取收藏状态
        const favoriteStatuses = await this.batchCheckFavoriteStatus(documents.map(d => d.id));

        documents.forEach((doc, index) => {
            const docId = doc.id;
            const isFavorite = favoriteStatuses[index];

            // 在文档卡片中查找并添加收藏按钮
            const docElement = document.querySelector(`[data-document-id="${docId}"]`);
            if (docElement) {
                // 移除现有的收藏按钮
                const existingFavoriteBtn = docElement.querySelector('.doc-favorite-button');
                if (existingFavoriteBtn) {
                    existingFavoriteBtn.remove();
                }

                // 创建新的收藏按钮
                const favoriteButton = this.createFavoriteButton(docId, isFavorite);
                favoriteButton.className = 'doc-favorite-button';

                // 添加到文档卡片的标题区域
                const titleElement = docElement.querySelector('.doc-title');
                if (titleElement) {
                    titleElement.style.position = 'relative';
                    titleElement.style.paddingRight = '40px';
                    favoriteButton.style.position = 'absolute';
                    favoriteButton.style.top = '0';
                    favoriteButton.style.right = '0';
                    titleElement.appendChild(favoriteButton);
                }
            }
        });
    }

    // 为单个文档添加收藏按钮
    async addFavoriteButtonToDocument(docOrEvent) {
        // 处理两种可能的参数：文档对象或事件对象
        let doc;

        if (docOrEvent && docOrEvent.id) {
            // 如果直接传递文档对象
            doc = docOrEvent;
        } else if (docOrEvent && docOrEvent.detail && docOrEvent.detail.document) {
            // 如果传递的是CustomEvent对象
            doc = docOrEvent.detail.document;
        } else {
            console.error('addFavoriteButtonToDocument: 无效的参数类型', docOrEvent);
            return;
        }

        const docId = doc.id;

        // 获取收藏状态
        const isFavorite = await this.checkFavoriteStatus(docId);

        // 创建收藏按钮
        const favoriteButton = this.createFavoriteButton(docId, isFavorite);
        favoriteButton.className = 'modal-favorite-button';

        // 在文档查看模态框中查找标题区域
        const modal = document.querySelector('.modal');
        if (modal) {
            const header = modal.querySelector('.modal-header');
            if (header) {
                // 移除现有的收藏按钮
                const existingBtn = header.querySelector('.modal-favorite-button');
                if (existingBtn) {
                    existingBtn.remove();
                }

                header.style.position = 'relative';
                favoriteButton.style.position = 'absolute';
                favoriteButton.style.top = '10px';
                favoriteButton.style.right = '40px';
                favoriteButton.style.zIndex = '1000';
                header.appendChild(favoriteButton);
            }
        }
    }

    // 创建收藏按钮
    createFavoriteButton(docId, isFavorite) {
        const button = document.createElement('button');
        button.className = `favorite-button ${isFavorite ? 'favorited' : ''}`;
        button.setAttribute('data-doc-id', docId);
        button.setAttribute('title', isFavorite ? '取消收藏' : '收藏');
        button.innerHTML = isFavorite ? '❤️' : '🤍';

        button.addEventListener('click', async (e) => {
            e.stopPropagation();
            await this.toggleFavorite(docId, button);
        });

        return button;
    }

    // 切换收藏状态
    async toggleFavorite(docId, button) {
        try {
            const userId = this.userId;
            if (!userId) {
                this.showNotification('请先登录', 'warning');
                return;
            }

            const cached = this.favoriteCache.get(docId);
            const currentStatus = cached ? cached.isFavorite : false;
            const newStatus = !currentStatus;

            if (newStatus) {
                // 添加收藏
                const response = await axios.post(`/api/favorite/document/${docId}?userId=${userId}`);
                if (response.data && response.data.success) {
                    button.innerHTML = '❤️';
                    button.className = 'favorite-button favorited';
                    button.title = '取消收藏';
                    this.favoriteCache.set(docId, { isFavorite: true });
                    this.showNotification('收藏成功', 'success');
                }
            } else {
                // 取消收藏
                const response = await axios.delete(`/api/favorite/document/${docId}?userId=${userId}`);
                if (response.data && response.data.success) {
                    button.innerHTML = '🤍';
                    button.className = 'favorite-button';
                    button.title = '收藏';
                    this.favoriteCache.set(docId, { isFavorite: false });
                    this.showNotification('已取消收藏', 'info');
                }
            }

        } catch (error) {
            console.error('切换收藏状态失败:', error);
            this.showNotification('操作失败: ' + this.getErrorMessage(error), 'error');
        }
    }

    // 检查单个文档的收藏状态
    async checkFavoriteStatus(docId) {
        try {
            const userId = this.userId;
            if (!userId) return false;

            // 检查缓存
            const cached = this.favoriteCache.get(docId);
            if (cached !== undefined) {
                return cached.isFavorite;
            }

            // 查询服务器
            const response = await axios.get(`/api/favorite/document/${docId}?userId=${userId}`);
            if (response.data && response.data.success) {
                const isFavorite = response.data.data;
                this.favoriteCache.set(docId, { isFavorite });
                return isFavorite;
            }
            return false;
        } catch (error) {
            console.error('获取收藏状态失败:', error);
            return false;
        }
    }

    // 批量检查收藏状态
    async batchCheckFavoriteStatus(docIds) {
        try {
            const userId = this.userId;
            if (!userId) return docIds.map(() => false);

            const results = [];

            for (const docId of docIds) {
                // 检查缓存
                const cached = this.favoriteCache.get(docId);
                if (cached !== undefined) {
                    results.push(cached.isFavorite);
                    continue;
                }

                try {
                    const response = await axios.get(`/api/favorite/document/${docId}?userId=${userId}`);
                    const isFavorite = response.data && response.data.success ? response.data.data : false;
                    this.favoriteCache.set(docId, { isFavorite });
                    results.push(isFavorite);
                } catch {
                    results.push(false);
                }
            }

            return results;
        } catch (error) {
            console.error('批量获取收藏状态失败:', error);
            return docIds.map(() => false);
        }
    }

    // 渲染文档标签
    renderDocumentTags(tags) {
        if (!tags || tags.length === 0) {
            return '';
        }

        const tagsHtml = tags.map(tag =>
            `<span class="doc-tag">${this.escapeHtml(tag.name)}</span>`
        ).join('');

        return `<div class="doc-tags">${tagsHtml}</div>`;
    }

    // 获取分类名称
    getCategoryName(categoryId) {
        if (!categoryId) return null;

        // 如果有全局的文档管理器，尝试从中获取分类
        if (typeof documentManager !== 'undefined' && documentManager.categories) {
            const category = documentManager.categories.find(cat => cat.id === categoryId);
            return category ? category.name : null;
        }

        return null;
    }

    // 格式化时间
    formatTime(timeString) {
        if (!timeString) return '未知时间';

        try {
            const date = new Date(timeString);
            const now = new Date();
            const diff = now - date;

            if (diff < 60 * 1000) return '刚刚';
            if (diff < 3600 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`;
            if (diff < 24 * 3600 * 1000) return `${Math.floor(diff / (3600 * 1000))}小时前`;

            const year = date.getFullYear();
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const day = date.getDate().toString().padStart(2, '0');
            return `${year}-${month}-${day}`;
        } catch (error) {
            console.error('格式化时间失败:', error);
            return '未知时间';
        }
    }

    // 截断文本
    truncateText(text, maxLength) {
        if (!text) return '无内容';
        if (text.length <= maxLength) return this.escapeHtml(text);
        return this.escapeHtml(text.substring(0, maxLength)) + '...';
    }

    // HTML转义
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // 获取错误信息
    getErrorMessage(error) {
        if (error.response) {
            if (error.response.status === 404) {
                return 'API接口不存在 (404)';
            } else if (error.response.status === 401) {
                return '未授权，请重新登录 (401)';
            } else if (error.response.status === 500) {
                return '服务器内部错误 (500)';
            } else if (error.response.data && error.response.data.message) {
                return error.response.data.message;
            }
            return `请求失败: ${error.response.status}`;
        } else if (error.request) {
            return '网络连接失败，请检查网络';
        } else {
            return error.message || '未知错误';
        }
    }

    // 更新收藏徽章
    updateFavoritesBadge(count) {
        const badge = document.getElementById('favorites-badge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    }

    // 显示通知
    showNotification(message, type = 'info') {
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };
        const icon = icons[type] || '';

        // 简单的alert通知
        alert(`${icon} ${message}`);
    }
}

// 全局收藏管理器实例
const favoriteManager = new FavoriteManager();

// 初始化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => favoriteManager.initialize(), 1000);
    });
} else {
    setTimeout(() => favoriteManager.initialize(), 1000);
}

// 确保全局可访问
window.favoriteManager = favoriteManager;
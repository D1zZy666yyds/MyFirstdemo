// favorite-manager.js - 完全匹配后端FavoriteController.java的完整修复版

class FavoriteManager {
    constructor() {
        this.favoriteCache = new Map(); // 缓存收藏状态: {docId: {isFavorite: boolean}}
        this.isInitialized = false;
        this.userId = null;
        this.favoritesCount = 0;

        // 筛选相关属性
        this.currentFilterCategory = null;
        this.currentFilterTag = null;
        this.allFavorites = []; // 存储所有收藏文档，用于筛选
        this.categories = []; // 存储收藏分类数据（带favoriteCount）
        this.tags = []; // 存储收藏标签数据（带favoriteCount）
        this.hasFilterInitialized = false;

        // 缓存优化
        this.filterDataCache = {
            categories: [],
            tags: [],
            lastUpdated: null
        };
        this.filterCacheTTL = 5 * 60 * 1000; // 5分钟缓存
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

            // 创建筛选容器（如果不存在）
            this.createFilterContainer();

            // 先加载筛选数据
            await this.loadFilterData();

            // 再绑定事件
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

    // 创建筛选容器
    createFilterContainer() {
        const favoritesPage = document.getElementById('favorites-page');
        if (!favoritesPage) return;

        // 检查是否已有筛选容器
        let filterContainer = document.getElementById('favorites-filters');
        if (!filterContainer) {
            filterContainer = document.createElement('div');
            filterContainer.id = 'favorites-filters';

            // 插入到页面头部后面
            const header = favoritesPage.querySelector('.page-header');
            if (header) {
                header.insertAdjacentElement('afterend', filterContainer);
            } else {
                favoritesPage.insertAdjacentElement('afterbegin', filterContainer);
            }
        }
    }

    // 加载筛选数据（收藏专用）- 完全匹配后端提供的接口
    async loadFilterData(forceRefresh = false) {
        try {
            if (!this.userId) return;

            // 检查缓存
            const now = Date.now();
            if (!forceRefresh &&
                this.filterDataCache.lastUpdated &&
                now - this.filterDataCache.lastUpdated < this.filterCacheTTL) {
                console.log('使用缓存的筛选数据');
                this.categories = this.filterDataCache.categories;
                this.tags = this.filterDataCache.tags;
                return;
            }

            console.log('加载收藏筛选数据...');

            // 同时加载收藏分类和标签 - 完全匹配后端提供的接口
            const [categoriesRes, tagsRes] = await Promise.allSettled([
                axios.get(`/api/favorite/user/${this.userId}/categories`),
                axios.get(`/api/favorite/user/${this.userId}/tags`)
            ]);

            // 处理分类数据 - 完全匹配后端FavoriteController.java
            if (categoriesRes.status === 'fulfilled' &&
                categoriesRes.value.data &&
                categoriesRes.value.data.success) {
                this.categories = categoriesRes.value.data.data || [];
                this.filterDataCache.categories = this.categories;
                console.log('收藏分类数据:', this.categories.length, '个分类');
            } else {
                console.warn('加载收藏分类失败:', categoriesRes.reason);
                this.categories = [];
            }

            // 处理标签数据 - 完全匹配后端FavoriteController.java
            if (tagsRes.status === 'fulfilled' &&
                tagsRes.value.data &&
                tagsRes.value.data.success) {
                this.tags = tagsRes.value.data.data || [];
                this.filterDataCache.tags = this.tags;
                console.log('收藏标签数据:', this.tags.length, '个标签');
            } else {
                console.warn('加载收藏标签失败:', tagsRes.reason);
                this.tags = [];
            }

            this.filterDataCache.lastUpdated = Date.now();
            this.hasFilterInitialized = true;
            console.log('筛选数据加载完成 - 分类:', this.categories.length, '标签:', this.tags.length);

        } catch (error) {
            console.error('加载筛选数据失败:', error);
            this.categories = [];
            this.tags = [];
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
            if (e.target.closest('.btn-refresh-filters')) {
                this.loadFilterData(true).then(() => this.loadFavorites());
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

    // 加载收藏列表（支持筛选）- 完全匹配后端接口
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

            // 确保筛选容器存在
            this.createFilterContainer();

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
            console.log('当前筛选分类:', this.currentFilterCategory);
            console.log('当前筛选标签:', this.currentFilterTag);

            // 构建查询参数（支持筛选）- 完全匹配后端接口
            const params = {};
            if (this.currentFilterCategory) {
                params.categoryId = this.currentFilterCategory;
            }
            if (this.currentFilterTag) {
                params.tagId = this.currentFilterTag;
            }

            console.log('API请求参数:', params);

            // 调用收藏API - 完全匹配后端接口
            const response = await axios.get(`/api/favorite/user/${userId}`, { params });

            console.log('收藏API响应:', response.data);

            if (response.data && response.data.success) {
                const favorites = response.data.data || [];
                this.allFavorites = favorites; // 保存所有收藏用于筛选
                this.favoritesCount = favorites.length;

                // 更新筛选工具栏
                const filterContainer = document.getElementById('favorites-filters');
                if (filterContainer) {
                    const filterHtml = this.createFilterToolbar();
                    filterContainer.innerHTML = filterHtml;
                }

                // 如果没有筛选数据，重新加载一次
                if (!this.hasFilterInitialized || (this.categories.length === 0 && favorites.length > 0)) {
                    console.log('重新加载筛选数据...');
                    await this.loadFilterData(true);
                }

                if (favorites.length === 0) {
                    // 检查是否有筛选条件
                    if (this.currentFilterCategory || this.currentFilterTag) {
                        // 有筛选条件但没有结果
                        const filterStatus = this.getFilterStatusText();
                        container.innerHTML = `
                            <div class="favorites-empty">
                                <div class="empty-icon">🔍</div>
                                <h3>未找到符合条件的收藏</h3>
                                <p>${filterStatus}</p>
                                <button id="clear-filters-in-empty" class="btn-secondary" style="margin-top: 15px;">
                                    清除筛选
                                </button>
                            </div>
                        `;

                        // 绑定清除筛选按钮事件
                        const clearBtn = container.querySelector('#clear-filters-in-empty');
                        if (clearBtn) {
                            clearBtn.addEventListener('click', () => this.clearFilters());
                        }
                    } else {
                        // 没有收藏文档
                        container.innerHTML = `
                            <div class="favorites-empty">
                                <div class="empty-icon">❤️</div>
                                <h3>暂无收藏文档</h3>
                                <p>快去收藏你喜欢的文档吧</p>
                            </div>
                        `;
                    }
                    this.updateFavoritesBadge(0);

                    // 绑定筛选事件
                    this.bindFilterEvents();
                    return;
                }

                // 应用筛选并显示文档
                const filteredFavorites = this.applyFiltersToData(favorites);
                this.displayFavorites(container, filteredFavorites);

                // 更新筛选器UI
                this.updateFilterUI();

                // 绑定筛选事件
                this.bindFilterEvents();

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

    // 创建筛选工具栏 - 完全匹配后端数据结构
    createFilterToolbar() {
        const categories = this.categories || [];
        const tags = this.tags || [];
        const isFiltering = this.currentFilterCategory || this.currentFilterTag;

        console.log('创建筛选工具栏 - 分类数量:', categories.length, '标签数量:', tags.length);

        // 如果没有筛选数据，显示简化的工具栏
        if (categories.length === 0 && tags.length === 0) {
            return `
                <div class="favorites-filter-toolbar">
                    <div class="filter-row">
                        <div class="filter-status">
                            ${this.getFilterStatusText()}
                        </div>
                        <div class="filter-actions">
                            <button class="btn-refresh-favorites btn-secondary" title="刷新">
                                🔄 刷新收藏
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }

        return `
            <div class="favorites-filter-toolbar">
                <div class="filter-row">
                    ${categories.length > 0 ? `
                    <div class="filter-group">
                        <label for="favorite-category-filter">分类筛选:</label>
                        <select id="favorite-category-filter" class="form-select">
                            <option value="">全部分类</option>
                            ${categories.map(cat => `
                                <option value="${cat.id}" ${this.currentFilterCategory === cat.id ? 'selected' : ''}>
                                    ${this.escapeHtml(cat.name || '未命名')}${cat.favoriteCount ? ` (${cat.favoriteCount})` : ''}
                                </option>
                            `).join('')}
                        </select>
                    </div>
                    ` : ''}
                    
                    ${tags.length > 0 ? `
                    <div class="filter-group">
                        <label for="favorite-tag-filter">标签筛选:</label>
                        <select id="favorite-tag-filter" class="form-select">
                            <option value="">全部标签</option>
                            ${tags.map(tag => `
                                <option value="${tag.id}" ${this.currentFilterTag === tag.id ? 'selected' : ''}>
                                    ${this.escapeHtml(tag.name || '未命名')}${tag.favoriteCount ? ` (${tag.favoriteCount})` : ''}
                                </option>
                            `).join('')}
                        </select>
                    </div>
                    ` : ''}
                    
                    <div class="filter-actions">
                        ${isFiltering ? `
                            <button id="clear-favorite-filters" class="btn-secondary">
                                清除筛选
                            </button>
                        ` : ''}
                        <button class="btn-refresh-filters btn-secondary" title="刷新筛选数据">
                            🔄 筛选数据
                        </button>
                        <button class="btn-refresh-favorites btn-secondary" title="刷新收藏">
                            🔄 刷新收藏
                        </button>
                    </div>
                </div>
                <div class="filter-status" id="favorite-filter-status">
                    ${this.getFilterStatusText()}
                </div>
            </div>
        `;
    }

    // 显示收藏文档 - 完全匹配后端数据结构
    displayFavorites(container, favorites) {
        console.log('显示收藏文档:', favorites.length);

        // 清空内容区域
        container.innerHTML = '';

        if (favorites.length === 0) {
            const filterStatus = this.getFilterStatusText();
            container.innerHTML = `
                <div class="favorites-empty">
                    <div class="empty-icon">🔍</div>
                    <h3>未找到符合条件的收藏</h3>
                    <p>${filterStatus}</p>
                    <button id="clear-filters-in-empty" class="btn-secondary" style="margin-top: 15px;">
                        清除筛选
                    </button>
                </div>
            `;

            // 绑定清除筛选按钮事件
            const clearBtn = container.querySelector('#clear-filters-in-empty');
            if (clearBtn) {
                clearBtn.addEventListener('click', () => this.clearFilters());
            }

            // 绑定筛选事件
            this.bindFilterEvents();
            return;
        }

        // 创建收藏网格
        const grid = document.createElement('div');
        grid.className = 'favorites-grid';

        favorites.forEach(doc => {
            // 完全匹配后端返回的数据结构
            const categoryName = this.getCategoryName(doc.categoryId) || '未分类';
            const tagsHtml = this.renderDocumentTags(doc.tags || []);
            const contentPreview = this.truncateText(doc.content || '', 100);
            const timeStr = this.formatTime(doc.createdTime || doc.createTime);
            const favoriteCount = doc.favoriteCount || 0;
            const isFavorite = doc.isFavorite !== false; // 默认已收藏

            const card = document.createElement('div');
            card.className = 'favorite-doc-card';
            card.setAttribute('data-document-id', doc.id);
            card.innerHTML = `
                <div class="doc-header">
                    <h4 class="doc-title" style="cursor: pointer;">
                        ${this.escapeHtml(doc.title || '无标题')}
                    </h4>
                    <button class="favorite-button favorited" data-doc-id="${doc.id}" title="取消收藏">
                        ${favoriteCount > 0 ? `❤️ ${favoriteCount}` : '❤️'}
                    </button>
                </div>
                <div class="doc-meta">
                    <span>📁 ${categoryName}</span>
                    <span>📅 ${timeStr}</span>
                    <span>⭐ 已收藏</span>
                </div>
                ${tagsHtml}
                <div class="doc-preview" style="cursor: pointer;">
                    ${contentPreview}
                </div>
                <div class="doc-actions">
                    <button class="btn-small btn-view" data-doc-id="${doc.id}">查看</button>
                    <button class="btn-small btn-edit" data-doc-id="${doc.id}">编辑</button>
                    <button class="btn-small btn-danger btn-delete" data-doc-id="${doc.id}">删除</button>
                </div>
            `;

            grid.appendChild(card);
        });

        container.appendChild(grid);

        // 绑定事件
        this.bindFavoritesCardEvents();
        this.bindFilterEvents();

        // 更新徽章
        this.updateFavoritesBadge(this.allFavorites.length);
    }

    // 绑定筛选事件
    bindFilterEvents() {
        // 分类筛选器
        const categoryFilter = document.getElementById('favorite-category-filter');
        if (categoryFilter) {
            categoryFilter.addEventListener('change', (e) => {
                const categoryId = e.target.value;
                console.log('收藏分类筛选改变:', categoryId ? categoryId : '全部分类');

                this.currentFilterCategory = categoryId ? parseInt(categoryId) : null;
                this.applyFilters();
            });
        }

        // 标签筛选器
        const tagFilter = document.getElementById('favorite-tag-filter');
        if (tagFilter) {
            tagFilter.addEventListener('change', (e) => {
                const tagId = e.target.value;
                console.log('收藏标签筛选改变:', tagId ? tagId : '全部标签');

                this.currentFilterTag = tagId ? parseInt(tagId) : null;
                this.applyFilters();
            });
        }

        // 清除筛选按钮
        const clearFilterBtn = document.getElementById('clear-favorite-filters');
        if (clearFilterBtn) {
            clearFilterBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.clearFilters();
            });
        }

        // 刷新筛选数据按钮
        document.querySelectorAll('.btn-refresh-filters').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                await this.loadFilterData(true);
                await this.loadFavorites();
            });
        });

        // 刷新收藏按钮
        document.querySelectorAll('.btn-refresh-favorites').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                await this.loadFavorites();
            });
        });
    }

    // 应用筛选
    applyFilters() {
        console.log('应用筛选 - 分类:', this.currentFilterCategory, '标签:', this.currentFilterTag);

        const filteredFavorites = this.applyFiltersToData(this.allFavorites);
        const container = document.getElementById('favorites-content');
        const filterContainer = document.getElementById('favorites-filters');

        if (!container || !filterContainer) return;

        // 更新筛选工具栏
        const filterHtml = this.createFilterToolbar();
        filterContainer.innerHTML = filterHtml;

        if (filteredFavorites.length === 0) {
            // 没有匹配的收藏
            const filterStatus = this.getFilterStatusText();
            container.innerHTML = `
                <div class="favorites-empty">
                    <div class="empty-icon">🔍</div>
                    <h3>未找到符合条件的收藏</h3>
                    <p>${filterStatus}</p>
                    <button id="clear-filters-in-empty" class="btn-secondary" style="margin-top: 15px;">
                        清除筛选
                    </button>
                </div>
            `;

            // 绑定清除筛选按钮事件
            const clearBtn = container.querySelector('#clear-filters-in-empty');
            if (clearBtn) {
                clearBtn.addEventListener('click', () => this.clearFilters());
            }
        } else {
            // 显示筛选后的收藏
            this.displayFavorites(container, filteredFavorites);
        }

        // 绑定筛选事件
        this.bindFilterEvents();
        this.updateFilterStatus();
    }

    // 对数据进行筛选 - 完全匹配后端数据结构
    applyFiltersToData(favorites) {
        if (!favorites || !Array.isArray(favorites)) return [];

        let filtered = [...favorites];

        // 按分类筛选 - 使用 categoryId 字段
        if (this.currentFilterCategory) {
            filtered = filtered.filter(doc => {
                if (!doc.categoryId) return false;
                return doc.categoryId == this.currentFilterCategory;
            });
        }

        // 按标签筛选 - 使用 tags 字段
        if (this.currentFilterTag) {
            filtered = filtered.filter(doc => {
                if (!doc.tags || !Array.isArray(doc.tags)) return false;
                return doc.tags.some(tag => tag.id == this.currentFilterTag);
            });
        }

        console.log('筛选结果:', filtered.length, '个文档');
        return filtered;
    }

    // 清除筛选
    clearFilters() {
        console.log('清除收藏筛选');

        this.currentFilterCategory = null;
        this.currentFilterTag = null;

        // 更新筛选工具栏
        const filterContainer = document.getElementById('favorites-filters');
        if (filterContainer) {
            const filterHtml = this.createFilterToolbar();
            filterContainer.innerHTML = filterHtml;
        }

        // 显示所有收藏
        const container = document.getElementById('favorites-content');
        if (container) {
            this.displayFavorites(container, this.allFavorites);
        }

        // 绑定筛选事件
        this.bindFilterEvents();
        this.updateFilterStatus();
    }

    // 更新筛选状态显示
    updateFilterStatus() {
        const statusElement = document.getElementById('favorite-filter-status');
        if (statusElement) {
            statusElement.textContent = this.getFilterStatusText();
        }

        // 更新清除按钮显示
        const clearBtn = document.getElementById('clear-favorite-filters');
        if (clearBtn) {
            if (this.currentFilterCategory || this.currentFilterTag) {
                clearBtn.style.display = 'inline-block';
            } else {
                clearBtn.style.display = 'none';
            }
        }
    }

    // 更新筛选器UI
    updateFilterUI() {
        const categoryFilter = document.getElementById('favorite-category-filter');
        const tagFilter = document.getElementById('favorite-tag-filter');

        if (categoryFilter) {
            categoryFilter.value = this.currentFilterCategory || "";
        }

        if (tagFilter) {
            tagFilter.value = this.currentFilterTag || "";
        }

        this.updateFilterStatus();
    }

    // 获取筛选状态文本
    getFilterStatusText() {
        const filteredCount = this.applyFiltersToData(this.allFavorites).length;
        const totalCount = this.allFavorites.length;

        if (this.currentFilterCategory && this.currentFilterTag) {
            const category = this.categories.find(c => c.id == this.currentFilterCategory);
            const tag = this.tags.find(t => t.id == this.currentFilterTag);
            return `筛选: ${category?.name || '未知分类'} + ${tag?.name || '未知标签'} (${filteredCount}/${totalCount})`;
        } else if (this.currentFilterCategory) {
            const category = this.categories.find(c => c.id == this.currentFilterCategory);
            return `筛选: ${category?.name || '未知分类'} (${filteredCount}/${totalCount})`;
        } else if (this.currentFilterTag) {
            const tag = this.tags.find(t => t.id == this.currentFilterTag);
            return `筛选: ${tag?.name || '未知标签'} (${filteredCount}/${totalCount})`;
        } else {
            return `全部收藏 (${totalCount}个文档)`;
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

        // 批量获取收藏状态 - 使用后端批量接口
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
        let doc;

        if (docOrEvent && docOrEvent.id) {
            doc = docOrEvent;
        } else if (docOrEvent && docOrEvent.detail && docOrEvent.detail.document) {
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

    // 切换收藏状态 - 完全匹配后端接口
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
                // 添加收藏 - 完全匹配后端接口
                const response = await axios.post(`/api/favorite/document/${docId}?userId=${userId}`);
                if (response.data && response.data.success) {
                    button.innerHTML = '❤️';
                    button.className = 'favorite-button favorited';
                    button.title = '取消收藏';
                    this.favoriteCache.set(docId, { isFavorite: true });
                    this.showNotification('收藏成功', 'success');

                    // 刷新筛选数据
                    setTimeout(() => this.loadFilterData(true), 500);
                }
            } else {
                // 取消收藏 - 完全匹配后端接口
                const response = await axios.delete(`/api/favorite/document/${docId}?userId=${userId}`);
                if (response.data && response.data.success) {
                    button.innerHTML = '🤍';
                    button.className = 'favorite-button';
                    button.title = '收藏';
                    this.favoriteCache.set(docId, { isFavorite: false });
                    this.showNotification('已取消收藏', 'info');

                    // 刷新筛选数据
                    setTimeout(() => this.loadFilterData(true), 500);
                }
            }

        } catch (error) {
            console.error('切换收藏状态失败:', error);
            this.showNotification('操作失败: ' + this.getErrorMessage(error), 'error');
        }
    }

    // 检查单个文档的收藏状态 - 完全匹配后端接口
    async checkFavoriteStatus(docId) {
        try {
            const userId = this.userId;
            if (!userId) return false;

            // 检查缓存
            const cached = this.favoriteCache.get(docId);
            if (cached !== undefined) {
                return cached.isFavorite;
            }

            // 查询服务器 - 完全匹配后端接口
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

    // 批量检查收藏状态 - 完全匹配后端接口
    async batchCheckFavoriteStatus(docIds) {
        try {
            const userId = this.userId;
            if (!userId) return docIds.map(() => false);

            // 检查缓存中已有的结果
            const cachedResults = [];
            const uncachedIds = [];

            docIds.forEach(docId => {
                const cached = this.favoriteCache.get(docId);
                if (cached !== undefined) {
                    cachedResults.push({ docId, isFavorite: cached.isFavorite });
                } else {
                    uncachedIds.push(docId);
                }
            });

            // 如果所有都在缓存中，直接返回
            if (uncachedIds.length === 0) {
                return docIds.map(docId =>
                    cachedResults.find(r => r.docId === docId).isFavorite
                );
            }

            // 使用批量接口查询未缓存的 - 完全匹配后端接口
            try {
                const response = await axios.post('/api/favorite/batch-check', {
                    documentIds: uncachedIds
                }, {
                    params: { userId }
                });

                if (response.data && response.data.success) {
                    const batchResult = response.data.data;

                    // 更新缓存
                    Object.entries(batchResult).forEach(([docIdStr, isFavorite]) => {
                        const docId = parseInt(docIdStr);
                        this.favoriteCache.set(docId, { isFavorite });
                    });

                    // 合并结果
                    const allResults = [...cachedResults];
                    Object.entries(batchResult).forEach(([docIdStr, isFavorite]) => {
                        const docId = parseInt(docIdStr);
                        allResults.push({ docId, isFavorite });
                    });

                    // 按原始顺序返回
                    return docIds.map(docId =>
                        allResults.find(r => r.docId === docId)?.isFavorite || false
                    );
                }
            } catch (batchError) {
                console.warn('批量接口失败，降级为单个查询:', batchError);
            }

            // 降级为单个查询
            const results = [];
            for (const docId of docIds) {
                const status = await this.checkFavoriteStatus(docId);
                results.push(status);
            }
            return results;

        } catch (error) {
            console.error('批量获取收藏状态失败:', error);
            // 降级为单个查询
            const results = [];
            for (const docId of docIds) {
                const status = await this.checkFavoriteStatus(docId);
                results.push(status);
            }
            return results;
        }
    }

    // 新功能：获取文档收藏数量 - 匹配后端接口
    async getFavoriteCount(docId) {
        try {
            const response = await axios.get(`/api/favorite/document/${docId}/count`);
            if (response.data && response.data.success) {
                return response.data.data;
            }
            return 0;
        } catch (error) {
            console.error('获取收藏数量失败:', error);
            return 0;
        }
    }

    // 新功能：获取收藏统计信息 - 匹配后端接口
    async getFavoriteStats() {
        try {
            const userId = this.userId;
            if (!userId) return null;

            const response = await axios.get(`/api/favorite/user/${userId}/stats`);
            if (response.data && response.data.success) {
                return response.data.data;
            }
            return null;
        } catch (error) {
            console.error('获取收藏统计失败:', error);
            return null;
        }
    }

    // 新功能：获取热门收藏 - 匹配后端接口
    async getHotFavorites(limit = 10) {
        try {
            const userId = this.userId;
            if (!userId) return [];

            const response = await axios.get(`/api/favorite/hot/${userId}?limit=${limit}`);
            if (response.data && response.data.success) {
                return response.data.data || [];
            }
            return [];
        } catch (error) {
            console.error('获取热门收藏失败:', error);
            return [];
        }
    }

    // 新功能：丰富文档信息 - 匹配后端接口
    async enrichDocumentsWithFavoriteInfo(documents) {
        try {
            const userId = this.userId;
            if (!userId) return documents;

            const response = await axios.post(`/api/favorite/enrich-documents?userId=${userId}`, documents);
            if (response.data && response.data.success) {
                return response.data.data;
            }
            return documents;
        } catch (error) {
            console.error('丰富文档信息失败:', error);
            return documents;
        }
    }

    // 新功能：获取分类统计 - 匹配后端接口
    async getCategoryFavoriteCounts() {
        try {
            const userId = this.userId;
            if (!userId) return {};

            const response = await axios.get(`/api/favorite/category-stats/${userId}`);
            if (response.data && response.data.success) {
                return response.data.data || {};
            }
            return {};
        } catch (error) {
            console.error('获取分类统计失败:', error);
            return {};
        }
    }

    // 新功能：获取用户收藏总数 - 匹配后端接口
    async getFavoriteCountByUser() {
        try {
            const userId = this.userId;
            if (!userId) return 0;

            const response = await axios.get(`/api/favorite/user/${userId}/count`);
            if (response.data && response.data.success) {
                return response.data.data || 0;
            }
            return 0;
        } catch (error) {
            console.error('获取用户收藏总数失败:', error);
            return 0;
        }
    }

    // 渲染文档标签
    renderDocumentTags(tags) {
        if (!tags || tags.length === 0) {
            return '';
        }

        const tagsHtml = tags.map(tag =>
            `<span class="doc-tag" data-tag-id="${tag.id}">${this.escapeHtml(tag.name)}</span>`
        ).join('');

        return `<div class="doc-tags">${tagsHtml}</div>`;
    }

    // 获取分类名称
    getCategoryName(categoryId) {
        if (!categoryId) return null;

        // 从收藏的分类列表中查找
        const favoriteCategory = this.categories.find(cat => cat.id === categoryId);
        if (favoriteCategory) {
            return favoriteCategory.name;
        }

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
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');

            // 如果是一年内，显示月日时分
            if (year === now.getFullYear()) {
                return `${month}-${day} ${hours}:${minutes}`;
            }

            // 否则显示年月日
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

    // 搜索收藏文档 - 完全匹配后端接口
    async searchFavoriteDocuments(keyword, categoryId = null, tagId = null) {
        try {
            const userId = this.userId;
            if (!userId) {
                this.showNotification('请先登录', 'warning');
                return [];
            }

            if (!keyword || keyword.trim() === '') {
                return this.getUserFavorites();
            }

            const params = { keyword: keyword.trim() };
            if (categoryId) params.categoryId = categoryId;
            if (tagId) params.tagId = tagId;

            // 完全匹配后端接口
            const response = await axios.get(`/api/favorite/search/${userId}`, { params });

            if (response.data && response.data.success) {
                return response.data.data || [];
            } else {
                throw new Error(response.data?.message || '搜索失败');
            }
        } catch (error) {
            console.error('搜索收藏文档失败:', error);
            this.showNotification('搜索失败: ' + error.message, 'error');
            return [];
        }
    }

    // 获取用户收藏（用于兼容）
    async getUserFavorites() {
        const response = await axios.get(`/api/favorite/user/${this.userId}`);
        if (response.data && response.data.success) {
            return response.data.data || [];
        }
        return [];
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
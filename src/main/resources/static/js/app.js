// 修复后的完整 app.js
class KnowledgeBaseApp {
    constructor() {
        this.currentPage = 'dashboard';
        this.categories = [];
        this.categoryCache = new Map(); // 缓存分类ID到名称的映射
        this.searchHistory = JSON.parse(localStorage.getItem('searchHistory') || '[]');
        this.init();
    }

    async init() {
        console.log('知识库应用初始化...');

        try {
            // 先检查认证状态
            const isAuthenticated = await authManager.checkAuthStatus();

            if (!isAuthenticated) {
                console.log('用户未认证，跳转到登录页');
                authManager.redirectToLogin();
                return;
            }

            console.log('用户已认证，继续初始化应用');
            this.setupNavigation();
            await this.loadInitialData();
            this.setupEventListeners();

        } catch (error) {
            console.error('应用初始化失败:', error);
            this.showError('应用初始化失败: ' + error.message);
        }
    }

    setupNavigation() {
        // 监听hash变化来切换页面
        window.addEventListener('hashchange', () => {
            this.handleRouteChange();
        });

        // 初始路由处理
        this.handleRouteChange();
    }

    handleRouteChange() {
        const hash = window.location.hash.slice(1) || 'dashboard';
        this.showPage(hash);
    }

    showPage(pageName) {
        console.log('切换页面:', pageName);

        // 隐藏所有页面
        document.querySelectorAll('.page').forEach(page => {
            page.classList.remove('active');
        });

        // 更新导航激活状态
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });

        // 显示目标页面
        const targetPage = document.getElementById(`${pageName}-page`);
        const targetNav = document.querySelector(`[href="#${pageName}"]`);

        if (targetPage) {
            targetPage.classList.add('active');
        }
        if (targetNav) {
            targetNav.classList.add('active');
        }

        this.currentPage = pageName;

        // 加载页面特定数据
        this.loadPageData(pageName);
    }

    // 清理模态框的方法
    clearModals() {
        const modalContainer = document.getElementById('modal-container');
        if (modalContainer) {
            modalContainer.innerHTML = '';
        }
    }

    async loadPageData(pageName) {
        console.log('加载页面数据:', pageName);

        try {
            // 在切换页面时，清理之前的模态框
            this.clearModals();

            // 根据页面名称加载数据
            switch (pageName) {
                case 'dashboard':
                    if (window.dashboardManager) {
                        await dashboardManager.loadDashboardData();
                    }
                    break;
                case 'documents':
                    await this.loadDocumentsPage();
                    break;
                case 'search':
                    await this.setupSearchPage();
                    break;
                case 'tags':
                    if (window.tagManager) {
                        await tagManager.init();
                    }
                    break;
                case 'knowledge-graph':
                    if (window.knowledgeGraphManager) {
                        setTimeout(() => {
                            if (!window.knowledgeGraphManager.initialized) {
                                window.knowledgeGraphManager.init();
                            } else {
                                window.knowledgeGraphManager.onPageShow();
                            }
                        }, 200);
                    }
                    break;
            }
        } catch (error) {
            console.error('加载页面数据失败:', error);
            this.showError('加载页面数据失败: ' + error.message);
        }
    }

    async loadDocumentsPage() {
        try {
            // 检查URL参数是否有分类ID
            const urlParams = new URLSearchParams(window.location.search);
            const categoryId = urlParams.get('categoryId');

            // 确保文档管理器已初始化
            if (window.documentManager) {
                if (!window.documentManager.isInitialized) {
                    console.log('文档管理器未初始化，正在初始化...');
                    await window.documentManager.initialize();
                }

                // 如果有分类ID，加载该分类的文档
                if (categoryId) {
                    await window.documentManager.loadDocuments(categoryId);
                } else {
                    await window.documentManager.loadDocuments();
                }
            } else {
                console.error('文档管理器未加载');

                // 尝试重新获取文档管理器
                setTimeout(() => {
                    if (window.documentManager) {
                        // 延迟重新执行
                        this.loadDocumentsPage();
                    } else {
                        this.showError('文档管理器未加载，请刷新页面');
                    }
                }, 100);
            }
        } catch (error) {
            console.error('加载文档页面失败:', error);
            this.showError('加载文档页面失败: ' + error.message);
        }
    }

    async loadInitialData() {
        console.log('加载初始数据...');
        // 加载分类和标签等共享数据
        await this.loadCategoriesForFilter();
    }

    async loadCategoriesForFilter() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载用户分类，用户ID:', userId);

            const response = await axios.get('/api/category/user/' + userId);
            console.log('分类响应:', response.data);

            if (response.data.success) {
                this.categories = response.data.data || [];

                // 建立分类缓存
                this.categoryCache.clear();
                this.categories.forEach(category => {
                    if (category.id && category.name) {
                        this.categoryCache.set(category.id, category.name);
                    }
                });

                console.log('分类缓存建立完成:', this.categoryCache.size, '条记录');
                this.updateCategoryFilters();
                console.log('加载分类完成:', this.categories.length);
            } else {
                console.error('加载分类失败:', response.data.message);
            }
        } catch (error) {
            console.error('加载分类失败:', error);
            // 不显示错误，因为分类过滤器不是关键功能
        }
    }

    // 获取分类显示名称（简化版）
    getCategoryDisplay(categoryInfo) {
        if (!categoryInfo && categoryInfo !== 0) return '未分类';

        // 情况1：已经是字符串名称
        if (typeof categoryInfo === 'string') {
            return categoryInfo;
        }

        // 情况2：是数字ID
        if (typeof categoryInfo === 'number' || /^\d+$/.test(String(categoryInfo))) {
            const categoryId = Number(categoryInfo);
            const cachedName = this.categoryCache.get(categoryId);
            return cachedName || `分类${categoryId}`;
        }

        // 情况3：是对象
        if (typeof categoryInfo === 'object') {
            if (categoryInfo.name) {
                return categoryInfo.name;
            }
            if (categoryInfo.id) {
                const cachedName = this.categoryCache.get(categoryInfo.id);
                return cachedName || `分类${categoryInfo.id}`;
            }
        }

        return '未分类';
    }

    updateCategoryFilters() {
        const categorySelects = document.querySelectorAll('#category-filter, #search-category');
        categorySelects.forEach(select => {
            if (select) {
                select.innerHTML = '<option value="">全部分类</option>';
                if (this.categories && this.categories.length > 0) {
                    this.categories.forEach(category => {
                        const option = document.createElement('option');
                        option.value = category.id;
                        option.textContent = category.name;
                        select.appendChild(option);
                    });
                }
            }
        });
    }

    setupEventListeners() {
        // 全局搜索 - 增强功能
        const globalSearch = document.getElementById('global-search');
        if (globalSearch) {
            // 实时搜索建议
            globalSearch.addEventListener('input', this.debounce((e) => {
                const keyword = e.target.value.trim();
                if (keyword.length > 0) {
                    this.showSearchSuggestions(keyword);
                } else {
                    this.hideSearchSuggestions();
                }
            }, 300));

            globalSearch.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });

            // 点击其他地方隐藏建议
            document.addEventListener('click', (e) => {
                if (!e.target.closest('.search-header')) {
                    this.hideSearchSuggestions();
                }
            });
        }

        // 标签筛选
        const searchTag = document.getElementById('search-tag');
        if (searchTag) {
            searchTag.addEventListener('change', () => {
                console.log('标签筛选变化:', searchTag.value);
                this.performSearch();
            });
        }

        // 分类筛选
        const searchCategory = document.getElementById('search-category');
        if (searchCategory) {
            searchCategory.addEventListener('change', () => {
                console.log('分类筛选变化:', searchCategory.value);
                this.performSearch();
            });
        }
    }

    // 防抖函数
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // 显示搜索建议
    async showSearchSuggestions(keyword) {
        if (!keyword || keyword.length < 2) {
            this.hideSearchSuggestions();
            return;
        }

        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get('/api/search/suggestions', {
                params: {
                    keyword,
                    userId: userId,
                    limit: 5
                }
            });

            if (response.data.success) {
                this.displaySearchSuggestions(response.data.data, keyword);
            }
        } catch (error) {
            console.error('获取搜索建议失败:', error);
            this.hideSearchSuggestions();
        }
    }

    // 显示搜索建议
    displaySearchSuggestions(suggestions, keyword) {
        let suggestionsContainer = document.getElementById('search-suggestions');
        if (!suggestionsContainer) {
            suggestionsContainer = document.createElement('div');
            suggestionsContainer.id = 'search-suggestions';
            suggestionsContainer.className = 'search-suggestions';
            document.querySelector('.search-header').appendChild(suggestionsContainer);
        }

        if (!suggestions || suggestions.length === 0) {
            suggestionsContainer.innerHTML = `
                <div class="suggestion-item no-suggestions">
                    <span>无相关建议</span>
                </div>
            `;
            suggestionsContainer.style.display = 'block';
            return;
        }

        suggestionsContainer.innerHTML = suggestions.map(suggestion => `
            <div class="suggestion-item" onclick="app.selectSuggestion('${this.escapeHtml(suggestion)}')">
                <span class="suggestion-text">${this.highlightText(suggestion, keyword)}</span>
                <span class="suggestion-type">建议</span>
            </div>
        `).join('');

        // 添加搜索历史
        const historyItems = this.searchHistory
            .filter(item => item.toLowerCase().includes(keyword.toLowerCase()))
            .slice(0, 3)
            .map(item => `
                <div class="suggestion-item" onclick="app.selectSuggestion('${this.escapeHtml(item)}')">
                    <span class="suggestion-text">${this.highlightText(item, keyword)}</span>
                    <span class="suggestion-type history">历史</span>
                </div>
            `).join('');

        if (historyItems) {
            suggestionsContainer.innerHTML += historyItems;
        }

        suggestionsContainer.style.display = 'block';
    }

    // 隐藏搜索建议
    hideSearchSuggestions() {
        const suggestionsContainer = document.getElementById('search-suggestions');
        if (suggestionsContainer) {
            suggestionsContainer.style.display = 'none';
        }
    }

    // 选择搜索建议
    selectSuggestion(suggestion) {
        const globalSearch = document.getElementById('global-search');
        if (globalSearch) {
            globalSearch.value = suggestion;
            this.hideSearchSuggestions();
            this.performSearch();
        }
    }

    // 核心修复：执行搜索
    async performSearch() {
        const globalSearch = document.getElementById('global-search');
        const searchCategory = document.getElementById('search-category');
        const searchTag = document.getElementById('search-tag');

        if (!globalSearch) {
            console.error('搜索输入框未找到');
            return;
        }

        const keyword = globalSearch.value.trim();

        if (!keyword) {
            this.showError('请输入搜索关键词');
            return;
        }

        // 保存搜索历史
        this.saveToSearchHistory(keyword);

        try {
            const userId = authManager.getCurrentUserId();
            this.showSearchLoading(true);

            console.log('🔍 开始搜索:', keyword);

            let response;

            // 根据筛选条件选择API
            const categoryId = searchCategory && searchCategory.value ? parseInt(searchCategory.value) : null;
            const tagId = searchTag && searchTag.value ? parseInt(searchTag.value) : null;

            if (categoryId && tagId) {
                // 高级搜索
                const searchCriteria = {
                    keyword: keyword,
                    categoryId: categoryId,
                    tagIds: [tagId],
                    limit: 50
                };
                response = await axios.post('/api/search/advanced', searchCriteria, {
                    params: { userId: userId }
                });
            } else if (categoryId) {
                // 分类搜索
                response = await axios.get('/api/search/category', {
                    params: {
                        keyword: keyword,
                        categoryId: categoryId,
                        userId: userId,
                        limit: 50
                    }
                });
            } else if (tagId) {
                // 标签搜索
                response = await axios.get('/api/search/tag', {
                    params: {
                        keyword: keyword,
                        tagId: tagId,
                        userId: userId,
                        limit: 50
                    }
                });
            } else {
                // 基础搜索
                response = await axios.get('/api/search', {
                    params: {
                        keyword: keyword,
                        userId: userId,
                        limit: 50
                    }
                });
            }

            console.log('搜索响应:', response.data);

            if (response.data.success) {
                const documents = response.data.data || [];
                console.log(`接收到 ${documents.length} 个文档`);

                // 显示搜索结果
                this.displaySearchResults(documents, keyword);

                // 更新搜索历史显示
                this.displaySearchHistory();
            } else {
                throw new Error(response.data.message || '搜索失败');
            }
        } catch (error) {
            console.error('搜索失败:', error);
            this.showError('搜索失败: ' + error.message);
            this.displaySearchResults([], keyword);
        } finally {
            this.showSearchLoading(false);
        }
    }

    // 渲染搜索结果项（兼容多种字段名）
    renderSearchResultItem(doc, keyword) {
        // 验证文档数据
        if (!doc || !doc.id) {
            console.warn('无效的文档数据:', doc);
            return '';
        }

        // 🎯 兼容性处理：支持多种字段名
        const docId = doc.id || doc.docId;
        const title = doc.title || doc.name || '无标题';
        const content = doc.content || '无内容';
        const categoryId = doc.categoryId || doc.category;
        const tags = doc.tags || doc.tagList || [];
        const updateTime = doc.updateTime || doc.updatedTime || doc.createdTime;

        // 获取文档标题（带高亮）
        const highlightedTitle = this.highlightText(title, keyword);

        // 获取内容预览（简单截取）
        const contentPreview = content ?
            this.highlightText(this.getSimplePreview(content, 150), keyword) :
            '无内容';

        // 🎯 修复：使用新的分类显示方法
        const categoryDisplay = this.getCategoryDisplay(categoryId);

        // 🎯 修复：标签显示（兼容数组和对象列表）
        let tagsDisplay = this.formatTags(tags);

        // 格式化时间
        const formattedTime = updateTime ?
            new Date(updateTime).toLocaleString('zh-CN') : '未知';

        return `
        <div class="search-result-item" data-doc-id="${docId}">
            <div class="result-header">
                <h4 class="result-title">${highlightedTitle}</h4>
                <div class="result-actions">
                    <button onclick="app.viewSearchDocument(${docId})" class="btn-small" title="查看">👁️</button>
                    <button onclick="app.editSearchDocument(${docId})" class="btn-small" title="编辑">✏️</button>
                </div>
            </div>
            <div class="result-content">
                <p class="doc-preview">${contentPreview}</p>
            </div>
            <div class="result-meta">
                <span class="meta-item">
                    <strong>分类:</strong> ${categoryDisplay}
                </span>
                <span class="meta-item">
                    <strong>标签:</strong> ${tagsDisplay}
                </span>
                <span class="meta-item">
                    <strong>更新时间:</strong> ${formattedTime}
                </span>
            </div>
        </div>
    `;
    }


    // 显示搜索加载状态
    showSearchLoading(loading) {
        const searchButton = document.querySelector('.search-header .btn-primary');
        const resultsContainer = document.getElementById('search-results');

        if (loading) {
            if (searchButton) searchButton.disabled = true;
            if (resultsContainer) {
                resultsContainer.innerHTML = `
                    <div class="search-loading">
                        <div class="loading-spinner"></div>
                        <p>搜索中...</p>
                    </div>
                `;
            }
        } else {
            if (searchButton) searchButton.disabled = false;
        }
    }

    // 🎯 核心修复：显示搜索结果（不再过滤）
    displaySearchResults(documents, keyword) {
        const resultsContainer = document.getElementById('search-results');
        if (!resultsContainer) {
            console.error('搜索结果容器未找到');
            return;
        }

        // 验证数据
        if (!Array.isArray(documents)) {
            console.error('返回数据不是数组:', documents);
            documents = [];
        }

        if (documents.length === 0) {
            resultsContainer.innerHTML = this.renderNoResults(keyword);
            return;
        }

        // 🎯 直接显示所有返回的文档，信任后端的搜索结果
        resultsContainer.innerHTML = `
            <div class="search-results-header">
                <div class="results-stats">
                    <span>找到 ${documents.length} 个文档</span>
                    <span class="search-keyword">关键词: "${keyword}"</span>
                </div>
                <div class="results-actions">
                    <button onclick="app.clearSearchFilters()" class="btn-secondary btn-small">清除筛选</button>
                </div>
            </div>
            <div class="search-results-list">
                ${documents.map(doc => this.renderSearchResultItem(doc, keyword)).join('')}
            </div>
        `;
    }

    // 无结果时的显示
    renderNoResults(keyword) {
        return `
            <div class="no-results">
                <div class="no-results-icon">🔍</div>
                <h3>没有找到相关文档</h3>
                <p>关键词: "${keyword}"</p>
                <div class="search-tips">
                    <h4>搜索提示：</h4>
                    <ul>
                        <li>使用更具体的关键词</li>
                        <li>尝试不同的搜索词组合</li>
                        <li>检查筛选条件</li>
                        <li>使用文档标题中的关键词</li>
                    </ul>
                </div>
            </div>
        `;
    }

    // 格式化标签显示
    formatTags(tags) {
        if (!tags) return '无标签';

        // 尝试解析标签数据
        try {
            if (typeof tags === 'string') {
                // 尝试解析JSON字符串
                const parsed = JSON.parse(tags);
                if (Array.isArray(parsed)) {
                    tags = parsed;
                }
            }

            if (Array.isArray(tags)) {
                const tagNames = tags.map(tag => {
                    if (typeof tag === 'string') return tag;
                    if (tag && tag.name) return tag.name;
                    return '';
                }).filter(name => name && name.trim() !== '');

                return tagNames.length > 0 ? tagNames.join(', ') : '无标签';
            }
        } catch (e) {
            // 如果解析失败，尝试直接使用
            if (typeof tags === 'string') {
                return tags;
            }
        }

        return '无标签';
    }

    // 简单内容预览（不包含关键词过滤）
    getSimplePreview(content, maxLength = 150) {
        if (!content) return '';

        // 移除HTML标签
        const plainText = content.replace(/<[^>]*>/g, '');

        // 截取指定长度
        if (plainText.length <= maxLength) {
            return plainText;
        }

        // 截取并在末尾加省略号
        return plainText.substring(0, maxLength) + '...';
    }

    // 处理搜索结果中的文档查看
    async viewSearchDocument(docId) {
        try {
            if (window.documentManager) {
                await window.documentManager.viewDocument(docId);
            } else {
                throw new Error('文档管理器未初始化');
            }
        } catch (error) {
            console.error('查看文档失败:', error);
            this.showError('无法查看文档: ' + error.message);
        }
    }

    // 处理搜索结果中的文档编辑
    async editSearchDocument(docId) {
        try {
            if (window.documentManager) {
                await window.documentManager.editDocument(docId);
            } else {
                throw new Error('文档管理器未初始化');
            }
        } catch (error) {
            console.error('编辑文档失败:', error);
            this.showError('无法编辑文档: ' + error.message);
        }
    }

    // 高亮文本
    highlightText(text, keyword) {
        if (!text || !keyword) return this.escapeHtml(text || '');

        const escapedKeyword = this.escapeRegex(keyword);
        const regex = new RegExp(`(${escapedKeyword})`, 'gi');
        return this.escapeHtml(text).replace(regex, '<mark>$1</mark>');
    }

    // 转义正则表达式特殊字符
    escapeRegex(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    // 保存搜索历史
    saveToSearchHistory(keyword) {
        if (!keyword || keyword.trim() === '') return;

        const trimmedKeyword = keyword.trim();

        // 移除重复项
        this.searchHistory = this.searchHistory.filter(item => item !== trimmedKeyword);
        // 添加到开头
        this.searchHistory.unshift(trimmedKeyword);
        // 限制历史记录数量
        this.searchHistory = this.searchHistory.slice(0, 10);
        // 保存到本地存储
        localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
    }

    // 清除搜索筛选
    clearSearchFilters() {
        const searchCategory = document.getElementById('search-category');
        const searchTag = document.getElementById('search-tag');
        const globalSearch = document.getElementById('global-search');

        if (searchCategory) searchCategory.value = '';
        if (searchTag) searchTag.value = '';

        // 如果搜索框有内容，重新搜索
        if (globalSearch && globalSearch.value.trim()) {
            this.performSearch();
        }
    }

    // 导出搜索结果
    exportSearchResults() {
        const resultsContainer = document.getElementById('search-results');
        if (!resultsContainer) return;

        const results = Array.from(resultsContainer.querySelectorAll('.search-result-item'));
        if (results.length === 0) {
            this.showError('没有可导出的搜索结果');
            return;
        }

        const exportData = results.map(item => {
            const titleElem = item.querySelector('.result-title');
            const categoryElem = item.querySelector('.meta-item:nth-child(1)');
            const timeElem = item.querySelector('.meta-item:nth-child(3)');

            return {
                title: titleElem ? titleElem.textContent.replace(/🔍/g, '').trim() : '',
                category: categoryElem ? categoryElem.textContent.replace('分类:', '').trim() : '',
                updateTime: timeElem ? timeElem.textContent.replace('更新时间:', '').trim() : ''
            };
        }).filter(item => item.title); // 过滤掉空标题

        if (exportData.length === 0) {
            this.showError('没有有效的搜索结果可以导出');
            return;
        }

        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `search-results-${new Date().toISOString().split('T')[0]}.json`;
        a.click();
        URL.revokeObjectURL(url);

        this.showSuccess(`已导出 ${exportData.length} 条搜索结果`);
    }

    async setupSearchPage() {
        console.log('设置搜索页面');
        // 加载标签数据用于筛选
        await this.loadTagsForSearch();

        // 显示搜索历史
        this.displaySearchHistory();
    }

    async loadTagsForSearch() {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/tag/user/${userId}`);

            if (response.data.success) {
                const tags = response.data.data || [];
                this.updateTagFilter(tags);
            }
        } catch (error) {
            console.error('加载标签失败:', error);
        }
    }

    updateTagFilter(tags) {
        const tagSelect = document.getElementById('search-tag');
        if (tagSelect && tags.length > 0) {
            tagSelect.innerHTML = '<option value="">全部标签</option>';
            tags.forEach(tag => {
                const option = document.createElement('option');
                option.value = tag.id;
                option.textContent = tag.name;
                tagSelect.appendChild(option);
            });
        }
    }

    // 显示搜索历史
    displaySearchHistory() {
        const historyContainer = document.getElementById('search-history');
        if (!historyContainer) {
            console.error('搜索历史容器未找到');
            return;
        }

        if (!this.searchHistory || this.searchHistory.length === 0) {
            historyContainer.innerHTML = `
                <div class="search-history-section">
                    <h4>搜索历史</h4>
                    <div class="no-history">
                        <span>暂无搜索历史</span>
                    </div>
                </div>
            `;
            return;
        }

        historyContainer.innerHTML = `
            <div class="search-history-section">
                <div class="search-history-header">
                    <h4>搜索历史</h4>
                    <button onclick="app.clearSearchHistory()" class="btn-secondary btn-small">清除历史</button>
                </div>
                <div class="history-items">
                    ${this.searchHistory.map(item => `
                        <div class="history-item">
                            <span class="history-text" onclick="event.stopPropagation(); app.useHistoryItem('${this.escapeHtml(item)}')">
                                ${this.escapeHtml(item)}
                            </span>
                            <button onclick="event.stopPropagation(); app.removeHistoryItem('${this.escapeHtml(item)}')" class="btn-remove" title="删除此项">×</button>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    // 删除历史项
    removeHistoryItem(item) {
        // event.stopPropagation() 已经在HTML中调用
        this.searchHistory = this.searchHistory.filter(history => history !== item);
        localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
        this.displaySearchHistory();
    }

    // 使用历史项
    useHistoryItem(item) {
        // event.stopPropagation() 已经在HTML中调用
        const globalSearch = document.getElementById('global-search');
        if (globalSearch) {
            globalSearch.value = item;
            this.performSearch();
        }
    }

    clearSearchHistory() {
        this.searchHistory = [];
        localStorage.removeItem('searchHistory');
        this.displaySearchHistory();
    }

    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return String(unsafe)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    showError(message) {
        console.error('应用错误:', message);
        if (typeof window.showNotification === 'function') {
            window.showNotification('error', message);
        } else {
            alert('错误: ' + message);
        }
    }

    showSuccess(message) {
        console.log('应用成功:', message);
        if (typeof window.showNotification === 'function') {
            window.showNotification('success', message);
        } else {
            alert('成功: ' + message);
        }
    }
}

// 全局错误处理
window.addEventListener('error', function(event) {
    console.error('全局错误捕获:', event.error);

    // 屏蔽特定错误
    if (event.error && event.error.message &&
        (event.error.message.includes('未加载') ||
            event.error.message.includes('未初始化'))) {
        event.preventDefault();
        console.log('已处理已知错误:', event.error.message);
    }
});

// 未捕获的Promise错误
window.addEventListener('unhandledrejection', function(event) {
    console.error('未处理的Promise错误:', event.reason);
    event.preventDefault();
});

// 应用初始化
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM加载完成，初始化主应用');
    window.app = new KnowledgeBaseApp();
});
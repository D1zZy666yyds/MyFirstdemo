// 修复后的完整 app.js - 支持分类和标签筛选的搜索版
class KnowledgeBaseApp {
    constructor() {
        this.currentPage = 'dashboard';
        this.categories = [];
        this.categoryCache = new Map();
        this.tagsCache = new Map();
        this.searchHistory = JSON.parse(localStorage.getItem('searchHistory') || '[]');
        this.init();
    }

    async init() {
        console.log('知识库应用初始化...');

        try {
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
        window.addEventListener('hashchange', () => {
            this.handleRouteChange();
        });
        this.handleRouteChange();
    }

    handleRouteChange() {
        const hash = window.location.hash.slice(1) || 'dashboard';
        this.showPage(hash);
    }

    showPage(pageName) {
        console.log('切换页面:', pageName);

        document.querySelectorAll('.page').forEach(page => {
            page.classList.remove('active');
        });

        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });

        const targetPage = document.getElementById(`${pageName}-page`);
        const targetNav = document.querySelector(`[href="#${pageName}"]`);

        if (targetPage) {
            targetPage.classList.add('active');
        }
        if (targetNav) {
            targetNav.classList.add('active');
        }

        this.currentPage = pageName;
        this.loadPageData(pageName);
    }

    clearModals() {
        const modalContainer = document.getElementById('modal-container');
        if (modalContainer) {
            modalContainer.innerHTML = '';
        }
    }

    async loadPageData(pageName) {
        console.log('加载页面数据:', pageName);

        try {
            this.clearModals();

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
            const urlParams = new URLSearchParams(window.location.search);
            const categoryId = urlParams.get('categoryId');

            if (window.documentManager) {
                if (!window.documentManager.isInitialized) {
                    console.log('文档管理器未初始化，正在初始化...');
                    await window.documentManager.initialize();
                }

                if (categoryId) {
                    await window.documentManager.loadDocuments(categoryId);
                } else {
                    await window.documentManager.loadDocuments();
                }
            } else {
                console.error('文档管理器未加载');
                setTimeout(() => {
                    if (window.documentManager) {
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
        await this.loadCategoriesForFilter();
        await this.loadTagsForCache();
    }

    async loadCategoriesForFilter() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载用户分类，用户ID:', userId);

            const response = await axios.get('/api/category/user/' + userId);
            console.log('分类响应:', response.data);

            if (response.data.success) {
                this.categories = response.data.data || [];

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
        }
    }

    async loadTagsForCache() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载标签缓存，用户ID:', userId);

            const response = await axios.get(`/api/tag/user/${userId}`);
            if (response.data.success) {
                const tags = response.data.data || [];
                this.tagsCache.clear();
                tags.forEach(tag => {
                    if (tag.id && tag.name) {
                        this.tagsCache.set(tag.id, tag.name);
                    }
                });
                console.log('标签缓存建立完成:', this.tagsCache.size, '条记录');
            }
        } catch (error) {
            console.error('加载标签缓存失败:', error);
        }
    }

    getCategoryDisplay(categoryInfo) {
        if (!categoryInfo && categoryInfo !== 0) return '未分类';

        if (typeof categoryInfo === 'string') {
            return categoryInfo;
        }

        if (typeof categoryInfo === 'number' || /^\d+$/.test(String(categoryInfo))) {
            const categoryId = Number(categoryInfo);
            const cachedName = this.categoryCache.get(categoryId);
            return cachedName || `分类${categoryId}`;
        }

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

    getTagDisplay(tagInfo) {
        if (!tagInfo) return '无标签';

        if (typeof tagInfo === 'string') {
            return tagInfo;
        }

        if (typeof tagInfo === 'number' || /^\d+$/.test(String(tagInfo))) {
            const tagId = Number(tagInfo);
            const cachedName = this.tagsCache.get(tagId);
            return cachedName || `标签${tagId}`;
        }

        if (typeof tagInfo === 'object') {
            if (tagInfo.name) {
                return tagInfo.name;
            }
            if (tagInfo.id) {
                const cachedName = this.tagsCache.get(tagInfo.id);
                return cachedName || `标签${tagInfo.id}`;
            }
        }

        return '无标签';
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
        // 全局搜索 - 只保留Enter键搜索
        const globalSearch = document.getElementById('global-search');
        if (globalSearch) {
            globalSearch.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }

        // 标签筛选支持多选
        const searchTag = document.getElementById('search-tag');
        if (searchTag) {
            searchTag.addEventListener('change', () => {
                console.log('标签筛选变化:', this.getSelectedTagIds());
                this.performSearch();
            });
        }

        const searchCategory = document.getElementById('search-category');
        if (searchCategory) {
            searchCategory.addEventListener('change', () => {
                console.log('分类筛选变化:', searchCategory.value);
                this.performSearch();
            });
        }

        const searchSort = document.getElementById('search-sort');
        if (searchSort) {
            searchSort.addEventListener('change', () => {
                console.log('排序方式变化:', searchSort.value);
                this.performSearch();
            });
        }
    }

    getSelectedTagIds() {
        const searchTag = document.getElementById('search-tag');
        if (!searchTag) return [];

        if (searchTag.multiple) {
            return Array.from(searchTag.selectedOptions)
                .map(option => option.value)
                .filter(value => value !== '')
                .map(id => parseInt(id));
        } else {
            return searchTag.value ? [parseInt(searchTag.value)] : [];
        }
    }

    toggleAdvancedFilters() {
        console.log('切换高级筛选器');

        const filterContent = document.getElementById('filter-content');
        const toggleIcon = document.querySelector('.filter-header .toggle-icon');

        if (!filterContent || !toggleIcon) {
            console.error('筛选元素未找到');
            return;
        }

        const isHidden = filterContent.style.display === 'none' ||
            filterContent.style.display === '';

        filterContent.style.display = isHidden ? 'block' : 'none';
        toggleIcon.textContent = isHidden ? '▲' : '▼';

        if (isHidden) {
            this.loadFiltersIfNeeded();
        }
    }

    async loadFiltersIfNeeded() {
        console.log('加载筛选器数据');

        try {
            const categorySelect = document.getElementById('search-category');
            if (categorySelect && categorySelect.options.length <= 1) {
                console.log('加载分类选项...');
                await this.loadCategoriesForFilter();
            }

            const tagSelect = document.getElementById('search-tag');
            if (tagSelect && tagSelect.options.length <= 1) {
                console.log('加载标签选项...');
                await this.loadTagsForSearch();
            }
        } catch (error) {
            console.error('加载筛选器数据失败:', error);
        }
    }

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

    // 🎯 核心搜索方法 - 修复：正确传递多标签参数
    async performSearch() {
        console.log('=== 🔍 开始执行搜索 ===');

        const globalSearch = document.getElementById('global-search');
        const searchCategory = document.getElementById('search-category');
        const searchTag = document.getElementById('search-tag');
        const searchSort = document.getElementById('search-sort');

        console.log('📋 页面筛选器状态:');
        console.log('  - 关键词:', globalSearch ? globalSearch.value : '未找到');
        console.log('  - 分类ID:', searchCategory ? `${searchCategory.value} (${searchCategory.options[searchCategory.selectedIndex]?.text})` : '未找到');
        console.log('  - 标签ID:', searchTag ? this.getSelectedTagIds() : '未找到');
        console.log('  - 排序方式:', searchSort ? searchSort.value : '未找到');

        if (!globalSearch) {
            console.error('搜索输入框未找到');
            return;
        }

        const keyword = globalSearch.value.trim();

        if (!keyword) {
            this.showError('请输入搜索关键词');
            return;
        }

        this.saveToSearchHistory(keyword);

        try {
            const userId = authManager.getCurrentUserId();
            console.log('👤 用户ID:', userId);

            this.showSearchLoading(true);
            this.showSearchStatusBar(keyword, searchCategory, searchTag);

            console.log('🔍 搜索关键词:', keyword);

            const categoryId = searchCategory && searchCategory.value ? parseInt(searchCategory.value) : null;
            const tagIds = this.getSelectedTagIds();
            const sortBy = searchSort ? searchSort.value : 'relevance';

            console.log('🎯 智能搜索参数:');
            console.log('  - 关键词:', keyword);
            console.log('  - 分类ID:', categoryId);
            console.log('  - 标签IDs:', tagIds);
            console.log('  - 用户ID:', userId);
            console.log('  - 排序方式:', sortBy);
            console.log('  - 限制数:', 50);

            console.log('📡 发送API请求到: /api/search/smart');

            // 🎯 修复关键：正确构建GET请求的数组参数
            const params = new URLSearchParams();
            params.append('keyword', keyword);
            if (categoryId) params.append('categoryId', categoryId);
            if (tagIds && tagIds.length > 0) {
                // 🎯 重要：对于GET请求中的List参数，需要多次append同一个参数名
                tagIds.forEach(tagId => {
                    params.append('tagIds', tagId);
                });
            }
            params.append('userId', userId);
            params.append('limit', 50);
            params.append('sortBy', sortBy);

            console.log('📤 请求参数（URL编码）:', params.toString());

            const response = await axios.get('/api/search/smart', {
                params: params,
                // 🎯 设置正确的参数序列化器
                paramsSerializer: function(params) {
                    return params.toString();
                }
            });

            console.log('📥 收到响应:');
            console.log('  - 状态码:', response.status);
            console.log('  - 成功状态:', response.data.success);
            console.log('  - 消息:', response.data.message);
            console.log('  - 结果数量:', response.data.data?.length || 0);

            if (response.data.success) {
                const results = response.data.data || [];
                console.log(`✅ 搜索成功，返回 ${results.length} 个结果`);

                this.displaySearchResults(results, keyword);

                this.displaySearchHistory();

                this.showExportButton(results.length > 0);
            } else {
                console.warn('⚠️ 搜索返回失败状态:', response.data.message);
                throw new Error(response.data.message || '搜索失败');
            }
        } catch (error) {
            console.error('❌ 搜索请求失败:');
            console.error('  - 错误信息:', error.message);
            if (error.response) {
                console.error('  - 状态码:', error.response.status);
                console.error('  - 响应数据:', error.response.data);
            }

            this.showError('搜索失败: ' + (error.response?.data?.message || error.message));
            this.displaySearchResults([], keyword);
        } finally {
            this.showSearchLoading(false);
            console.log('=== 🔍 搜索结束 ===');
        }
    }

    // 🎯 备选方案：使用POST请求（如果需要）
    async performSearchPost() {
        const globalSearch = document.getElementById('global-search');
        const keyword = globalSearch.value.trim();

        if (!keyword) {
            this.showError('请输入搜索关键词');
            return;
        }

        const categoryId = document.getElementById('search-category')?.value || null;
        const tagIds = this.getSelectedTagIds();
        const sortBy = document.getElementById('search-sort')?.value || 'relevance';
        const userId = authManager.getCurrentUserId();

        try {
            this.showSearchLoading(true);

            // 使用POST请求，可以更自然地传递数组
            const response = await axios.post('/api/search/advanced', {
                keyword: keyword,
                categoryId: categoryId,
                tagIds: tagIds.length > 0 ? tagIds : null,
                sortBy: sortBy,
                limit: 50
            }, {
                params: { userId: userId }
            });

            if (response.data.success) {
                const results = response.data.data || [];
                this.displaySearchResults(results, keyword);
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('POST搜索失败:', error);
            this.showError('搜索失败: ' + error.message);
        } finally {
            this.showSearchLoading(false);
        }
    }

    showSearchStatusBar(keyword, searchCategory, searchTag) {
        const searchStatus = document.getElementById('search-status');
        const searchKeyword = document.getElementById('search-keyword');
        const filterConditions = document.getElementById('filter-conditions');

        if (searchStatus) {
            searchStatus.style.display = 'block';
        }

        if (searchKeyword) {
            searchKeyword.textContent = keyword;
        }

        if (filterConditions) {
            const conditions = [];

            if (searchCategory && searchCategory.value) {
                const categoryName = searchCategory.options[searchCategory.selectedIndex].text;
                conditions.push(`分类: ${categoryName}`);
            }

            const selectedTagIds = this.getSelectedTagIds();
            if (selectedTagIds.length > 0) {
                const tagNames = selectedTagIds.map(tagId => {
                    const tagName = this.tagsCache.get(tagId) || `标签${tagId}`;
                    return tagName;
                }).join(', ');
                conditions.push(`标签: ${tagNames}`);
            }

            filterConditions.textContent = conditions.length > 0 ? conditions.join(' | ') : '无';
        }
    }

    showExportButton(show) {
        const exportBtn = document.getElementById('export-btn');
        if (exportBtn) {
            exportBtn.style.display = show ? 'inline-block' : 'none';
        }
    }

    renderSearchResultItem(result, keyword) {
        if (!result || !result.id) {
            console.warn('无效的搜索结果数据:', result);
            return '';
        }

        const docId = result.id;
        const title = result.title || '无标题';
        const contentPreview = result.contentPreview || '无内容预览';
        const categoryId = result.categoryId;
        const categoryName = result.categoryName || '未分类';
        const tags = result.tags || [];
        const updateTime = result.updatedTime || result.createdTime;
        const relevanceScore = result.relevanceScore || 0;

        const highlightedTitle = this.highlightText(title, keyword);
        const highlightedPreview = this.highlightText(contentPreview, keyword);

        const tagsDisplay = tags.length > 0
            ? tags.map(tag => `<span class="tag-badge">${tag.name}</span>`).join('')
            : '<span class="no-tag">无标签</span>';

        const formattedTime = updateTime
            ? new Date(updateTime).toLocaleString('zh-CN')
            : '未知';

        const relevanceDisplay = relevanceScore > 0
            ? `<span class="relevance-score" title="相关性评分">${(relevanceScore * 100).toFixed(1)}%</span>`
            : '';

        return `
        <div class="search-result-item" data-doc-id="${docId}">
            <div class="result-header">
                <h4 class="result-title">
                    ${highlightedTitle}
                    ${relevanceDisplay}
                </h4>
                <div class="result-actions">
                    <button onclick="app.viewSearchDocument(${docId})" class="btn-small" title="查看">👁️</button>
                    <button onclick="app.editSearchDocument(${docId})" class="btn-small" title="编辑">✏️</button>
                </div>
            </div>
            <div class="result-content">
                <p class="doc-preview">${highlightedPreview}</p>
            </div>
            <div class="result-meta">
                <span class="meta-item">
                    <strong>分类:</strong> ${this.escapeHtml(categoryName)}
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

    showSearchLoading(loading) {
        const searchButton = document.querySelector('.search-hero .btn-primary');
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

    displaySearchResults(results, keyword) {
        const resultsContainer = document.getElementById('search-results');
        if (!resultsContainer) {
            console.error('搜索结果容器未找到');
            return;
        }

        if (!Array.isArray(results)) {
            console.error('返回数据不是数组:', results);
            results = [];
        }

        const resultsCount = document.getElementById('search-results-count');
        if (resultsCount) {
            resultsCount.textContent = `${results.length} 个结果`;
        }

        if (results.length === 0) {
            resultsContainer.innerHTML = this.renderNoResults(keyword);
            return;
        }

        resultsContainer.innerHTML = `
            <div class="search-results-list">
                ${results.map(result => this.renderSearchResultItem(result, keyword)).join('')}
            </div>
        `;
    }

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
                        <li>尝试调整排序方式</li>
                    </ul>
                </div>
            </div>
        `;
    }

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

    highlightText(text, keyword) {
        if (!text || !keyword) return this.escapeHtml(text || '');

        const escapedKeyword = this.escapeRegex(keyword);
        const regex = new RegExp(`(${escapedKeyword})`, 'gi');
        return this.escapeHtml(text).replace(regex, '<mark>$1</mark>');
    }

    escapeRegex(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    saveToSearchHistory(keyword) {
        if (!keyword || keyword.trim() === '') return;

        const trimmedKeyword = keyword.trim();

        this.searchHistory = this.searchHistory.filter(item => item !== trimmedKeyword);
        this.searchHistory.unshift(trimmedKeyword);
        this.searchHistory = this.searchHistory.slice(0, 10);
        localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));

        this.updateSearchStats();
    }

    clearSearchFilters() {
        const searchCategory = document.getElementById('search-category');
        const searchTag = document.getElementById('search-tag');
        const searchSort = document.getElementById('search-sort');
        const searchStatus = document.getElementById('search-status');

        if (searchCategory) searchCategory.value = '';
        if (searchTag) {
            if (searchTag.multiple) {
                Array.from(searchTag.options).forEach(option => option.selected = false);
            } else {
                searchTag.value = '';
            }
        }
        if (searchSort) searchSort.value = 'relevance';

        if (searchStatus) {
            searchStatus.style.display = 'none';
        }

        const globalSearch = document.getElementById('global-search');
        if (globalSearch && globalSearch.value.trim()) {
            this.performSearch();
        }
    }

    async setupSearchPage() {
        console.log('设置搜索页面');

        try {
            const filterContent = document.getElementById('filter-content');
            const toggleIcon = document.querySelector('.filter-header .toggle-icon');

            if (filterContent && toggleIcon) {
                filterContent.style.display = 'block';
                toggleIcon.textContent = '▲';
                console.log('高级筛选器已展开');
            }

            await Promise.all([
                this.loadCategoriesForFilter(),
                this.loadTagsForSearch()
            ]);

            this.displaySearchHistory();
            this.updateSearchStats();

        } catch (error) {
            console.error('设置搜索页面失败:', error);
            this.showError('初始化搜索页面失败: ' + error.message);
        }
    }

    async loadTagsForSearch() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载搜索标签，用户ID:', userId);

            const response = await axios.get(`/api/tag/user/${userId}`);
            console.log('标签响应:', response.data);

            if (response.data.success) {
                const tags = response.data.data || [];
                console.log(`获取到 ${tags.length} 个标签`);
                this.updateTagFilter(tags);
            } else {
                console.error('加载标签失败:', response.data.message);
            }
        } catch (error) {
            console.error('加载标签失败:', error);
        }
    }

    updateTagFilter(tags) {
        const tagSelect = document.getElementById('search-tag');
        if (tagSelect) {
            tagSelect.innerHTML = '<option value="">全部标签</option>';

            tagSelect.multiple = true;
            tagSelect.style.height = '100px';

            if (tags && tags.length > 0) {
                tags.forEach(tag => {
                    const option = document.createElement('option');
                    option.value = tag.id;
                    option.textContent = tag.name;
                    option.dataset.tagName = tag.name;
                    tagSelect.appendChild(option);
                });

                const hint = document.createElement('div');
                hint.className = 'select-hint';
                hint.textContent = '按住 Ctrl/Cmd 键可多选';
                hint.style.fontSize = '12px';
                hint.style.color = '#666';
                hint.style.marginTop = '5px';

                if (!document.querySelector('.select-hint')) {
                    tagSelect.parentNode.appendChild(hint);
                }

                console.log(`更新标签筛选器，添加 ${tags.length} 个标签选项（支持多选）`);
            } else {
                console.warn('没有标签数据可供筛选');
            }
        } else {
            console.error('标签筛选器元素未找到');
        }
    }

    updateSearchStats() {
        const historyCount = document.getElementById('history-count');
        const historyCountBadge = document.getElementById('history-count-badge');

        if (historyCount) {
            historyCount.textContent = this.searchHistory.length;
        }
        if (historyCountBadge) {
            historyCountBadge.textContent = this.searchHistory.length;
        }

        const recentSearch = document.getElementById('recent-search');
        if (recentSearch && this.searchHistory.length > 0) {
            recentSearch.textContent = this.searchHistory[0];
        }
    }

    displaySearchHistory() {
        const historyContainer = document.getElementById('search-history');
        if (!historyContainer) {
            console.error('搜索历史容器未找到');
            return;
        }

        if (!this.searchHistory || this.searchHistory.length === 0) {
            historyContainer.innerHTML = `
                <div class="no-history">
                    <span>暂无搜索历史</span>
                </div>
            `;
            return;
        }

        historyContainer.innerHTML = this.searchHistory.map(item => `
            <div class="history-item">
                <span class="history-text" onclick="event.stopPropagation(); app.useHistoryItem('${this.escapeHtml(item)}')">
                    ${this.escapeHtml(item)}
                </span>
                <button onclick="event.stopPropagation(); app.removeHistoryItem('${this.escapeHtml(item)}')" 
                        class="btn-remove" title="删除此项">×</button>
            </div>
        `).join('');
    }

    removeHistoryItem(item) {
        this.searchHistory = this.searchHistory.filter(history => history !== item);
        localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
        this.displaySearchHistory();
        this.updateSearchStats();
    }

    useHistoryItem(item) {
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
        this.updateSearchStats();
    }

    exportSearchResults() {
        this.showSuccess('导出功能正在开发中...');
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

    if (event.error && event.error.message &&
        (event.error.message.includes('未加载') ||
            event.error.message.includes('未初始化'))) {
        event.preventDefault();
        console.log('已处理已知错误:', event.error.message);
    }
});

window.addEventListener('unhandledrejection', function(event) {
    console.error('未处理的Promise错误:', event.reason);
    event.preventDefault();
});

// 应用初始化
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM加载完成，初始化主应用');
    window.app = new KnowledgeBaseApp();
});
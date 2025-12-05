// 分类管理功能 - 彻底修复统计问题版本
class CategoryManager {
    constructor() {
        this.categories = [];
        this.categoryTree = [];
        this.isInitialized = false;
        this.userId = null;
        this.currentPage = 1;
        this.pageSize = 20;
        this.searchKeyword = '';
        this.sortBy = 'name';
        this.isLoading = false;
    }

    // 初始化分类管理器
    async initialize() {
        try {
            console.log('🚀 初始化分类管理器...');

            // 等待认证完成
            await authManager.checkAuthStatus();
            if (!authManager.isAuthenticated()) {
                console.warn('用户未登录，无法加载分类');
                this.showLoginPrompt();
                return;
            }

            this.userId = authManager.getCurrentUserId();
            console.log('当前用户ID:', this.userId);

            // 先加载统计信息（最优先）
            await this.loadCategoryStats();

            // 然后加载分类树和列表
            await Promise.all([
                this.loadCategoryTree(),
                this.loadCategories()
            ]);

            this.setupEventListeners();
            this.isInitialized = true;

            console.log('✅ 分类管理器初始化完成');

        } catch (error) {

            console.error('分类管理器初始化失败:', error);

        }
    }

    // ==================== 核心方法 ====================

    // 设置事件监听器
    setupEventListeners() {
        // 分类搜索防抖
        const categorySearchInput = document.getElementById('category-search');
        if (categorySearchInput) {
            let searchTimer;
            categorySearchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimer);

                // 显示搜索中状态
                const searchIcon = categorySearchInput.previousElementSibling;
                if (searchIcon) {
                    searchIcon.textContent = '⏳';
                }

                searchTimer = setTimeout(() => {
                    this.searchKeyword = e.target.value;
                    this.currentPage = 1;
                    this.applyFiltersAndSort();
                    this.renderCategoryTree();

                    // 恢复搜索图标
                    if (searchIcon) {
                        searchIcon.textContent = '🔍';
                    }
                }, 300);
            });
        }

        // 分类排序
        const categorySortSelect = document.getElementById('category-sort');
        if (categorySortSelect) {
            categorySortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.currentPage = 1;
                this.applyFiltersAndSort();
                this.renderCategoryTree();

                // 添加视觉反馈
                categorySortSelect.style.boxShadow = '0 0 0 3px rgba(59, 130, 246, 0.2)';
                setTimeout(() => {
                    categorySortSelect.style.boxShadow = '';
                }, 300);
            });
        }
    }

    // ==================== 统计功能 - 关键修复 ====================

    // 加载分类统计信息 - 修复版
    async loadCategoryStats() {
        try {
            if (!this.userId) {
                console.error('❌ 用户ID未设置，无法加载统计信息');
                return;
            }

            console.log('📊 正在调用统计API...');
            console.log('API地址:', `/api/category/stats/${this.userId}`);

            const response = await axios.get(`/api/category/stats/${this.userId}`);
            console.log('📦 API响应完整数据:', JSON.stringify(response.data, null, 2));

            if (response.data.success && response.data.data) {
                const stats = response.data.data;
                console.log('📈 解析到的统计数据:', {
                    totalCategories: stats.totalCategories,
                    rootCategories: stats.rootCategories,
                    maxDepth: stats.maxDepth
                });

                // 直接更新DOM元素
                this.updateStatisticsDisplay(stats);
            } else {
                console.warn('⚠️ 统计API返回异常:', response.data);
            }
        } catch (error) {
            console.error('❌ 加载分类统计信息失败:', error);
            if (error.response) {
                console.error('响应状态:', error.response.status);
                console.error('响应数据:', error.response.data);
            }
        }
    }

    // 更新统计显示 - 修复版
    updateStatisticsDisplay(stats) {
        // 确保找到正确的DOM元素
        const totalElement = document.getElementById('categories-total');
        const rootElement = document.getElementById('categories-root');
        const depthElement = document.getElementById('categories-depth');

        console.log('🔍 查找DOM元素结果:', {
            totalElement: !!totalElement,
            rootElement: !!rootElement,
            depthElement: !!depthElement
        });

        if (totalElement) {
            const value = stats.totalCategories || 0;
            console.log('📝 更新分类总数:', value);
            totalElement.textContent = value;

            // 添加更新动画
            totalElement.style.animation = 'none';
            setTimeout(() => {
                totalElement.style.animation = 'pulse 0.5s';
            }, 10);
        } else {
            console.error('❌ 找不到元素: categories-total');
            // 尝试其他可能的选择器
            const altTotalElement = document.querySelector('.categories-stat-item:first-child .categories-stat-number');
            if (altTotalElement) {
                console.log('🔍 使用备选选择器找到元素');
                altTotalElement.textContent = stats.totalCategories || 0;
            }
        }

        if (rootElement) {
            const value = stats.rootCategories || 0;
            console.log('📝 更新根分类数:', value);
            rootElement.textContent = value;
            rootElement.style.animation = 'none';
            setTimeout(() => {
                rootElement.style.animation = 'pulse 0.5s';
            }, 10);
        } else {
            console.error('❌ 找不到元素: categories-root');
        }

        if (depthElement) {
            const value = stats.maxDepth || 0;
            console.log('📝 更新最大深度:', value);
            depthElement.textContent = value;
            depthElement.style.animation = 'none';
            setTimeout(() => {
                depthElement.style.animation = 'pulse 0.5s';
            }, 10);
        } else {
            console.error('❌ 找不到元素: categories-depth');
        }

        console.log('✅ 统计信息更新完成');
    }

    // ==================== 分类管理功能 ====================

    // 加载分类树
    async loadCategoryTree() {
        try {
            if (!this.userId) {
                console.error('用户ID未设置，无法加载分类树');
                return;
            }

            // 显示加载状态
            this.showLoadingState(true);

            const response = await axios.get(`/api/category/user/${this.userId}/tree`);

            if (response.data.success) {
                this.categoryTree = response.data.data || [];
                this.categories = this.flattenCategoryTree(this.categoryTree);
                this.totalCategories = this.categories.length;

                console.log('✅ 分类树加载完成:', this.categoryTree.length);

                // 应用搜索和排序
                this.applyFiltersAndSort();
                this.renderCategoryTree();
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('❌ 加载分类树失败:', error);
            this.showMessage('加载分类树失败: ' + error.message, 'error');
            this.showEmptyState();
        } finally {
            this.showLoadingState(false);
        }
    }

    // 加载分类列表（扁平结构）
    async loadCategories() {
        try {
            if (!this.userId) {
                console.error('用户ID未设置，无法加载分类列表');
                return;
            }

            const response = await axios.get(`/api/category/user/${this.userId}`);

            if (response.data.success) {
                this.categories = response.data.data || [];
                this.totalCategories = this.categories.length;
                console.log('✅ 分类列表加载完成:', this.categories.length);
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('❌ 加载分类列表失败:', error);
            this.categories = [];
        }
    }

    // 展平分类树
    flattenCategoryTree(tree, result = [], level = 0) {
        if (!tree || !Array.isArray(tree)) return result;

        tree.forEach(category => {
            result.push({
                ...category,
                level: level,
                hasChildren: category.children && category.children.length > 0
            });

            if (category.children && category.children.length > 0) {
                this.flattenCategoryTree(category.children, result, level + 1);
            }
        });
        return result;
    }

    // 应用搜索和排序
    applyFiltersAndSort() {
        if (!this.categories || !Array.isArray(this.categories)) {
            this.filteredCategoriesForRender = [];
            return;
        }

        let filteredCategories = [...this.categories];

        // 搜索过滤
        if (this.searchKeyword) {
            filteredCategories = filteredCategories.filter(category =>
                category.name && category.name.toLowerCase().includes(this.searchKeyword.toLowerCase())
            );
        }

        // 排序
        switch (this.sortBy) {
            case 'name':
                filteredCategories.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                break;
            case 'count':
                filteredCategories.sort((a, b) => (b.documentCount || 0) - (a.documentCount || 0));
                break;
            case 'created':
                filteredCategories.sort((a, b) => new Date(b.createdTime || 0) - new Date(a.createdTime || 0));
                break;
            case 'updated':
                filteredCategories.sort((a, b) => new Date(b.updatedTime || b.createdTime || 0) - new Date(a.updatedTime || a.createdTime || 0));
                break;
        }

        this.filteredCategoriesForRender = filteredCategories;
    }

    // 渲染分类树
    renderCategoryTree() {
        const container = document.getElementById('category-tree');
        if (!container) return;

        // 使用过滤后的分类重建树结构
        const filteredTree = this.rebuildTree(this.filteredCategoriesForRender || this.categories);

        if (!filteredTree || filteredTree.length === 0) {
            container.innerHTML = this.renderEmptyState();
            return;
        }

        // 渲染树结构
        container.innerHTML = this.renderTreeStructure(filteredTree);

        // 添加动画效果
        this.addTreeAnimations();
    }

    // 重建树结构
    rebuildTree(categories) {
        if (!categories || !Array.isArray(categories)) return [];

        const map = {};
        const roots = [];

        // 创建映射
        categories.forEach(category => {
            if (category && category.id) {
                map[category.id] = { ...category, children: [] };
            }
        });

        // 构建树结构
        categories.forEach(category => {
            if (!category || !category.id) return;

            const node = map[category.id];
            if (!node) return;

            if (category.parentId && map[category.parentId]) {
                map[category.parentId].children.push(node);
            } else {
                roots.push(node);
            }
        });

        return roots;
    }

    // 渲染树结构
    renderTreeStructure(categories, level = 0) {
        if (!categories || !Array.isArray(categories)) return '';

        return categories.map(category => {
            if (!category) return '';

            const documentCount = category.documentCount || 0;
            const hasChildren = category.children && category.children.length > 0;
            const indent = level * 20;

            return `
                <div class="category-item" data-category-id="${category.id}" style="margin-left: ${indent}px;">
                    <div class="category-content ${hasChildren ? 'has-children' : ''}">
                        <div class="category-info">
                            <div class="category-toggle" onclick="categoryManager.toggleCategory(${category.id})">
                                ${hasChildren ? '▼' : '•'}
                            </div>
                            <div class="category-details">
                                <span class="category-name">${this.escapeHtml(category.name || '未命名')}</span>
                                <div class="category-meta">
                                    ${documentCount ? `<span class="doc-count">📄 ${documentCount}</span>` : ''}
                                    ${category.createdTime ? `<span class="created-time">📅 ${this.getTimeAgo(new Date(category.createdTime))}</span>` : ''}
                                </div>
                            </div>
                        </div>
                        <div class="category-actions">
                            <button onclick="categoryManager.addSubCategory(${category.id})" 
                                    class="category-btn category-btn-add" 
                                    title="添加子分类">
                                +
                            </button>
                            <button onclick="categoryManager.editCategory(${category.id})" 
                                    class="category-btn category-btn-edit" 
                                    title="编辑分类">
                                ✏️
                            </button>
                            <button onclick="categoryManager.deleteCategory(${category.id})" 
                                    class="category-btn category-btn-delete" 
                                    title="删除分类">
                                🗑️
                            </button>
                        </div>
                    </div>
                    <div class="category-children" id="children-${category.id}" 
                         style="display: ${hasChildren ? 'block' : 'none'};">
                        ${hasChildren ? this.renderTreeStructure(category.children, level + 1) : ''}
                    </div>
                </div>
            `;
        }).join('');
    }

    // 渲染空状态
    renderEmptyState() {
        return `
            <div class="categories-empty">
                <div class="empty-icon">📂</div>
                <p>${this.searchKeyword ? '没有找到相关分类' : '暂无分类'}</p>
                <p class="empty-hint">${this.searchKeyword ? '尝试其他搜索关键词' : '创建第一个分类来组织您的知识'}</p>
                <button onclick="showCreateCategoryModal()" class="btn-primary" style="margin-top: 16px;">
                    新建分类
                </button>
            </div>
        `;
    }

    // 添加树动画效果
    addTreeAnimations() {
        setTimeout(() => {
            const categoryItems = document.querySelectorAll('.category-item');
            categoryItems.forEach((item, index) => {
                item.style.animation = 'fadeIn 0.3s ease forwards';
                item.style.animationDelay = `${index * 50}ms`;
                item.style.opacity = '0';
            });
        }, 0);
    }

    // 切换分类展开/折叠
    toggleCategory(categoryId) {
        const childrenDiv = document.getElementById(`children-${categoryId}`);
        const toggleBtn = document.querySelector(`[data-category-id="${categoryId}"] .category-toggle`);

        if (childrenDiv) {
            if (childrenDiv.style.display === 'none') {
                childrenDiv.style.display = 'block';
                toggleBtn.textContent = '▼';
                childrenDiv.style.animation = 'slideDown 0.3s ease';
            } else {
                childrenDiv.style.display = 'none';
                toggleBtn.textContent = '▶';
            }
        }
    }

    // ==================== CRUD 操作 ====================

    // 创建分类
    async createCategory(categoryData) {
        try {
            if (!this.userId) {
                this.showMessage('请先登录系统', 'warning');
                return false;
            }

            categoryData.userId = this.userId;
            console.log('📝 创建分类请求数据:', categoryData);

            const response = await axios.post('/api/category', categoryData);
            console.log('✅ 创建分类响应:', response.data);

            if (response.data.success) {
                this.showMessage('分类创建成功', 'success');
                // 重新加载数据和统计
                await this.loadCategoryTree();
                await this.loadCategoryStats();  // 重新加载统计
                return true;
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('❌ 创建分类失败:', error);
            const errorMsg = error.response?.data?.message || error.message;

            if (errorMsg.includes('已存在') || errorMsg.includes('exists')) {
                this.showMessage('分类名称已存在，请使用其他名称', 'error');
            } else {
                this.showMessage('创建分类失败: ' + errorMsg, 'error');
            }
            return false;
        }
    }

    // 更新分类
    async updateCategory(categoryId, categoryData) {
        try {
            if (!this.userId) {
                this.showMessage('请先登录系统', 'warning');
                return false;
            }

            const updateData = {
                id: categoryId,
                name: categoryData.name,
                parentId: categoryData.parentId,
                userId: this.userId
            };

            console.log('📝 更新分类请求数据:', updateData);

            const response = await axios.put(`/api/category/${categoryId}`, updateData);
            console.log('✅ 更新分类响应:', response.data);

            if (response.data.success) {
                this.showMessage('分类更新成功', 'success');
                // 重新加载数据和统计
                await this.loadCategoryTree();
                await this.loadCategoryStats();  // 重新加载统计
                return true;
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('❌ 更新分类失败:', error);
            const errorMsg = error.response?.data?.message || error.message;
            this.showMessage('更新分类失败: ' + errorMsg, 'error');
            return false;
        }
    }

    // 删除分类
    async deleteCategory(categoryId) {
        const category = this.categories.find(c => c.id === categoryId);
        if (!category) return;

        const documentCount = category.documentCount || 0;
        let confirmMessage = `确定要删除分类 "${category.name}" 吗？`;

        if (documentCount > 0) {
            confirmMessage += `\n\n该分类下有 ${documentCount} 个文档，删除前需要先处理这些文档。`;
        }

        const hasChildren = this.categoryTree.some(cat => cat.id === categoryId && cat.children?.length > 0);
        if (hasChildren) {
            confirmMessage += `\n\n该分类包含子分类，请先删除或移动所有子分类。`;
        }

        const confirmDelete = confirm(confirmMessage);
        if (!confirmDelete) return;

        try {
            const response = await axios.delete(`/api/category/${categoryId}`, {
                params: { userId: this.userId }
            });

            if (response.data.success) {
                this.showMessage('分类删除成功', 'success');
                // 重新加载数据和统计
                await this.loadCategoryTree();
                await this.loadCategoryStats();  // 重新加载统计
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('❌ 删除分类失败:', error);
            const errorMsg = error.response?.data?.message || error.message;

            if (errorMsg.includes('存在子分类') || errorMsg.includes('children')) {
                this.showMessage('删除失败：请先删除或移动所有子分类', 'error');
            } else if (errorMsg.includes('存在文档')) {
                this.showMessage('删除失败：请先移除或转移分类下的文档', 'error');
            } else {
                this.showMessage('删除分类失败: ' + errorMsg, 'error');
            }
        }
    }

    // ==================== 模态框相关方法 ====================

    // 显示创建分类模态框
    showCreateCategoryModal(parentId = null) {
        if (!authManager.isAuthenticated()) {
            this.showMessage('请先登录系统', 'warning');
            return;
        }

        const modalHtml = `
            <div class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>📂 新建分类</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="create-category-form" class="category-form">
                            <div class="form-group">
                                <label for="category-name">分类名称 *</label>
                                <input type="text" id="category-name" class="form-input" required 
                                       placeholder="请输入分类名称（最多50个字符）" 
                                       maxlength="50"
                                       autocomplete="off">
                                <div class="input-hint">建议使用清晰、有层次结构的名称</div>
                                <div id="category-name-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="category-parent">父分类（可选）</label>
                                <select id="category-parent" class="form-select">
                                    <option value="">无父分类（根分类）</option>
                                    ${this.renderParentCategoryOptions(parentId)}
                                </select>
                                <div class="input-hint">选择父分类可以创建层级结构</div>
                            </div>
                            
                            <div id="category-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">
                                    <span>创建分类</span>
                                </button>
                                <button type="button" class="btn-secondary" onclick="closeModal()">
                                    取消
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('modal-container').innerHTML = modalHtml;
        this.setupCategoryForm('create');

        // 聚焦到输入框
        setTimeout(() => {
            const input = document.getElementById('category-name');
            if (input) input.focus();
        }, 100);
    }

    // 编辑分类
    async editCategory(categoryId) {
        try {
            if (!authManager.isAuthenticated()) {
                this.showMessage('请先登录系统', 'warning');
                return;
            }

            const response = await axios.get(`/api/category/${categoryId}`, {
                params: { userId: this.userId }
            });

            if (response.data.success) {
                this.showEditCategoryModal(response.data.data);
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('获取分类详情失败:', error);
            this.showMessage('获取分类详情失败: ' + error.message, 'error');
        }
    }

    // 显示编辑分类模态框
    showEditCategoryModal(categoryData) {
        const modalHtml = `
            <div class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>✏️ 编辑分类</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="edit-category-form" class="category-form">
                            <input type="hidden" id="category-id" value="${categoryData.id}">
                            
                            <div class="form-group">
                                <label for="category-name">分类名称 *</label>
                                <input type="text" id="category-name" class="form-input" required 
                                       value="${this.escapeHtml(categoryData.name)}" 
                                       placeholder="请输入分类名称" 
                                       maxlength="50">
                                <div class="input-hint">同一层级下不能创建重复名称</div>
                                <div id="category-name-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="category-parent">父分类</label>
                                <select id="category-parent" class="form-select">
                                    <option value="">无父分类（根分类）</option>
                                    ${this.renderParentCategoryOptions(categoryData.parentId, categoryData.id)}
                                </select>
                                <div class="input-hint">不能将分类设置为自己的子分类</div>
                            </div>
                            
                            <div class="form-group">
                                <label>分类信息</label>
                                <div class="category-info-display">
                                    ${categoryData.createdTime ? `<span>创建时间: ${new Date(categoryData.createdTime).toLocaleString()}</span>` : ''}
                                    ${categoryData.documentCount !== undefined ? `<span>文档数量: ${categoryData.documentCount || 0} 个</span>` : ''}
                                    ${categoryData.updatedTime ? `<span>最后更新: ${new Date(categoryData.updatedTime).toLocaleString()}</span>` : ''}
                                </div>
                            </div>
                            
                            <div id="category-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">
                                    <span>更新分类</span>
                                </button>
                                <button type="button" class="btn-secondary" onclick="closeModal()">
                                    取消
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('modal-container').innerHTML = modalHtml;
        this.setupCategoryForm('edit', categoryData);
    }

    // 添加子分类
    addSubCategory(parentId) {
        this.showCreateCategoryModal(parentId);
    }

    // 设置分类表单
    setupCategoryForm(mode, categoryData = null) {
        const form = document.getElementById(`${mode}-category-form`);
        const messageDiv = document.getElementById('category-message');
        const nameInput = document.getElementById('category-name');
        const parentSelect = document.getElementById('category-parent');

        // 清除之前的监听器
        const newForm = form.cloneNode(true);
        form.parentNode.replaceChild(newForm, form);

        // 重新获取表单元素
        const newFormElement = document.getElementById(`${mode}-category-form`);
        const newMessageDiv = document.getElementById('category-message');
        const newNameInput = document.getElementById('category-name');
        const newParentSelect = document.getElementById('category-parent');

        newFormElement.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = {
                name: newNameInput.value.trim(),
                userId: this.userId
            };

            // 处理父分类
            if (newParentSelect && newParentSelect.value) {
                formData.parentId = parseInt(newParentSelect.value);
            }

            // 验证
            if (!formData.name) {
                this.showFormMessage('请输入分类名称', 'error', newMessageDiv);
                return;
            }

            if (formData.name.length > 50) {
                this.showFormMessage('分类名称不能超过50个字符', 'error', newMessageDiv);
                return;
            }

            try {
                let response;

                if (mode === 'create') {
                    response = await axios.post('/api/category', formData);
                } else {
                    // 编辑模式
                    formData.id = categoryData.id;
                    response = await axios.put(`/api/category/${categoryData.id}`, formData);
                }

                if (response.data.success) {
                    this.showFormMessage(
                        mode === 'create' ? '分类创建成功' : '分类更新成功',
                        'success',
                        newMessageDiv
                    );

                    setTimeout(() => {
                        closeModal();
                        // 重新加载数据
                        this.loadCategoryTree();
                        this.loadCategoryStats();  // 重新加载统计
                    }, 1000);
                } else {
                    throw new Error(response.data.message);
                }
            } catch (error) {
                console.error(`${mode === 'create' ? '创建' : '更新'}分类失败:`, error);
                const errorMsg = error.response?.data?.message || error.message;

                if (errorMsg.includes('已存在') || errorMsg.includes('exists')) {
                    this.showFormMessage('分类名称已存在，请使用其他名称', 'error', newMessageDiv);
                } else if (errorMsg.includes('子分类') || errorMsg.includes('children')) {
                    this.showFormMessage('不能将分类设置为自己的子分类', 'error', newMessageDiv);
                } else {
                    this.showFormMessage(
                        `${mode === 'create' ? '创建' : '更新'}分类失败: ${errorMsg}`,
                        'error',
                        newMessageDiv
                    );
                }
            }
        });

        // 如果是编辑模式，填充数据
        if (mode === 'edit' && categoryData) {
            newNameInput.value = categoryData.name;
            if (newParentSelect && categoryData.parentId) {
                newParentSelect.value = categoryData.parentId;
            }
        }

        // 聚焦到输入框
        setTimeout(() => {
            newNameInput.focus();
        }, 100);
    }

    // 渲染父分类选项
    renderParentCategoryOptions(selectedParentId = null, excludeId = null) {
        let options = '';

        const renderOptions = (categories, level = 0) => {
            categories.forEach(category => {
                // 排除自己
                if (category.id === excludeId) return;

                const prefix = '─'.repeat(level) + (level > 0 ? ' ' : '');
                const selected = category.id === selectedParentId ? 'selected' : '';
                const indent = '&nbsp;'.repeat(level * 4);

                options += `<option value="${category.id}" ${selected}>${indent}${prefix} ${this.escapeHtml(category.name)}</option>`;

                if (category.children && category.children.length > 0) {
                    renderOptions(category.children, level + 1);
                }
            });
        };

        renderOptions(this.categoryTree);
        return options;
    }

    // ==================== 批量操作 ====================

    // 处理批量删除
    async handleBatchDelete() {
        const selectedIds = this.getSelectedCategoryIds();
        if (selectedIds.length === 0) {
            this.showMessage('请先选择要删除的分类', 'warning');
            return;
        }

        const confirmDelete = confirm(`确定要删除选中的 ${selectedIds.length} 个分类吗？`);
        if (!confirmDelete) return;

        try {
            const promises = selectedIds.map(id =>
                axios.delete(`/api/category/${id}`, {
                    params: { userId: this.userId }
                })
            );

            const results = await Promise.allSettled(promises);
            const successCount = results.filter(r => r.status === 'fulfilled' && r.value.data.success).length;

            if (successCount > 0) {
                this.showMessage(`成功删除 ${successCount} 个分类`, 'success');
                // 重新加载数据
                await this.loadCategoryTree();
                await this.loadCategoryStats();
            }
        } catch (error) {
            console.error('批量删除失败:', error);
            this.showMessage('批量删除失败: ' + error.message, 'error');
        }
    }

    // 处理批量移动
    async handleBatchMove() {
        const selectedIds = this.getSelectedCategoryIds();
        if (selectedIds.length === 0) {
            this.showMessage('请先选择要移动的分类', 'warning');
            return;
        }

        // 显示移动模态框
        this.showBatchMoveModal(selectedIds);
    }

    // 显示批量移动模态框
    showBatchMoveModal(categoryIds) {
        const modalHtml = `
            <div class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>📁 移动分类</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p>将选中的 ${categoryIds.length} 个分类移动到：</p>
                        <div class="form-group">
                            <label for="target-parent">目标父分类</label>
                            <select id="target-parent" class="form-select">
                                <option value="">根分类</option>
                                ${this.renderParentCategoryOptions()}
                            </select>
                        </div>
                        <div id="move-message" class="message-container"></div>
                        <div class="form-actions">
                            <button id="confirm-move" class="btn-primary">
                                确认移动
                            </button>
                            <button type="button" class="btn-secondary" onclick="closeModal()">
                                取消
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('modal-container').innerHTML = modalHtml;

        // 绑定确认移动事件
        document.getElementById('confirm-move').addEventListener('click', async () => {
            const targetParentId = document.getElementById('target-parent').value || null;
            await this.executeBatchMove(categoryIds, targetParentId);
        });
    }

    // 执行批量移动
    async executeBatchMove(categoryIds, targetParentId) {
        try {
            const promises = categoryIds.map(id =>
                axios.put(`/api/category/${id}/move`, null, {
                    params: {
                        newParentId: targetParentId || '',
                        userId: this.userId
                    }
                })
            );

            const results = await Promise.allSettled(promises);
            const successCount = results.filter(r => r.status === 'fulfilled' && r.value.data.success).length;

            if (successCount > 0) {
                this.showMessage(`成功移动 ${successCount} 个分类`, 'success');
                closeModal();
                // 重新加载数据
                await this.loadCategoryTree();
                await this.loadCategoryStats();
            }
        } catch (error) {
            console.error('批量移动失败:', error);
            this.showFormMessage('批量移动失败: ' + error.message, 'error', document.getElementById('move-message'));
        }
    }

    // 获取选中的分类ID
    getSelectedCategoryIds() {
        const checkboxes = document.querySelectorAll('.category-checkbox:checked');
        return Array.from(checkboxes).map(cb => parseInt(cb.dataset.categoryId));
    }

    // ==================== 工具方法 ====================

    // 获取最大深度
    getMaxDepth(categories, currentDepth = 0) {
        if (!categories || !Array.isArray(categories) || categories.length === 0) return currentDepth;

        let maxDepth = currentDepth;
        categories.forEach(category => {
            if (category.children && category.children.length > 0) {
                const depth = this.getMaxDepth(category.children, currentDepth + 1);
                maxDepth = Math.max(maxDepth, depth);
            }
        });

        return maxDepth;
    }

    // 获取时间间隔描述
    getTimeAgo(date) {
        if (!(date instanceof Date) || isNaN(date)) return '未知时间';

        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);

        if (diffInSeconds < 60) return '刚刚';
        if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}分钟前`;
        if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}小时前`;
        if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}天前`;
        return date.toLocaleDateString();
    }

    // HTML转义
    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // 显示加载状态
    showLoadingState(show) {
        const container = document.getElementById('category-tree');
        if (!container) return;

        if (show) {
            container.innerHTML = `
                <div class="loading-state">
                    <div class="loading-spinner"></div>
                    <p>正在加载分类...</p>
                </div>
            `;
        }
    }

    // 显示空状态
    showEmptyState() {
        const container = document.getElementById('category-tree');
        if (container) {
            container.innerHTML = this.renderEmptyState();
        }
    }

    // 显示登录提示
    showLoginPrompt() {
        const container = document.getElementById('category-tree');
        if (!container) return;

        container.innerHTML = `
            <div class="categories-empty">
                <div class="empty-icon">🔒</div>
                <p>请先登录</p>
                <p class="empty-hint">登录后即可管理您的分类</p>
                <button onclick="window.location.hash = 'login'" class="btn-primary" style="margin-top: 16px;">
                    去登录
                </button>
            </div>
        `;
    }

    // 显示消息
    showMessage(message, type) {
        if (window.ElMessage) {
            const ElMessage = window.ElMessage;
            if (type === 'success') {
                ElMessage.success({
                    message: message,
                    showClose: true,
                    duration: 3000,
                    offset: 80
                });
            } else if (type === 'error') {
                ElMessage.error({
                    message: message,
                    showClose: true,
                    duration: 4000,
                    offset: 80
                });
            } else if (type === 'warning') {
                ElMessage.warning({
                    message: message,
                    showClose: true,
                    duration: 3000,
                    offset: 80
                });
            } else {
                ElMessage.info(message);
            }
        } else {
            alert(message);
        }
    }

    // 显示表单消息
    showFormMessage(message, type, container) {
        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
        container.innerHTML = `
            <div class="message ${type}">
                <span class="message-icon">${icon}</span>
                <div class="message-content">
                    <div class="message-text">${message}</div>
                </div>
                <button class="message-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        `;

        setTimeout(() => {
            const messageEl = container.querySelector('.message');
            if (messageEl) {
                messageEl.style.animation = 'slideInRight 0.3s ease reverse';
                setTimeout(() => messageEl.remove(), 300);
            }
        }, 5000);
    }
}

// ==================== 全局函数 ====================

let categoryManager;

function initCategoryManager() {
    categoryManager = new CategoryManager();
    categoryManager.initialize();
}

function showCreateCategoryModal(parentId = null) {
    if (categoryManager) {
        categoryManager.showCreateCategoryModal(parentId);
    } else {
        console.error('categoryManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

function editCategory(categoryId) {
    if (categoryManager) {
        categoryManager.editCategory(categoryId);
    } else {
        console.error('categoryManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

function deleteCategory(categoryId) {
    if (categoryManager) {
        categoryManager.deleteCategory(categoryId);
    } else {
        console.error('categoryManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

function closeModal() {
    const modal = document.querySelector('.modal');
    if (modal) {
        modal.style.animation = 'slideUp 0.3s ease reverse';
        setTimeout(() => {
            modal.remove();
        }, 300);
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    if (document.getElementById('categories-page')) {
        setTimeout(() => {
            console.log('🔄 初始化分类管理器...');
            initCategoryManager();
        }, 100);
    }
});

// 导出管理器实例
window.categoryManager = categoryManager;
class CategoryManager {
    constructor() {
        this.categories = [];
        this.categoryTree = [];
        this.isInitialized = false;
    }

    async initialize() {
        if (this.isInitialized) return;

        console.log('初始化分类管理器...');

        try {
            // 先检查认证状态
            const isAuthenticated = await authManager.checkAuthStatus();
            if (!isAuthenticated) {
                console.warn('用户未登录，无法加载分类');
                this.showError('请先登录系统');
                authManager.redirectToLogin();
                return;
            }

            // 加载分类树
            await this.loadCategoryTree();
            this.isInitialized = true;

        } catch (error) {
            console.error('分类管理器初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    async loadCategoryTree() {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/category/user/${userId}/tree`);

            if (response.data.success) {
                this.categoryTree = response.data.data || [];
                this.displayCategoryTree();
                console.log('分类树加载完成');
            } else {
                console.error('加载分类树失败:', response.data.message);
                this.showError('加载分类树失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('加载分类树失败:', error);
            this.showError('加载分类树失败: ' + error.message);
        }
    }

    async loadCategories() {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/category/user/${userId}`);

            if (response.data.success) {
                this.categories = response.data.data || [];
                console.log('分类列表加载完成:', this.categories.length);
            }
        } catch (error) {
            console.error('加载分类列表失败:', error);
            this.categories = [];
        }
    }

    displayCategoryTree() {
        const container = document.getElementById('category-tree');
        if (!container) {
            console.error('分类树容器未找到');
            return;
        }

        if (!this.categoryTree || this.categoryTree.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <p>暂无分类</p>
                    <button onclick="showCreateCategoryModal()" class="btn-primary">创建第一个分类</button>
                </div>
            `;
            return;
        }

        container.innerHTML = this.renderCategoryTree(this.categoryTree);
    }

    renderCategoryTree(categories, level = 0) {
        return categories.map(category => `
            <div class="category-item" data-category-id="${category.id}" style="margin-left: ${level * 20}px;">
                <div class="category-content">
                    <div class="category-info">
                        <span class="category-name">${this.escapeHtml(category.name)}</span>
                        ${category.documentCount ? `<span class="doc-count">${category.documentCount}</span>` : ''}
                    </div>
                    <div class="category-actions">
                        <button onclick="categoryManager.addSubCategory(${category.id})" class="btn-small" title="添加子分类">+</button>
                        <button onclick="categoryManager.editCategory(${category.id})" class="btn-small" title="编辑">✏️</button>
                        <button onclick="categoryManager.deleteCategory(${category.id})" class="btn-small btn-danger" title="删除">🗑️</button>
                    </div>
                </div>
                ${category.children && category.children.length > 0 ?
            this.renderCategoryTree(category.children, level + 1) : ''}
            </div>
        `).join('');
    }

    async createCategory(categoryData) {
        try {
            if (!authManager.isAuthenticated()) {
                this.showError('请先登录系统');
                return false;
            }

            const userId = authManager.getCurrentUserId();
            categoryData.userId = userId;

            console.log('创建分类请求数据:', categoryData);

            const response = await axios.post('/api/category', categoryData);

            console.log('创建分类响应:', response.data);

            if (response.data.success) {
                this.showSuccess('分类创建成功');
                await this.loadCategoryTree();
                return true;
            } else {
                this.showError('创建分类失败: ' + response.data.message);
                return false;
            }
        } catch (error) {
            console.error('创建分类失败:', error);
            console.error('错误详情:', error.response?.data);
            this.showError('创建分类失败: ' + (error.response?.data?.message || error.message));
            return false;
        }
    }

    async updateCategory(categoryId, categoryData) {
        try {
            if (!authManager.isAuthenticated()) {
                this.showError('请先登录系统');
                return false;
            }

            const userId = authManager.getCurrentUserId();

            const updateData = {
                id: categoryId,
                name: categoryData.name,
                parentId: categoryData.parentId,
                userId: userId
            };

            console.log('更新分类请求数据:', updateData);

            const response = await axios.put(`/api/category/${categoryId}`, updateData);

            console.log('更新分类响应:', response.data);

            if (response.data.success) {
                this.showSuccess('分类更新成功');
                await this.loadCategoryTree();
                return true;
            } else {
                this.showError('更新分类失败: ' + response.data.message);
                return false;
            }
        } catch (error) {
            console.error('更新分类失败:', error);
            console.error('错误详情:', error.response?.data);
            this.showError('更新分类失败: ' + (error.response?.data?.message || error.message));
            return false;
        }
    }

    async deleteCategory(categoryId) {
        if (!confirm('确定要删除这个分类吗？如果分类下有子分类或文档，将无法删除。')) return;

        try {
            if (!authManager.isAuthenticated()) {
                this.showError('请先登录系统');
                return;
            }

            const userId = authManager.getCurrentUserId();
            const response = await axios.delete(`/api/category/${categoryId}`, {
                params: { userId: userId }
            });

            if (response.data.success) {
                this.showSuccess('分类删除成功');
                await this.loadCategoryTree();
            } else {
                this.showError('删除分类失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('删除分类失败:', error);
            this.showError('删除分类失败: ' + error.message);
        }
    }

    async editCategory(categoryId) {
        try {
            if (!authManager.isAuthenticated()) {
                this.showError('请先登录系统');
                return;
            }

            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/category/${categoryId}`, {
                params: { userId: userId }
            });

            if (response.data.success) {
                const categoryData = response.data.data;
                this.showCategoryModal(categoryData, 'edit');
            } else {
                this.showError('加载分类失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('加载分类失败:', error);
            this.showError('加载分类失败: ' + error.message);
        }
    }

    async addSubCategory(parentId) {
        this.showCategoryModal(null, 'create', parentId);
    }

    showCategoryModal(categoryData, mode = 'create', parentId = null) {
        const modalContainer = document.getElementById('modal-container');
        if (!modalContainer) {
            console.error('模态框容器未找到');
            this.showError('系统错误：无法打开分类编辑');
            return;
        }

        const isEdit = mode === 'edit';
        const modalContent = `
            <div class="modal">
                <div class="modal-content category-modal">
                    <div class="modal-header">
                        <h3>${isEdit ? '编辑分类' : '新建分类'}</h3>
                        <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="category-form" class="category-form">
                            <input type="hidden" id="category-id" value="${isEdit ? categoryData.id : ''}">
                            <div class="form-group">
                                <label for="category-name">分类名称:</label>
                                <input type="text" id="category-name" 
                                       value="${isEdit ? this.escapeHtml(categoryData.name) : ''}" 
                                       required class="form-input" 
                                       placeholder="请输入分类名称">
                            </div>
                            <div class="form-group">
                                <label for="category-parent">父分类:</label>
                                <select id="category-parent" class="form-select">
                                    <option value="">无父分类（根分类）</option>
                                    ${this.renderParentCategoryOptions(categoryData?.parentId || parentId)}
                                </select>
                            </div>
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">${isEdit ? '保存' : '创建'}</button>
                                <button type="button" onclick="this.closest('.modal').remove()" class="btn-secondary">取消</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        modalContainer.innerHTML = modalContent;

        // 绑定表单提交事件
        const form = document.getElementById('category-form');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this.handleCategoryFormSubmit(mode);
            });
        }
    }

    renderParentCategoryOptions(selectedParentId = null) {
        let options = '';

        const renderOptions = (categories, level = 0) => {
            categories.forEach(category => {
                const prefix = '─'.repeat(level) + (level > 0 ? ' ' : '');
                const selected = category.id === selectedParentId ? 'selected' : '';
                options += `<option value="${category.id}" ${selected}>${prefix} ${this.escapeHtml(category.name)}</option>`;

                if (category.children && category.children.length > 0) {
                    renderOptions(category.children, level + 1);
                }
            });
        };

        renderOptions(this.categoryTree);
        return options;
    }

    async handleCategoryFormSubmit(mode) {
        const name = document.getElementById('category-name').value;
        const parentId = document.getElementById('category-parent').value || null;
        const categoryId = document.getElementById('category-id').value;

        const categoryData = {
            name,
            parentId: parentId ? parseInt(parentId) : null
        };

        let success = false;
        if (mode === 'edit') {
            success = await this.updateCategory(categoryId, categoryData);
        } else {
            success = await this.createCategory(categoryData);
        }

        if (success) {
            const modal = document.querySelector('.modal');
            if (modal) {
                modal.remove();
            }
        }
    }

    async getCategoryStats() {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/category/stats/${userId}`);

            if (response.data.success) {
                return response.data.data;
            }
            return null;
        } catch (error) {
            console.error('获取分类统计失败:', error);
            return null;
        }
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showError(message) {
        console.error('分类管理错误:', message);
        alert('错误: ' + message);
    }

    showSuccess(message) {
        console.log('分类管理成功:', message);
        alert('成功: ' + message);
    }
}

// 分类管理器实例
const categoryManager = new CategoryManager();

// 全局函数
function showCreateCategoryModal() {
    if (!authManager.isAuthenticated()) {
        alert('请先登录系统');
        return;
    }

    categoryManager.showCategoryModal();
}

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(() => {
        categoryManager.initialize();
    }, 100);
});
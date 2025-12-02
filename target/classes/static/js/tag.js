// 标签管理功能
class TagManager {
    constructor() {
        this.tags = [];
        this.currentPage = 1;
        this.pageSize = 20;
        this.searchKeyword = '';
        this.sortBy = 'name';
        this.userId = null;
    }

    // 初始化标签管理
    async init() {
        try {
            // 等待认证完成
            await authManager.checkAuthStatus();
            if (!authManager.isAuthenticated()) {
                console.warn('用户未登录，无法加载标签');
                return;
            }

            this.userId = authManager.getCurrentUserId();
            await this.loadTags();
            this.setupEventListeners();
        } catch (error) {
            console.error('标签管理器初始化失败:', error);
        }
    }

    // 设置事件监听器
    setupEventListeners() {
        // 搜索防抖
        let searchTimer;
        const tagSearchInput = document.getElementById('tag-search');
        if (tagSearchInput) {
            tagSearchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimer);
                searchTimer = setTimeout(() => {
                    this.searchKeyword = e.target.value;
                    this.loadTags();
                }, 300);
            });
        }

        // 排序
        const tagSortSelect = document.getElementById('tag-sort');
        if (tagSortSelect) {
            tagSortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.loadTags();
            });
        }
    }

    // 加载标签列表
    async loadTags() {
        try {
            if (!this.userId) {
                console.error('用户ID未设置，无法加载标签');
                return;
            }

            console.log('加载标签，用户ID:', this.userId);
            const response = await axios.get(`/api/tag/user/${this.userId}`);

            console.log('标签响应:', response.data);

            if (response.data.success) {
                this.tags = response.data.data || [];
                console.log('原始标签数据:', this.tags);

                // 应用搜索和排序
                this.applyFiltersAndSort();
                this.renderTags();
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('加载标签失败:', error);
            this.showMessage('加载标签失败: ' + error.message, 'error');
        }
    }

    // 应用搜索和排序
    applyFiltersAndSort() {
        let filteredTags = [...this.tags];

        // 搜索过滤
        if (this.searchKeyword) {
            filteredTags = filteredTags.filter(tag =>
                tag.name.toLowerCase().includes(this.searchKeyword.toLowerCase())
            );
        }

        // 排序
        switch (this.sortBy) {
            case 'name':
                filteredTags.sort((a, b) => a.name.localeCompare(b.name));
                break;
            case 'count':
                filteredTags.sort((a, b) => (b.documentCount || 0) - (a.documentCount || 0));
                break;
            case 'created':
                filteredTags.sort((a, b) => new Date(b.createdTime) - new Date(a.createdTime));
                break;
        }

        // 关键修复：不要覆盖 this.tags，只在渲染时使用过滤后的数据
        this.filteredTagsForRender = filteredTags;
    }

    // 渲染标签列表
    renderTags() {
        const container = document.getElementById('tags-list');
        if (!container) return;

        const tagsToRender = this.filteredTagsForRender || this.tags;

        if (tagsToRender.length === 0) {
            container.innerHTML = `
                <div class="tags-empty">
                    <div class="empty-icon">🏷️</div>
                    <p>${this.searchKeyword ? '没有找到相关标签' : '暂无标签'}</p>
                    <p class="empty-hint">${this.searchKeyword ? '尝试其他搜索关键词' : '创建第一个标签来开始管理您的知识'}</p>
                    <button onclick="showCreateTagModal()" class="btn-primary" style="margin-top: 16px;">
                        新建标签
                    </button>
                </div>
            `;
            return;
        }

        container.innerHTML = tagsToRender.map(tag => {
            // 确保 documentCount 正确显示
            const documentCount = tag.documentCount !== undefined && tag.documentCount !== null ? tag.documentCount : 0;

            return `
                <div class="tag-item" data-tag-id="${tag.id}">
                    <div class="tag-info">
                        <span class="tag-name">${this.escapeHtml(tag.name)}</span>
                    </div>
                    <div class="tag-stats">
                        <div class="tag-stat">
                            <div class="stat-number">${documentCount}</div>
                            <div class="stat-label">文档</div>
                        </div>
                    </div>
                    <div class="tag-actions">
                        <button onclick="viewTagDocuments(${tag.id})" class="btn-small" title="查看文档">👁️</button>
                        <button onclick="editTag(${tag.id})" class="btn-small" title="编辑">✏️</button>
                        <button onclick="deleteTag(${tag.id})" class="btn-small btn-danger" title="删除">🗑️</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    // 显示创建标签模态框
    showCreateTagModal() {
        if (!authManager.isAuthenticated()) {
            alert('请先登录系统');
            return;
        }

        const modalHtml = `
            <div class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>新建标签</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="create-tag-form" class="tag-form">
                            <div class="form-group">
                                <label for="tag-name">标签名称 *</label>
                                <input type="text" id="tag-name" class="form-input" required 
                                       placeholder="请输入标签名称" maxlength="20">
                                <div class="input-hint">最多20个字符，同一用户不能创建重复名称</div>
                                <div id="tag-name-error" class="error-message"></div>
                            </div>
                            
                            <div id="tag-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">创建</button>
                                <button type="button" class="btn-secondary" onclick="closeModal()">取消</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('modal-container').innerHTML = modalHtml;
        this.setupTagForm('create');
    }

    // 设置标签表单
    setupTagForm(mode, tagData = null) {
        const form = document.getElementById(`${mode}-tag-form`);
        const messageDiv = document.getElementById('tag-message');
        const nameInput = document.getElementById('tag-name');

        // 清除之前的监听器
        const newForm = form.cloneNode(true);
        form.parentNode.replaceChild(newForm, form);

        // 重新获取表单元素
        const newFormElement = document.getElementById(`${mode}-tag-form`);
        const newMessageDiv = document.getElementById('tag-message');
        const newNameInput = document.getElementById('tag-name');

        newFormElement.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = {
                name: newNameInput.value.trim(),
                userId: this.userId
            };

            // 验证
            if (!formData.name) {
                this.showFormMessage('请输入标签名称', 'error', newMessageDiv);
                return;
            }

            if (formData.name.length > 20) {
                this.showFormMessage('标签名称不能超过20个字符', 'error', newMessageDiv);
                return;
            }

            try {
                let response;

                if (mode === 'create') {
                    response = await axios.post('/api/tag', formData);
                } else {
                    // 编辑模式
                    formData.id = tagData.id;
                    response = await axios.put(`/api/tag/${tagData.id}`, formData);
                }

                if (response.data.success) {
                    this.showFormMessage(
                        mode === 'create' ? '标签创建成功' : '标签更新成功',
                        'success',
                        newMessageDiv
                    );

                    setTimeout(() => {
                        closeModal();
                        this.loadTags();
                    }, 1000);
                } else {
                    throw new Error(response.data.message);
                }
            } catch (error) {
                console.error(`${mode === 'create' ? '创建' : '更新'}标签失败:`, error);
                const errorMsg = error.response?.data?.message || error.message;

                // 特殊处理重复标签错误
                if (errorMsg.includes('已存在') || errorMsg.includes('exists') || errorMsg.includes('duplicate')) {
                    this.showFormMessage('标签名称已存在，请使用其他名称', 'error', newMessageDiv);
                } else if (errorMsg.includes('无权访问') || errorMsg.includes('permission')) {
                    this.showFormMessage('您没有权限执行此操作', 'error', newMessageDiv);
                } else {
                    this.showFormMessage(
                        `${mode === 'create' ? '创建' : '更新'}标签失败: ${errorMsg}`,
                        'error',
                        newMessageDiv
                    );
                }
            }
        });

        // 如果是编辑模式，填充数据
        if (mode === 'edit' && tagData) {
            newNameInput.value = tagData.name;
        }

        // 聚焦到输入框
        setTimeout(() => {
            newNameInput.focus();
        }, 100);
    }

    // 编辑标签
    async editTag(tagId) {
        try {
            if (!authManager.isAuthenticated()) {
                alert('请先登录系统');
                return;
            }

            const response = await axios.get(`/api/tag/${tagId}`, {
                params: { userId: this.userId }
            });

            if (response.data.success) {
                this.showEditTagModal(response.data.data);
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('获取标签详情失败:', error);
            this.showMessage('获取标签详情失败: ' + error.message, 'error');
        }
    }

    // 显示编辑标签模态框
    showEditTagModal(tagData) {
        const modalHtml = `
            <div class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>编辑标签</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="edit-tag-form" class="tag-form">
                            <div class="form-group">
                                <label for="tag-name">标签名称 *</label>
                                <input type="text" id="tag-name" class="form-input" required 
                                       value="${this.escapeHtml(tagData.name)}" 
                                       placeholder="请输入标签名称" maxlength="20">
                                <div class="input-hint">最多20个字符，同一用户不能创建重复名称</div>
                                <div id="tag-name-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label>标签信息</label>
                                <div class="tag-info-display">
                                    <span>创建时间: ${new Date(tagData.createdTime).toLocaleString()}</span>
                                    <span>关联文档: ${tagData.documentCount || 0} 个</span>
                                </div>
                            </div>
                            
                            <div id="tag-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">更新</button>
                                <button type="button" class="btn-secondary" onclick="closeModal()">取消</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('modal-container').innerHTML = modalHtml;
        this.setupTagForm('edit', tagData);
    }

    // 删除标签
    async deleteTag(tagId) {
        const tag = this.tags.find(t => t.id === tagId);
        if (!tag) return;

        const documentCount = tag.documentCount || 0;
        let confirmMessage = `确定要删除标签 "${tag.name}" 吗？`;

        if (documentCount > 0) {
            confirmMessage += `\n\n该标签关联了 ${documentCount} 个文档，删除后将移除所有关联关系。`;
        }

        const confirmDelete = confirm(confirmMessage);
        if (!confirmDelete) return;

        try {
            const response = await axios.delete(`/api/tag/${tagId}`, {
                params: { userId: this.userId }
            });

            if (response.data.success) {
                this.showMessage('标签删除成功', 'success');
                this.loadTags();
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('删除标签失败:', error);
            const errorMsg = error.response?.data?.message || error.message;

            if (errorMsg.includes('已被文档使用')) {
                this.showMessage('删除失败：该标签已被文档使用，无法删除', 'error');
            } else {
                this.showMessage('删除标签失败: ' + errorMsg, 'error');
            }
        }
    }

    // 工具方法
    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    showMessage(message, type) {
        // 使用 Element Plus 的消息提示
        if (window.ElMessage) {
            const ElMessage = window.ElMessage;
            if (type === 'success') {
                ElMessage.success(message);
            } else if (type === 'error') {
                ElMessage.error(message);
            } else {
                ElMessage.info(message);
            }
        } else {
            // 降级处理
            alert(message);
        }
    }

    showFormMessage(message, type, container) {
        container.innerHTML = `
            <div class="message ${type}">
                ${message}
            </div>
        `;

        setTimeout(() => {
            container.innerHTML = '';
        }, 5000);
    }
}

// 全局函数
let tagManager;

function initTagManager() {
    tagManager = new TagManager();
    tagManager.init();
}

function showCreateTagModal() {
    if (tagManager) {
        tagManager.showCreateTagModal();
    } else {
        console.error('tagManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

function editTag(tagId) {
    if (tagManager) {
        tagManager.editTag(tagId);
    } else {
        console.error('tagManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

function deleteTag(tagId) {
    if (tagManager) {
        tagManager.deleteTag(tagId);
    } else {
        console.error('tagManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

// 修改 viewTagDocuments 函数
async function viewTagDocuments(tagId) {
    try {
        console.log('查看标签文档，标签ID:', tagId);

        // 1. 跳转到搜索页面
        window.location.hash = 'search';

        // 2. 等待页面切换，然后设置标签筛选
        setTimeout(async () => {
            try {
                // 等待搜索页面元素加载
                await waitForElement('#search-tag', 10, 100);
                await waitForElement('#global-search', 10, 100);

                const tagFilter = document.getElementById('search-tag');
                const searchInput = document.getElementById('global-search');

                if (tagFilter && searchInput) {
                    tagFilter.value = tagId;
                    console.log('已设置标签筛选:', tagId);

                    // 触发搜索
                    if (window.app && typeof window.app.performSearch === 'function') {
                        // 设置搜索关键词为标签名
                        if (tagManager && tagManager.tags) {
                            const tag = tagManager.tags.find(t => t.id === tagId);
                            if (tag) {
                                searchInput.value = tag.name;
                            }
                        }

                        console.log('执行标签搜索...');
                        await window.app.performSearch();
                    } else {
                        console.error('主应用未初始化');
                        alert('应用未初始化，请刷新页面或稍后重试');
                    }
                } else {
                    console.error('搜索页面元素未找到');
                    alert('页面元素未加载完成，请稍后重试');
                }
            } catch (error) {
                console.error('设置标签筛选失败:', error);
                alert('设置标签筛选失败: ' + error.message);
            }
        }, 300);
    } catch (error) {
        console.error('跳转到标签文档失败:', error);
        alert('跳转失败: ' + error.message);
    }
}

// 添加等待元素的辅助函数
function waitForElement(selector, maxAttempts = 10, interval = 100) {
    return new Promise((resolve, reject) => {
        let attempts = 0;

        const checkElement = () => {
            attempts++;
            const element = document.querySelector(selector);

            if (element) {
                resolve(element);
            } else if (attempts >= maxAttempts) {
                reject(new Error(`元素 ${selector} 未在指定时间内出现`));
            } else {
                setTimeout(checkElement, interval);
            }
        };

        checkElement();
    });
}


function closeModal() {
    const modal = document.querySelector('.modal');
    if (modal) {
        modal.remove();
    }
}


// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    if (document.getElementById('tags-page')) {
        // 延迟初始化，确保DOM完全加载
        setTimeout(() => {
            initTagManager();
        }, 100);
    }
});
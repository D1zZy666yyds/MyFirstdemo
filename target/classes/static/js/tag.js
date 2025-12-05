// 标签管理功能 - 完整修复版本
class TagManager {
    constructor() {
        this.tags = [];
        this.currentPage = 1;
        this.pageSize = 20;
        this.searchKeyword = '';
        this.sortBy = 'name';
        this.userId = null;
        this.totalTags = 0;
        this.isLoading = false;
    }

    // 初始化标签管理
    async init() {
        try {
            // 等待认证完成
            await authManager.checkAuthStatus();
            if (!authManager.isAuthenticated()) {
                console.warn('用户未登录，无法加载标签');
                this.showLoginPrompt();
                return;
            }

            this.userId = authManager.getCurrentUserId();
            await this.loadTags();
            this.setupEventListeners();
            this.updateStats();
        } catch (error) {
            console.error('标签管理器初始化失败:', error);
            this.showMessage('初始化失败，请刷新页面重试', 'error');
        }
    }

    // ==================== 核心方法 ====================

    // 设置事件监听器
    setupEventListeners() {
        // 搜索防抖
        let searchTimer;
        const tagSearchInput = document.getElementById('tag-search');
        if (tagSearchInput) {
            tagSearchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimer);

                // 显示搜索中状态
                const searchIcon = tagSearchInput.previousElementSibling;
                if (searchIcon) {
                    searchIcon.textContent = '⏳';
                }

                searchTimer = setTimeout(() => {
                    this.searchKeyword = e.target.value;
                    this.currentPage = 1;
                    this.loadTags();

                    // 恢复搜索图标
                    if (searchIcon) {
                        searchIcon.textContent = '🔍';
                    }
                }, 300);
            });
        }

        // 排序
        const tagSortSelect = document.getElementById('tag-sort');
        if (tagSortSelect) {
            tagSortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.currentPage = 1;
                this.loadTags();

                // 添加视觉反馈
                tagSortSelect.style.boxShadow = '0 0 0 3px rgba(59, 130, 246, 0.2)';
                setTimeout(() => {
                    tagSortSelect.style.boxShadow = '';
                }, 300);
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

            const response = await axios.get(`/api/tag/user/${this.userId}`);

            if (response.data.success) {
                this.tags = response.data.data || [];
                this.totalTags = this.tags.length;

                // 应用搜索和排序
                this.applyFiltersAndSort();
                this.renderTags();
                this.updateStats();
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
            case 'updated':
                filteredTags.sort((a, b) => new Date(b.updatedTime || b.createdTime) - new Date(a.updatedTime || a.createdTime));
                break;
        }

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

        const html = tagsToRender.map((tag, index) => {
            const documentCount = tag.documentCount !== undefined && tag.documentCount !== null ? tag.documentCount : 0;
            const createdTime = new Date(tag.createdTime);
            const timeAgo = this.getTimeAgo(createdTime);

            return `
                <div class="tag-item" data-tag-id="${tag.id}" style="animation-delay: ${index * 50}ms">
                    <div class="tag-info">
                        <div class="tag-color-indicator"></div>
                        <div>
                            <span class="tag-name">${this.escapeHtml(tag.name)}</span>
                            <div class="tag-description">
                                ${timeAgo}创建 • ${tag.description || '无描述'}
                            </div>
                        </div>
                    </div>
                    <div class="tag-stats">
                        <div class="tag-stat">
                            <div class="stat-number">${documentCount}</div>
                            <div class="stat-label">文档</div>
                        </div>
                    </div>
                    <div class="tag-actions">
                        <button onclick="viewTagDocuments(${tag.id})" 
                                class="tag-btn tag-btn-view" 
                                title="查看文档">
                            👁️
                        </button>
                        <button onclick="editTag(${tag.id})" 
                                class="tag-btn tag-btn-edit" 
                                title="编辑标签">
                            ✏️
                        </button>
                        <button onclick="deleteTag(${tag.id})" 
                                class="tag-btn tag-btn-delete" 
                                title="删除标签">
                            🗑️
                        </button>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = html;

        // 添加淡入动画
        setTimeout(() => {
            const tagItems = container.querySelectorAll('.tag-item');
            tagItems.forEach(item => {
                item.style.animation = 'fadeIn 0.3s ease forwards';
                item.style.opacity = '0';
            });
        }, 0);
    }

    // 更新统计信息
    updateStats() {
        const totalElement = document.getElementById('tags-total');
        const usedElement = document.getElementById('tags-used');
        const recentElement = document.getElementById('tags-recent');

        if (totalElement) {
            totalElement.textContent = this.totalTags;
        }

        if (usedElement) {
            const usedTags = this.tags.filter(tag => (tag.documentCount || 0) > 0).length;
            usedElement.textContent = usedTags;
        }

        if (recentElement) {
            const recentTags = this.tags.filter(tag => {
                const createdTime = new Date(tag.createdTime);
                const weekAgo = new Date();
                weekAgo.setDate(weekAgo.getDate() - 7);
                return createdTime > weekAgo;
            }).length;
            recentElement.textContent = recentTags;
        }
    }

    // ==================== 标签表单相关方法 ====================

    // 设置标签表单
    setupTagForm(mode, tagData = null) {
        const form = document.getElementById(`${mode}-tag-form`);
        const messageDiv = document.getElementById('tag-message');
        const nameInput = document.getElementById('tag-name');
        const descriptionInput = document.getElementById('tag-description');

        // 清除之前的监听器
        const newForm = form.cloneNode(true);
        form.parentNode.replaceChild(newForm, form);

        // 重新获取表单元素
        const newFormElement = document.getElementById(`${mode}-tag-form`);
        const newMessageDiv = document.getElementById('tag-message');
        const newNameInput = document.getElementById('tag-name');
        const newDescriptionInput = document.getElementById('tag-description');

        newFormElement.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = {
                name: newNameInput.value.trim(),
                userId: this.userId
            };

            // 如果有描述字段
            if (newDescriptionInput) {
                formData.description = newDescriptionInput.value.trim();
            }

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
            if (newDescriptionInput && tagData.description) {
                newDescriptionInput.value = tagData.description;
            }
        }

        // 聚焦到输入框
        setTimeout(() => {
            newNameInput.focus();
        }, 100);
    }

    // ==================== 模态框相关方法 ====================

    // 显示创建标签模态框
    showCreateTagModal() {
        if (!authManager.isAuthenticated()) {
            this.showMessage('请先登录系统', 'warning');
            return;
        }

        const modalHtml = `
            <div class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>🏷️ 新建标签</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="create-tag-form" class="tag-form">
                            <div class="form-group">
                                <label for="tag-name">标签名称 *</label>
                                <input type="text" id="tag-name" class="form-input" required 
                                       placeholder="请输入标签名称（最多20个字符）" 
                                       maxlength="20"
                                       autocomplete="off">
                                <div class="input-hint">建议使用简洁明了的名称，便于识别和管理</div>
                                <div id="tag-name-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="tag-description">描述（可选）</label>
                                <textarea id="tag-description" class="form-input" 
                                          placeholder="添加标签描述，帮助理解用途..."
                                          rows="2"></textarea>
                                <div class="input-hint">最多100个字符</div>
                            </div>
                            
                            <div id="tag-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">
                                    <span>创建标签</span>
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
        this.setupTagForm('create');

        // 聚焦到输入框
        setTimeout(() => {
            const input = document.getElementById('tag-name');
            if (input) input.focus();
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
                        <h3>✏️ 编辑标签</h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="edit-tag-form" class="tag-form">
                            <div class="form-group">
                                <label for="tag-name">标签名称 *</label>
                                <input type="text" id="tag-name" class="form-input" required 
                                       value="${this.escapeHtml(tagData.name)}" 
                                       placeholder="请输入标签名称" 
                                       maxlength="20">
                                <div class="input-hint">同一用户不能创建重复名称</div>
                                <div id="tag-name-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="tag-description">描述（可选）</label>
                                <textarea id="tag-description" class="form-input" 
                                          placeholder="添加标签描述..."
                                          rows="2">${this.escapeHtml(tagData.description || '')}</textarea>
                            </div>
                            
                            <div class="form-group">
                                <label>标签信息</label>
                                <div class="tag-info-display">
                                    <span>创建时间: ${new Date(tagData.createdTime).toLocaleString()}</span>
                                    <span>关联文档: ${tagData.documentCount || 0} 个</span>
                                    ${tagData.updatedTime ? `<span>最后更新: ${new Date(tagData.updatedTime).toLocaleString()}</span>` : ''}
                                </div>
                            </div>
                            
                            <div id="tag-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">
                                    <span>更新标签</span>
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
        this.setupTagForm('edit', tagData);
    }

    // ==================== 删除标签方法 ====================

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

    // ==================== 工具方法 ====================

    // 获取时间间隔描述
    getTimeAgo(date) {
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

    // 显示登录提示
    showLoginPrompt() {
        const container = document.getElementById('tags-list');
        if (!container) return;

        container.innerHTML = `
            <div class="tags-empty">
                <div class="empty-icon">🔒</div>
                <p>请先登录</p>
                <p class="empty-hint">登录后即可管理您的标签</p>
                <button onclick="window.location.hash = 'login'" class="btn-primary" style="margin-top: 16px;">
                    去登录
                </button>
            </div>
        `;
    }
}

// ==================== 全局函数 ====================

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

function viewTagDocuments(tagId) {
    try {
        console.log('查看标签文档，标签ID:', tagId);

        let tagName = '';
        if (tagManager && tagManager.tags) {
            const tag = tagManager.tags.find(t => t.id === tagId);
            if (tag) {
                tagName = tag.name;
            }
        }

        const params = new URLSearchParams();
        params.set('tagId', tagId);
        if (tagName) {
            params.set('keyword', encodeURIComponent(tagName));
        }

        window.location.hash = `search?${params.toString()}`;

        console.log('跳转到搜索页面，参数:', params.toString());

    } catch (error) {
        console.error('跳转到标签文档失败:', error);
        if (tagManager) {
            tagManager.showMessage('跳转失败: ' + error.message, 'error');
        }
    }
}

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
        modal.style.animation = 'slideUp 0.3s ease reverse';
        setTimeout(() => {
            modal.remove();
        }, 300);
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    if (document.getElementById('tags-page')) {
        setTimeout(() => {
            initTagManager();
        }, 100);
    }
});
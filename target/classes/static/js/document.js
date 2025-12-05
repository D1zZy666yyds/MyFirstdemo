class DocumentManager {
    constructor() {
        this.documents = [];
        this.currentCategory = null;
        this.currentTag = null;
        this.categories = [];
        this.tags = [];
        this.isInitialized = false;
        this.editors = {};
        this.editFormSubmitHandler = null;
        this.hasTagFilterInitialized = false; // 新增：防止重复初始化
    }

    async initialize() {
        if (this.isInitialized) return;

        console.log('初始化文档管理器...');

        try {
            const isAuthenticated = await authManager.checkAuthStatus();
            if (!isAuthenticated) {
                console.warn('用户未登录，无法加载文档');
                this.showError('请先登录系统');
                authManager.redirectToLogin();
                return;
            }

            // 1. 先加载分类
            await this.loadCategories();

            // 2. 加载标签（只加载数据，不立即设置筛选器）
            await this.loadTags();

            // 3. 加载文档
            await this.loadDocuments();

            this.isInitialized = true;

            // 4. 设置筛选器（延迟执行，确保DOM已存在）
            setTimeout(() => {
                this.setupCategoryFilter();
                this.setupTagFilter();
            }, 100);

        } catch (error) {
            console.error('文档管理器初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // 修改：统一的标签加载方法
    async loadTags() {
        try {
            const userId = authManager.getCurrentUserId();
            console.log('加载标签，用户ID:', userId);

            const response = await axios.get(`/api/tag/user/${userId}`);
            console.log('标签API响应:', response.data);

            if (response.data.success) {
                this.tags = response.data.data || [];
                console.log('标签加载完成，数量:', this.tags.length);
            } else {
                console.error('加载标签失败:', response.data.message);
                this.tags = [];
            }
        } catch (error) {
            console.error('加载标签失败:', error);
            console.error('错误详情:', error.response?.data || error.message);
            this.tags = [];
        }
    }

    // 修改：设置标签筛选器（只执行一次）
    setupTagFilter() {
        // 防止重复初始化
        if (this.hasTagFilterInitialized) {
            console.log('标签筛选器已初始化，跳过');
            return;
        }

        const tagFilter = document.getElementById('tag-filter');
        if (!tagFilter) {
            console.error('标签筛选器元素未找到: #tag-filter');
            setTimeout(() => this.setupTagFilter(), 100); // 延迟重试
            return;
        }

        console.log('设置标签筛选器，标签数量:', this.tags.length);

        // 清空并添加默认选项
        tagFilter.innerHTML = '<option value="">全部标签</option>';

        // 添加所有标签选项
        this.tags.forEach(tag => {
            const option = document.createElement('option');
            option.value = tag.id;
            option.textContent = tag.name;
            tagFilter.appendChild(option);
        });

        // 设置事件监听器（使用事件委托）
        tagFilter.addEventListener('change', (e) => {
            const tagId = e.target.value;
            console.log('标签筛选改变:', tagId ? tagId : '全部标签');

            // 重置分类筛选（互斥筛选）
            const categoryFilter = document.getElementById('category-filter');
            if (categoryFilter && categoryFilter.value) {
                categoryFilter.value = "";
                this.currentCategory = null;
            }

            this.currentTag = tagId ? parseInt(tagId) : null;
            this.loadDocuments(null, tagId ? parseInt(tagId) : null);
        });

        this.hasTagFilterInitialized = true;
        console.log('标签筛选器设置完成，选项数:', tagFilter.options.length);
    }

    // 修改：统一填充所有选择器（包括标签筛选器）
    populateCategorySelects() {
        const categorySelects = [
            document.getElementById('doc-category'),
            document.getElementById('edit-doc-category'),
            document.getElementById('category-filter'),
            document.getElementById('search-category'),
            document.getElementById('create-doc-category')
        ];

        categorySelects.forEach(select => {
            if (select) {
                // 保留第一个选项
                const firstOption = select.options[0];
                select.innerHTML = '';
                if (firstOption) {
                    select.appendChild(firstOption);
                }

                // 添加所有分类选项
                this.categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.id;
                    option.textContent = category.name;
                    select.appendChild(option);
                });
            }
        });
    }

    // 新增：统一填充标签选择器（包括标签筛选器）
    populateTagSelects() {
        const tagSelects = [
            document.getElementById('doc-tags'),
            document.getElementById('edit-doc-tags'),
            document.getElementById('create-doc-tags'),
            document.getElementById('tag-filter')
        ];

        tagSelects.forEach(select => {
            if (select) {
                // 保留第一个选项
                const firstOption = select.options[0];
                select.innerHTML = '';
                if (firstOption) {
                    select.appendChild(firstOption);
                }

                // 添加所有标签选项
                this.tags.forEach(tag => {
                    const option = document.createElement('option');
                    option.value = tag.id;
                    option.textContent = tag.name;
                    select.appendChild(option);
                });
            }
        });
    }

    async loadCategories() {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/category/user/${userId}`);

            if (response.data.success) {
                this.categories = response.data.data || [];
                console.log('分类加载完成:', this.categories.length);
                this.populateCategorySelects();
            } else {
                console.error('加载分类失败:', response.data.message);
                this.categories = [];
            }
        } catch (error) {
            console.error('加载分类失败:', error);
            this.categories = [];
        }
    }

    async loadDocuments(categoryId = null, tagId = null) {
        try {
            if (!authManager.isAuthenticated()) {
                console.warn('用户未登录，无法加载文档');
                return;
            }

            const userId = authManager.getCurrentUserId();
            let url;
            let params = {};

            // 优先处理标签筛选
            if (tagId) {
                url = `/api/tag/document/${tagId}/documents`;
                params.userId = userId;
                console.log('按标签筛选，标签ID:', tagId);
            }
            // 然后处理分类筛选
            else if (categoryId) {
                url = `/api/document/category/${categoryId}`;
                params.userId = userId;
                console.log('按分类筛选，分类ID:', categoryId);
            }
            // 默认获取所有文档
            else {
                url = `/api/document/user/${userId}`;
                console.log('获取所有文档');
            }

            console.log('加载文档，URL:', url, '参数:', params);
            const response = await axios.get(url, { params: params });

            if (response.data.success) {
                this.documents = response.data.data || [];

                // 为每个文档加载标签
                for (let doc of this.documents) {
                    await this.loadDocumentTags(doc);
                }

                console.log('文档加载完成:', this.documents.length);
                this.displayDocuments();

                // 更新筛选器状态显示
                this.updateFilterStatus(categoryId, tagId);

                this.safeTriggerDocumentListLoaded();
            } else {
                console.error('加载文档失败:', response.data.message);
                this.showError('加载文档失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('加载文档失败:', error);
            this.showError('加载文档失败: ' + error.message);
        }
    }

    // 更新筛选器状态显示
    updateFilterStatus(categoryId, tagId) {
        const categoryFilter = document.getElementById('category-filter');
        const tagFilter = document.getElementById('tag-filter');

        if (categoryFilter) {
            categoryFilter.value = categoryId || "";
        }

        if (tagFilter) {
            tagFilter.value = tagId || "";
        }

        // 更新当前筛选状态
        this.currentCategory = categoryId;
        this.currentTag = tagId;

        // 显示/隐藏清除筛选按钮
        this.updateClearFilterButton();
    }

    // 更新清除筛选按钮
    updateClearFilterButton() {
        const clearBtn = document.getElementById('clear-filters');
        if (clearBtn) {
            if (this.currentCategory || this.currentTag) {
                clearBtn.style.display = 'inline-block';
            } else {
                clearBtn.style.display = 'none';
            }
        }
    }

    setupCategoryFilter() {
        const categoryFilter = document.getElementById('category-filter');
        if (categoryFilter) {
            // 移除旧的事件监听器（防止重复绑定）
            const newCategoryFilter = categoryFilter.cloneNode(true);
            categoryFilter.parentNode.replaceChild(newCategoryFilter, categoryFilter);

            // 重新获取元素
            const freshCategoryFilter = document.getElementById('category-filter');

            // 确保有"全部分类"选项
            if (freshCategoryFilter.options.length > 0 && freshCategoryFilter.options[0].value !== "") {
                const allOption = document.createElement('option');
                allOption.value = "";
                allOption.textContent = "全部分类";
                freshCategoryFilter.insertBefore(allOption, freshCategoryFilter.firstChild);
            }

            // 绑定事件
            freshCategoryFilter.addEventListener('change', (e) => {
                const categoryId = e.target.value;
                console.log('分类筛选改变:', categoryId ? categoryId : '全部分类');

                // 重置标签筛选
                const tagFilter = document.getElementById('tag-filter');
                if (tagFilter) {
                    tagFilter.value = "";
                    this.currentTag = null;
                }

                this.loadDocuments(categoryId || null);
            });

            console.log('分类筛选器设置完成，选项数:', freshCategoryFilter.options.length);
        } else {
            console.error('分类筛选元素未找到');
        }
    }

    displayDocuments() {
        const container = document.getElementById('documents-list');
        if (!container) {
            console.error('文档容器未找到');
            return;
        }

        if (!this.documents || this.documents.length === 0) {
            // 显示筛选状态信息
            let message = '暂无文档';
            if (this.currentCategory) {
                const category = this.categories.find(c => c.id === this.currentCategory);
                message = `分类"${category ? category.name : '未知'}"下暂无文档`;
            } else if (this.currentTag) {
                const tag = this.tags.find(t => t.id === this.currentTag);
                message = `标签"${tag ? tag.name : '未知'}"下暂无文档`;
            }

            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📄</div>
                    <p>${message}</p>
                    ${this.currentCategory || this.currentTag ?
                '<button onclick="documentManager.clearFilters()" class="btn-secondary" style="margin-top: 10px;">清除筛选</button>' :
                ''
            }
                </div>
            `;
            return;
        }

        // 生成文档卡片
        container.innerHTML = this.documents.map(doc => `
            <div class="doc-card" data-document-id="${doc.id}">
                <div class="doc-title">${this.escapeHtml(doc.title || '无标题')}</div>
                <div class="doc-meta">
                    <span>分类: ${this.getCategoryName(doc.categoryId)}</span>
                    <span>更新时间: ${doc.updateTime ? new Date(doc.updateTime).toLocaleDateString() : '未知'}</span>
                </div>
                <div class="doc-tags">
                    ${this.renderDocumentTags(doc.tags)}
                </div>
                <div class="doc-actions">
                    <button onclick="documentManager.viewDocument(${doc.id})" class="btn-secondary">查看</button>
                    <button onclick="documentManager.editDocument(${doc.id})" class="btn-secondary">编辑</button>
                    <button onclick="documentManager.deleteDocument(${doc.id})" class="btn-danger">删除</button>
                </div>
            </div>
        `).join('');
    }

    // 清除筛选
    clearFilters() {
        console.log('清除所有筛选');

        // 重置筛选器值
        const categoryFilter = document.getElementById('category-filter');
        const tagFilter = document.getElementById('tag-filter');

        if (categoryFilter) categoryFilter.value = "";
        if (tagFilter) tagFilter.value = "";

        // 重置状态
        this.currentCategory = null;
        this.currentTag = null;

        // 隐藏清除按钮
        this.updateClearFilterButton();

        // 重新加载所有文档
        this.loadDocuments();
    }

    // 渲染文档标签
    renderDocumentTags(tags) {
        if (!tags || tags.length === 0) {
            return '<span class="no-tags">无标签</span>';
        }

        return tags.map(tag => `
            <span class="doc-tag" data-tag-id="${tag.id}">
                ${this.escapeHtml(tag.name)}
            </span>
        `).join('');
    }

    getCategoryName(categoryId) {
        if (!categoryId) return '未分类';
        const category = this.categories.find(cat => cat.id === categoryId);
        return category ? category.name : '未分类';
    }

    // 加载文档及其标签
    async loadDocumentWithTags(documentId) {
        try {
            const userId = authManager.getCurrentUserId();

            const docResponse = await axios.get(`/api/document/${documentId}`, {
                params: { userId: userId }
            });

            if (!docResponse.data.success) {
                throw new Error(docResponse.data.message);
            }

            const document = docResponse.data.data;

            const tagResponse = await axios.get(`/api/tag/document/${documentId}`, {
                params: { userId: userId }
            });

            if (tagResponse.data.success) {
                document.tags = tagResponse.data.data || [];
            } else {
                document.tags = [];
            }

            return document;
        } catch (error) {
            console.error('加载文档失败:', error);
            throw error;
        }
    }

    async loadDocumentTags(document) {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/tag/document/${document.id}`, {
                params: { userId: userId }
            });

            if (response.data.success) {
                document.tags = response.data.data || [];
            } else {
                document.tags = [];
            }
        } catch (error) {
            console.error(`获取文档 ${document.id} 的标签失败:`, error);
            document.tags = [];
        }
    }

    // 编辑器相关方法（保持不变）
    cleanEditorContent(content) {
        // 保持原样...
        if (!content || typeof content !== 'string') {
            return '';
        }

        const contentStr = content.trim();

        // 1. 如果是完整的 EasyMDE 源码模式
        if (contentStr.includes('function(e){var t=this.codemirror')) {
            console.log('🔄 检测到 EasyMDE 污染模式，尝试处理');

            const valueMatches = [
                /setValue\(['"`]([^'"`]*)['"`]\)/g,
                /setValue\(([^)]+)\)/g
            ];

            for (const pattern of valueMatches) {
                const matches = [...contentStr.matchAll(pattern)];
                if (matches.length > 0) {
                    for (const match of matches) {
                        if (match[1] && match[1].length > 0 &&
                            !match[1].includes('this.codemirror') &&
                            !match[1].includes('function(')) {
                            console.log('✅ 从污染内容中提取到文本');
                            return match[1];
                        }
                    }
                }
            }

            return '';
        }

        // 2. 如果是明显的JS函数代码
        if ((contentStr.includes('function(') && contentStr.includes('return')) ||
            contentStr.includes('this.codemirror') ||
            contentStr.includes('getValue()')) {
            console.log('⚠️ 检测到JS代码，清空内容');
            return '';
        }

        // 3. 正常内容直接返回
        return contentStr;
    }

    async initEditor(elementId, content = '', mode = 'create') {
        // 保持原样...
        console.log('🔄 初始化编辑器:', elementId, '模式:', mode);

        let safeContent = this.cleanEditorContent(content);

        if (!safeContent && content && content.length > 50) {
            safeContent = '⚠️ 此文档内容异常（可能由于编辑器故障）。\n请重新输入您的内容，系统已修复此问题。';
        }

        if (this.editors[elementId]) {
            try {
                const editor = this.editors[elementId];
                if (editor.toTextArea) {
                    editor.toTextArea();
                }
                if (editor.element) {
                    editor.element.remove();
                }
            } catch (e) {
                console.warn('清理旧编辑器时出错:', e);
            }
            delete this.editors[elementId];
        }

        await this.waitForElement(elementId);

        const editorElement = document.getElementById(elementId);
        if (!editorElement) {
            console.error('编辑器元素未找到:', elementId);
            return null;
        }

        try {
            editorElement.innerHTML = '<textarea class="editor-textarea"></textarea>';
            const textarea = editorElement.querySelector('textarea');
            textarea.value = safeContent;

            textarea.style.width = '100%';
            textarea.style.height = '400px';
            textarea.style.padding = '10px';
            textarea.style.border = '1px solid #ddd';
            textarea.style.fontFamily = 'monospace';
            textarea.style.resize = 'vertical';

            const shouldUseEasyMDE = typeof EasyMDE !== 'undefined' &&
                !this.containsJsCode(safeContent);

            if (shouldUseEasyMDE) {
                try {
                    console.log('使用EasyMDE初始化编辑器');
                    const easyMDE = new EasyMDE({
                        element: textarea,
                        initialValue: safeContent,
                        spellChecker: false,
                        autosave: { enabled: false },
                        toolbar: [
                            "bold", "italic", "heading", "|",
                            "quote", "unordered-list", "ordered-list", "|",
                            "link", "image", "|",
                            "preview", "|",
                            "guide"
                        ],
                        status: false,
                        placeholder: "请输入文档内容，支持Markdown语法...",
                        autoDownloadFontAwesome: false
                    });

                    this.editors[elementId] = easyMDE;
                    return easyMDE;
                } catch (easyMdeError) {
                    console.error('EasyMDE初始化失败，使用普通文本区域:', easyMdeError);
                }
            }

            console.log('使用简单文本区域编辑器');
            const simpleEditor = {
                getContent: function() {
                    return textarea.value;
                },
                value: textarea.value,
                isSimpleEditor: true,
                destroy: function() {
                    if (textarea && textarea.parentNode) {
                        textarea.parentNode.removeChild(textarea);
                    }
                }
            };

            this.editors[elementId] = simpleEditor;
            return simpleEditor;

        } catch (error) {
            console.error('编辑器初始化失败:', error);

            editorElement.innerHTML = `<textarea style="width:100%;height:400px;padding:10px;border:1px solid #ddd;">${this.escapeHtml(safeContent)}</textarea>`;

            const fallbackTextarea = editorElement.querySelector('textarea');
            const fallbackEditor = {
                getContent: function() { return fallbackTextarea.value; },
                value: fallbackTextarea.value,
                isFallback: true,
                destroy: function() { }
            };

            this.editors[elementId] = fallbackEditor;
            return fallbackEditor;
        }
    }

    containsJsCode(content) {
        // 保持原样...
        if (!content || typeof content !== 'string') return false;

        const jsPatterns = [
            /this\.codemirror/,
            /function\s*\(/,
            /getValue\s*\(/,
            /setValue\s*\(/,
            /getWrapperElement\s*\(/,
            /EasyMDE\.prototype/
        ];

        for (const pattern of jsPatterns) {
            if (pattern.test(content)) {
                return true;
            }
        }

        return false;
    }

    waitForElement(elementId, maxAttempts = 10, interval = 100) {
        return new Promise((resolve, reject) => {
            let attempts = 0;

            const checkElement = () => {
                attempts++;
                const element = document.getElementById(elementId);

                if (element) {
                    resolve(element);
                } else if (attempts >= maxAttempts) {
                    reject(new Error(`元素 ${elementId} 未找到`));
                } else {
                    setTimeout(checkElement, interval);
                }
            };

            checkElement();
        });
    }

    getEditorContent(editorId) {
        const editor = this.editors[editorId];
        if (!editor) return '';

        try {
            let content = '';

            if (editor.isSimpleEditor) {
                content = editor.getContent();
            } else if (editor.isFallback) {
                content = editor.getContent();
            } else if (typeof editor.value === 'function') {
                content = editor.value();
            } else if (typeof editor.value === 'string') {
                content = editor.value;
            } else if (editor.codemirror) {
                content = editor.codemirror.getValue();
            } else {
                const textarea = document.querySelector(`#${editorId} textarea`);
                content = textarea ? textarea.value : '';
            }

            if (this.containsJsCode(content)) {
                console.warn('⚠️ 获取的内容包含JS代码，已清空');
                return '';
            }

            return content;
        } catch (error) {
            console.error('获取编辑器内容失败:', error);
            return '';
        }
    }

    // 编辑文档表单
    renderDocumentEditForm(docData) {
        const categoryOptions = this.categories.map(cat => `
            <option value="${cat.id}" ${docData.categoryId === cat.id ? 'selected' : ''}>
                ${this.escapeHtml(cat.name)}
            </option>
        `).join('');

        const docTagIds = docData.tags ? docData.tags.map(tag => tag.id) : [];

        return `
            <form id="edit-document-form" class="document-form">
                <input type="hidden" id="edit-doc-id" value="${docData.id}">
                <div class="form-group">
                    <label for="edit-doc-title">标题:</label>
                    <input type="text" id="edit-doc-title" value="${this.escapeHtml(docData.title || '')}" required class="form-input">
                </div>
                <div class="form-group">
                    <label for="edit-doc-category">分类:</label>
                    <select id="edit-doc-category" class="form-select">
                        <option value="">未分类</option>
                        ${categoryOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label for="edit-doc-tags">标签:</label>
                    <select id="edit-doc-tags" multiple class="form-select form-select-tags">
                        ${this.tags.map(tag => `
                            <option value="${tag.id}" ${docTagIds.includes(tag.id) ? 'selected' : ''}>
                                ${this.escapeHtml(tag.name)}
                            </option>
                        `).join('')}
                    </select>
                    <div class="input-hint">按住 Ctrl 键可选择多个标签</div>
                </div>
                <div class="form-group">
                    <label>内容:</label>
                    <div class="editor-info">
                        <span class="editor-tip">支持Markdown语法，左侧编辑，右侧实时预览</span>
                    </div>
                    <div id="edit-doc-editor" class="mavon-editor-container"></div>
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn-primary">保存</button>
                    <button type="button" onclick="this.closest('.modal').remove()" class="btn-secondary">取消</button>
                </div>
            </form>
        `;
    }

    async editDocument(documentId) {
        try {
            if (!authManager.isAuthenticated()) {
                this.showError('请先登录系统');
                return;
            }

            const document = await this.loadDocumentWithTags(documentId);
            this.showDocumentModal(document, 'edit');
        } catch (error) {
            console.error('加载文档失败:', error);
            this.showError('加载文档失败: ' + error.message);
        }
    }

    async showCreateDocumentModal() {
        if (!authManager.isAuthenticated()) {
            alert('请先登录系统');
            return;
        }

        const modalContent = `
            <div class="modal">
                <div class="modal-content document-modal large-modal">
                    <div class="modal-header">
                        <h3>新建文档</h3>
                        <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="create-document-form" class="document-form">
                            <div class="form-group">
                                <label for="doc-title">标题:</label>
                                <input type="text" id="doc-title" required class="form-input" placeholder="请输入文档标题">
                            </div>
                            <div class="form-group">
                                <label for="doc-category">分类:</label>
                                <select id="doc-category" class="form-select">
                                    <option value="">未分类</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="doc-tags">标签:</label>
                                <select id="doc-tags" multiple class="form-select form-select-tags">
                                    <option value="">选择标签...</option>
                                </select>
                                <div class="input-hint">按住 Ctrl 键可选择多个标签</div>
                            </div>
                            <div class="form-group">
                                <label>内容:</label>
                                <div class="editor-info">
                                    <span class="editor-tip">支持Markdown语法，左侧编辑，右侧实时预览</span>
                                </div>
                                <div id="create-doc-editor" class="mavon-editor-container"></div>
                            </div>
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">创建</button>
                                <button type="button" onclick="this.closest('.modal').remove()" class="btn-secondary">取消</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        const modalContainer = document.getElementById('modal-container');
        if (modalContainer) {
            modalContainer.innerHTML = modalContent;

            // 填充分类和标签选项
            this.populateCategorySelects();
            this.populateTagSelects();

            try {
                await this.initEditor('create-doc-editor', '', 'create');
            } catch (error) {
                console.error('编辑器初始化失败:', error);
                const editorElement = document.getElementById('create-doc-editor');
                if (editorElement) {
                    editorElement.innerHTML = '<textarea style="width:100%;height:400px;padding:10px;border:1px solid #ddd;"></textarea>';
                }
            }

            const form = document.getElementById('create-document-form');
            if (form) {
                form.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    await this.handleCreateDocument();
                });
            }
        } else {
            console.error('模态框容器未找到');
        }
    }

    async handleCreateDocument() {
        try {
            const title = document.getElementById('doc-title').value;
            const categoryId = document.getElementById('doc-category').value || null;
            const tagSelect = document.getElementById('doc-tags');
            const tagIds = Array.from(tagSelect.selectedOptions)
                .map(option => option.value)
                .filter(id => id);

            const content = this.getEditorContent('create-doc-editor');

            if (!title.trim()) {
                this.showError('请输入文档标题');
                return;
            }

            if (!content.trim()) {
                this.showError('请输入文档内容');
                return;
            }

            const userId = authManager.getCurrentUserId();
            const documentData = {
                title: title.trim(),
                content: content.trim(),
                categoryId: categoryId ? parseInt(categoryId) : null,
                userId: userId
            };

            console.log('创建文档请求数据:', documentData);

            const response = await axios.post('/api/document', documentData);

            if (response.data.success) {
                const createdDocument = response.data.data;

                if (tagIds.length > 0) {
                    try {
                        await axios.post(`/api/tag/document/${createdDocument.id}/batch`, tagIds, {
                            params: { userId: userId }
                        });
                        console.log('文档标签设置成功');
                    } catch (tagError) {
                        console.error('设置文档标签失败:', tagError);
                    }
                }

                this.showSuccess('文档创建成功');
                const modal = document.querySelector('.modal');
                if (modal) {
                    modal.remove();
                }
                if (this.editors['create-doc-editor']) {
                    try {
                        if (this.editors['create-doc-editor'].destroy) {
                            this.editors['create-doc-editor'].destroy();
                        }
                    } catch (e) {
                    }
                    delete this.editors['create-doc-editor'];
                }
                await this.loadDocuments();
            } else {
                this.showError('创建文档失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('创建文档失败:', error);
            this.showError('创建文档失败: ' + error.message);
        }
    }

    async handleEditDocument(event) {
        try {
            if (event) {
                event.preventDefault();
            }

            const documentId = document.getElementById('edit-doc-id').value;
            const title = document.getElementById('edit-doc-title').value;
            const categoryId = document.getElementById('edit-doc-category').value || null;
            const tagSelect = document.getElementById('edit-doc-tags');
            const tagIds = Array.from(tagSelect.selectedOptions)
                .map(option => option.value)
                .filter(id => id);

            const content = this.getEditorContent('edit-doc-editor');

            if (!title.trim()) {
                this.showError('请输入文档标题');
                return;
            }

            if (!content.trim()) {
                this.showError('请输入文档内容');
                return;
            }

            const userId = authManager.getCurrentUserId();
            const documentData = {
                title: title.trim(),
                content: content.trim(),
                categoryId: categoryId ? parseInt(categoryId) : null,
                userId: userId
            };

            console.log('更新文档请求数据:', documentData);

            const response = await axios.put(`/api/document/${documentId}`, documentData);

            if (response.data.success) {
                try {
                    await axios.post(`/api/tag/document/${documentId}/batch`, tagIds, {
                        params: { userId: userId }
                    });
                    console.log('文档标签更新成功');
                } catch (tagError) {
                    console.error('更新文档标签失败:', tagError);
                }

                this.showSuccess('文档更新成功');
                const modal = document.querySelector('.modal');
                if (modal) {
                    modal.remove();
                }
                if (this.editors['edit-doc-editor']) {
                    try {
                        if (this.editors['edit-doc-editor'].destroy) {
                            this.editors['edit-doc-editor'].destroy();
                        }
                    } catch (e) {
                    }
                    delete this.editors['edit-doc-editor'];
                }
                await this.loadDocuments();
            } else {
                this.showError('更新文档失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('更新文档失败:', error);
            this.showError('更新文档失败: ' + error.message);
        }
    }

    async viewDocument(documentId) {
        try {
            const doc = await this.loadDocumentWithTags(documentId);
            this.showDocumentViewModal(doc);
            this.safeTriggerDocumentLoaded(doc);
        } catch (error) {
            console.error('查看文档失败:', error);
            this.showError('查看文档失败: ' + error.message);
        }
    }

    showDocumentViewModal(doc) {
        const modalContent = `
            <div class="modal">
                <div class="modal-content document-modal large">
                    <div class="modal-header">
                        <h3>${this.escapeHtml(doc.title || '无标题')}</h3>
                        <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <div class="doc-view-meta">
                            <span>分类: ${this.getCategoryName(doc.categoryId)}</span>
                            <span>标签: ${this.renderDocumentTagsForView(doc.tags)}</span>
                            <span>创建时间: ${doc.createdTime ? new Date(doc.createdTime).toLocaleString() : '未知'}</span>
                            <span>更新时间: ${doc.updatedTime ? new Date(doc.updatedTime).toLocaleString() : '未知'}</span>
                        </div>
                        <div class="doc-view-content markdown-body">
                            <div id="markdown-preview"></div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button onclick="documentManager.editDocument(${doc.id})" class="btn-primary">编辑</button>
                        <button onclick="this.closest('.modal').remove()" class="btn-secondary">关闭</button>
                    </div>
                </div>
            </div>
        `;

        const modalContainer = document.getElementById('modal-container');
        if (modalContainer) {
            modalContainer.innerHTML = modalContent;

            setTimeout(() => {
                const previewDiv = document.getElementById('markdown-preview');
                if (previewDiv && doc.content) {
                    const cleanContent = this.cleanEditorContent(doc.content);
                    let html = cleanContent || '此文档内容异常，请编辑修复';

                    if (cleanContent && cleanContent !== '此文档内容异常，请编辑修复') {
                        html = cleanContent
                            .replace(/^### (.*$)/gm, '<h3>$1</h3>')
                            .replace(/^## (.*$)/gm, '<h2>$1</h2>')
                            .replace(/^# (.*$)/gm, '<h1>$1</h1>')
                            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                            .replace(/\*(.*?)\*/g, '<em>$1</em>')
                            .replace(/`(.*?)`/g, '<code>$1</code>')
                            .replace(/!\[(.*?)\]\((.*?)\)/g, '<img alt="$1" src="$2" style="max-width:100%;">')
                            .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2">$1</a>')
                            .replace(/\n/g, '<br>');
                    }

                    previewDiv.innerHTML = html;
                }
            }, 100);
        }
    }

    renderDocumentTagsForView(tags) {
        if (!tags || tags.length === 0) {
            return '无标签';
        }
        return tags.map(tag => this.escapeHtml(tag.name)).join(', ');
    }

    async deleteDocument(docId) {
        if (!confirm('确定要删除这个文档吗？文档将移动到回收站，您可以随时恢复。')) return;

        try {
            if (!authManager.isAuthenticated()) {
                this.showError('请先登录系统');
                return false;
            }

            const userId = authManager.getCurrentUserId();
            const response = await axios.delete(`/api/document/${docId}`, {
                params: { userId: userId }
            });

            if (response.data.success) {
                this.showSuccess('文档已移动到回收站');

                try {
                    if (typeof window !== 'undefined' && window.document && window.document.dispatchEvent) {
                        const event = new CustomEvent('documentDeleted', { detail: { docId } });
                        window.document.dispatchEvent(event);
                    }
                } catch (error) {
                    console.warn('触发文档删除事件失败:', error);
                }

                await this.loadDocuments();
                return true;
            } else {
                this.showError('删除文档失败: ' + response.data.message);
                return false;
            }
        } catch (error) {
            console.error('删除文档失败:', error);
            this.showError('删除文档失败: ' + error.message);
            return false;
        }
    }

    async permanentDeleteDocument(documentId) {
        if (!confirm('确定要永久删除这个文档吗？此操作不可撤销。')) {
            return;
        }

        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.delete(`/api/document/permanent/${documentId}`, {
                params: { userId: userId }
            });

            if (response.data.success) {
                this.showSuccess('文档永久删除成功');
                await this.loadDocuments();
            } else {
                this.showError('删除文档失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('删除文档失败:', error);
            this.showError('删除文档失败: ' + error.message);
        }
    }

    async showDocumentModal(docData, mode) {
        const modalContent = `
            <div class="modal">
                <div class="modal-content document-modal large-modal">
                    <div class="modal-header">
                        <h3>${mode === 'edit' ? '编辑文档' : '新建文档'}</h3>
                        <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                    </div>
                    <div class="modal-body">
                        ${mode === 'edit' ? this.renderDocumentEditForm(docData) : this.renderDocumentCreateForm()}
                    </div>
                </div>
            </div>
        `;

        const modalContainer = document.getElementById('modal-container');
        if (modalContainer) {
            modalContainer.innerHTML = modalContent;

            if (mode === 'edit') {
                this.populateCategorySelects();
                this.populateTagSelects();

                setTimeout(async () => {
                    try {
                        await this.initEditor('edit-doc-editor', docData.content || '', 'edit');
                    } catch (error) {
                        console.error('编辑器初始化失败:', error);
                        const editorElement = document.getElementById('edit-doc-editor');
                        if (editorElement) {
                            editorElement.innerHTML = `<textarea style="width:100%;height:400px;padding:10px;border:1px solid #ddd;">${this.escapeHtml(docData.content || '')}</textarea>`;
                        }
                    }
                }, 100);

                const form = document.getElementById('edit-document-form');
                if (form) {
                    if (this.editFormSubmitHandler) {
                        form.removeEventListener('submit', this.editFormSubmitHandler);
                    }

                    this.editFormSubmitHandler = (e) => {
                        e.preventDefault();
                        this.handleEditDocument(e);
                    };

                    form.addEventListener('submit', this.editFormSubmitHandler);
                }
            }
        }
    }

    renderDocumentCreateForm() {
        const categoryOptions = this.categories.map(cat => `
            <option value="${cat.id}">${this.escapeHtml(cat.name)}</option>
        `).join('');

        const tagOptions = this.tags.map(tag => `
            <option value="${tag.id}">${this.escapeHtml(tag.name)}</option>
        `).join('');

        return `
            <form id="create-document-form" class="document-form">
                <div class="form-group">
                    <label for="create-doc-title">标题:</label>
                    <input type="text" id="create-doc-title" required class="form-input" placeholder="请输入文档标题">
                </div>
                <div class="form-group">
                    <label for="create-doc-category">分类:</label>
                    <select id="create-doc-category" class="form-select">
                        <option value="">未分类</option>
                        ${categoryOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label for="create-doc-tags">标签:</label>
                    <select id="create-doc-tags" multiple class="form-select form-select-tags">
                        <option value="">选择标签...</option>
                        ${tagOptions}
                    </select>
                    <div class="input-hint">按住 Ctrl 键可选择多个标签</div>
                </div>
                <div class="form-group">
                    <label>内容:</label>
                    <div class="editor-info">
                        <span class="editor-tip">支持Markdown语法，左侧编辑，右侧实时预览</span>
                    </div>
                    <div id="create-doc-editor" class="mavon-editor-container"></div>
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn-primary">创建</button>
                    <button type="button" onclick="this.closest('.modal').remove()" class="btn-secondary">取消</button>
                </div>
            </form>
        `;
    }

    safeTriggerDocumentListLoaded() {
        try {
            if (typeof document !== 'undefined' && document.dispatchEvent) {
                const event = new CustomEvent('documentListLoaded', {
                    detail: { documents: this.documents }
                });
                document.dispatchEvent(event);
            }
        } catch (error) {
            console.warn('触发文档列表事件失败:', error);
        }
    }

    safeTriggerDocumentLoaded(doc) {
        try {
            if (typeof window !== 'undefined' && window.document && window.document.dispatchEvent) {
                const event = new CustomEvent('documentLoaded', {
                    detail: { document: doc }
                });
                window.document.dispatchEvent(event);
            }
        } catch (error) {
            console.warn('触发文档加载事件失败:', error);
        }
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showError(message) {
        console.error('文档管理错误:', message);
        alert('错误: ' + message);
    }

    showSuccess(message) {
        console.log('文档管理成功:', message);
        alert('成功: ' + message);
    }
}

// 文档管理器实例
const documentManager = new DocumentManager();

// 全局函数
function showCreateDocumentModal() {
    if (documentManager) {
        documentManager.showCreateDocumentModal();
    } else {
        console.error('documentManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

// 清除筛选的全局函数
function clearFilters() {
    if (documentManager) {
        documentManager.clearFilters();
    }
}

// 简化初始化：只在DOM加载完成后执行一次
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(() => {
        if (documentManager && !documentManager.isInitialized) {
            documentManager.initialize();
        }
    }, 500);
});

// 确保全局可访问
window.documentManager = documentManager;
window.clearFilters = clearFilters;
window.showCreateDocumentModal = showCreateDocumentModal;
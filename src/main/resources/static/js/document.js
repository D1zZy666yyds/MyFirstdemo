class DocumentManager {
    constructor() {
        this.documents = [];
        this.currentCategory = null;
        this.categories = [];
        this.tags = []; // 新增标签数据
        this.isInitialized = false;
        this.editors = {}; // 存储编辑器实例
        this.editFormSubmitHandler = null; // 用于存储事件处理器
    }

    async initialize() {
        if (this.isInitialized) return;

        console.log('初始化文档管理器...');

        try {
            // 先检查认证状态
            const isAuthenticated = await authManager.checkAuthStatus();
            if (!isAuthenticated) {
                console.warn('用户未登录，无法加载文档');
                this.showError('请先登录系统');
                authManager.redirectToLogin();
                return;
            }

            // 先加载分类和标签，再加载文档
            await this.loadCategories();
            await this.loadTags(); // 新增：加载标签
            await this.loadDocuments();
            this.isInitialized = true;

        } catch (error) {
            console.error('文档管理器初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    // ==================== 修复方法：内容安全清理 ====================
    cleanEditorContent(content) {
        if (!content || typeof content !== 'string') {
            return '';
        }

        const contentStr = content.trim();

        // 1. 如果是完整的 EasyMDE 源码模式
        if (contentStr.includes('function(e){var t=this.codemirror')) {
            console.log('🔄 检测到 EasyMDE 污染模式，尝试处理');

            // 尝试提取可能的原始内容
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

            // 如果无法提取，返回空
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

    // 新增：加载标签数据
    async loadTags() {
        try {
            const userId = authManager.getCurrentUserId();
            const response = await axios.get(`/api/tag/user/${userId}`);

            if (response.data.success) {
                this.tags = response.data.data || [];
                console.log('标签加载完成:', this.tags.length);
                this.populateTagSelects();
            } else {
                console.error('加载标签失败:', response.data.message);
                this.tags = [];
            }
        } catch (error) {
            console.error('加载标签失败:', error);
            this.tags = [];
        }
    }

    // 新增：填充标签选择器
    populateTagSelects() {
        const tagSelects = [
            document.getElementById('doc-tags'),
            document.getElementById('edit-doc-tags'),
            document.getElementById('create-doc-tags') // 新增：创建表单标签选择器
        ];

        tagSelects.forEach(select => {
            if (select) {
                select.innerHTML = '<option value="">选择标签...</option>';
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

    populateCategorySelects() {
        const categorySelects = [
            document.getElementById('doc-category'),
            document.getElementById('edit-doc-category'),
            document.getElementById('category-filter'),
            document.getElementById('search-category'),
            document.getElementById('create-doc-category') // 新增：创建表单分类选择器
        ];

        categorySelects.forEach(select => {
            if (select) {
                // 保留第一个选项（通常是"未分类"或"全部分类"）
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

    async loadDocuments(categoryId = null) {
        try {
            if (!authManager.isAuthenticated()) {
                console.warn('用户未登录，无法加载文档');
                return;
            }

            const userId = authManager.getCurrentUserId();
            let url = `/api/document/user/${userId}`;

            if (categoryId) {
                url += `?categoryId=${categoryId}`;
            }

            console.log('加载文档，URL:', url);
            const response = await axios.get(url);

            if (response.data.success) {
                this.documents = response.data.data || [];

                // 为每个文档加载标签
                for (let doc of this.documents) {
                    await this.loadDocumentTags(doc);
                }

                console.log('文档加载完成:', this.documents.length);
                this.displayDocuments();

                // 安全地触发文档列表加载事件
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

    // 安全地触发事件（防止document未定义）
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
            // 静默失败，不影响主要功能
        }
    }

    // 修复 safeTriggerDocumentLoaded 方法参数名
    safeTriggerDocumentLoaded(doc) {  // 注意：参数名改为doc
        try {
            if (typeof window !== 'undefined' && window.document && window.document.dispatchEvent) {
                const event = new CustomEvent('documentLoaded', {
                    detail: { document: doc }  // 注意：这里属性名还是document
                });
                window.document.dispatchEvent(event);
            }
        } catch (error) {
            console.warn('触发文档加载事件失败:', error);
            // 静默失败，不影响主要功能
        }
    }

    // 新增：加载文档标签
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

    // 修改：显示文档时显示标签，并为每个文档卡片添加data-document-id属性
    displayDocuments() {
        const container = document.getElementById('documents-list');
        if (!container) {
            console.error('文档容器未找到');
            return;
        }

        if (!this.documents || this.documents.length === 0) {
            container.innerHTML = '<div class="empty-state">暂无文档</div>';
            return;
        }

        // 生成文档卡片HTML，添加data-document-id属性
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
                <div class="doc-content-preview">${this.escapeHtml(doc.content ? doc.content.substring(0, 100) + '...' : '无内容')}</div>
                <div class="doc-actions">
                    <button onclick="documentManager.viewDocument(${doc.id})" class="btn-secondary">查看</button>
                    <button onclick="documentManager.editDocument(${doc.id})" class="btn-secondary">编辑</button>
                    <button onclick="documentManager.deleteDocument(${doc.id})" class="btn-danger">删除</button>
                </div>
            </div>
        `).join('');
    }

    // 新增：渲染文档标签
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

    // 修改：编辑文档，加载标签信息
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

    // 新增：加载文档及其标签
    async loadDocumentWithTags(documentId) {
        try {
            const userId = authManager.getCurrentUserId();

            // 获取文档基本信息
            const docResponse = await axios.get(`/api/document/${documentId}`, {
                params: { userId: userId }
            });

            if (!docResponse.data.success) {
                throw new Error(docResponse.data.message);
            }

            const document = docResponse.data.data;

            // 获取文档标签
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

    // ==================== 修复的核心：initEditor 方法 ====================
    async initEditor(elementId, content = '', mode = 'create') {
        console.log('🔄 初始化编辑器:', elementId, '模式:', mode);

        // 🔧 1. 智能内容清理
        let safeContent = this.cleanEditorContent(content);

        // 如果是污染内容被清空，给用户友好的提示
        if (!safeContent && content && content.length > 50) {
            safeContent = '⚠️ 此文档内容异常（可能由于编辑器故障）。\n请重新输入您的内容，系统已修复此问题。';
        }

        // 2. 销毁现有编辑器实例
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

        // 3. 等待DOM渲染
        await this.waitForElement(elementId);

        const editorElement = document.getElementById(elementId);
        if (!editorElement) {
            console.error('编辑器元素未找到:', elementId);
            return null;
        }

        try {
            // 清空容器并创建textarea
            editorElement.innerHTML = '<textarea class="editor-textarea"></textarea>';
            const textarea = editorElement.querySelector('textarea');
            textarea.value = safeContent;

            // 设置文本区域样式
            textarea.style.width = '100%';
            textarea.style.height = '400px';
            textarea.style.padding = '10px';
            textarea.style.border = '1px solid #ddd';
            textarea.style.fontFamily = 'monospace';
            textarea.style.resize = 'vertical';

            // 4. 检查是否应该使用EasyMDE
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

            // 5. 使用简单文本区域编辑器
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

            // 最终的fallback
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

    // 辅助方法：检查是否包含JS代码
    containsJsCode(content) {
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

    // 新增：等待元素渲染的辅助方法
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

    // ==================== 修复：内容获取方法 ====================
    getEditorContent(editorId) {
        const editor = this.editors[editorId];
        if (!editor) return '';

        try {
            let content = '';

            if (editor.isSimpleEditor) {
                // 简单编辑器
                content = editor.getContent();
            } else if (editor.isFallback) {
                // 后备编辑器
                content = editor.getContent();
            } else if (typeof editor.value === 'function') {
                // EasyMDE 实例
                content = editor.value();
            } else if (typeof editor.value === 'string') {
                // 字符串值
                content = editor.value;
            } else if (editor.codemirror) {
                // CodeMirror 实例
                content = editor.codemirror.getValue();
            } else {
                // 最后尝试从DOM获取
                const textarea = document.querySelector(`#${editorId} textarea`);
                content = textarea ? textarea.value : '';
            }

            // 最后的安全检查
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

    // 修改：编辑文档表单
    renderDocumentEditForm(docData) {
        const categoryOptions = this.categories.map(cat => `
            <option value="${cat.id}" ${docData.categoryId === cat.id ? 'selected' : ''}>
                ${this.escapeHtml(cat.name)}
            </option>
        `).join('');

        // 获取文档的标签ID
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

    // ==================== 修复：新建文档表单 ====================
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

            // 填充选择器选项
            this.populateCategorySelects();
            this.populateTagSelects();

            try {
                // 初始化编辑器
                await this.initEditor('create-doc-editor', '', 'create');
            } catch (error) {
                console.error('编辑器初始化失败:', error);
                // 后备方案
                const editorElement = document.getElementById('create-doc-editor');
                if (editorElement) {
                    editorElement.innerHTML = '<textarea style="width:100%;height:400px;padding:10px;border:1px solid #ddd;"></textarea>';
                }
            }

            // 设置表单提交事件
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

    // ==================== 修复：创建文档 ====================
    async handleCreateDocument() {
        try {
            const title = document.getElementById('doc-title').value;
            const categoryId = document.getElementById('doc-category').value || null;
            const tagSelect = document.getElementById('doc-tags');
            const tagIds = Array.from(tagSelect.selectedOptions)
                .map(option => option.value)
                .filter(id => id);

            // 🔧 使用修复后的内容获取方法
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

            // 1. 先创建文档
            const response = await axios.post('/api/document', documentData);

            if (response.data.success) {
                const createdDocument = response.data.data;

                // 2. 如果有标签，设置文档标签
                if (tagIds.length > 0) {
                    try {
                        await axios.post(`/api/tag/document/${createdDocument.id}/batch`, tagIds, {
                            params: { userId: userId }
                        });
                        console.log('文档标签设置成功');
                    } catch (tagError) {
                        console.error('设置文档标签失败:', tagError);
                        // 标签设置失败不影响文档创建
                    }
                }

                this.showSuccess('文档创建成功');
                // 关闭模态框
                const modal = document.querySelector('.modal');
                if (modal) {
                    modal.remove();
                }
                // 清理编辑器实例
                if (this.editors['create-doc-editor']) {
                    try {
                        if (this.editors['create-doc-editor'].destroy) {
                            this.editors['create-doc-editor'].destroy();
                        }
                    } catch (e) {
                        // 忽略错误
                    }
                    delete this.editors['create-doc-editor'];
                }
                // 重新加载文档列表
                await this.loadDocuments();
            } else {
                this.showError('创建文档失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('创建文档失败:', error);
            this.showError('创建文档失败: ' + error.message);
        }
    }

    // ==================== 修复：编辑文档 ====================
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

            // 🔧 使用修复后的内容获取方法
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

            // 1. 先更新文档
            const response = await axios.put(`/api/document/${documentId}`, documentData);

            if (response.data.success) {
                // 2. 设置文档标签
                try {
                    await axios.post(`/api/tag/document/${documentId}/batch`, tagIds, {
                        params: { userId: userId }
                    });
                    console.log('文档标签更新成功');
                } catch (tagError) {
                    console.error('更新文档标签失败:', tagError);
                    // 标签更新失败不影响文档更新
                }

                this.showSuccess('文档更新成功');
                // 关闭模态框
                const modal = document.querySelector('.modal');
                if (modal) {
                    modal.remove();
                }
                // 清理编辑器实例
                if (this.editors['edit-doc-editor']) {
                    try {
                        if (this.editors['edit-doc-editor'].destroy) {
                            this.editors['edit-doc-editor'].destroy();
                        }
                    } catch (e) {
                        // 忽略错误
                    }
                    delete this.editors['edit-doc-editor'];
                }
                // 重新加载文档列表
                await this.loadDocuments();
            } else {
                this.showError('更新文档失败: ' + response.data.message);
            }
        } catch (error) {
            console.error('更新文档失败:', error);
            this.showError('更新文档失败: ' + error.message);
        }
    }

    // 在 viewDocument 方法中，确保正确触发事件
    async viewDocument(documentId) {
        try {
            const doc = await this.loadDocumentWithTags(documentId);
            this.showDocumentViewModal(doc);
            // 安全地触发文档加载事件
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

            // 显示内容前先清理
            setTimeout(() => {
                const previewDiv = document.getElementById('markdown-preview');
                if (previewDiv && doc.content) {
                    // 清理可能污染的内容
                    const cleanContent = this.cleanEditorContent(doc.content);
                    let html = cleanContent || '此文档内容异常，请编辑修复';

                    // 简单的Markdown转换（如果内容正常）
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

    // 新增：为查看页面渲染标签
    renderDocumentTagsForView(tags) {
        if (!tags || tags.length === 0) {
            return '无标签';
        }
        return tags.map(tag => this.escapeHtml(tag.name)).join(', ');
    }

    // 删除文档方法（移动到回收站）
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

                // 安全地触发文档删除事件
                try {
                    if (typeof window !== 'undefined' && window.document && window.document.dispatchEvent) {
                        const event = new CustomEvent('documentDeleted', { detail: { docId } });
                        window.document.dispatchEvent(event);
                    }
                } catch (error) {
                    console.warn('触发文档删除事件失败:', error);
                }

                // 重新加载文档列表
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

    // 保留原始的永久删除方法（如果需要）
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
                // 填充选择器选项
                this.populateCategorySelects();
                this.populateTagSelects();

                // 初始化编辑器（异步确保DOM已渲染）
                setTimeout(async () => {
                    try {
                        await this.initEditor('edit-doc-editor', docData.content || '', 'edit');
                    } catch (error) {
                        console.error('编辑器初始化失败:', error);
                        // 使用普通textarea作为后备
                        const editorElement = document.getElementById('edit-doc-editor');
                        if (editorElement) {
                            editorElement.innerHTML = `<textarea style="width:100%;height:400px;padding:10px;border:1px solid #ddd;">${this.escapeHtml(docData.content || '')}</textarea>`;
                        }
                    }
                }, 100);

                // 设置表单提交事件
                const form = document.getElementById('edit-document-form');
                if (form) {
                    // 移除旧的监听器（如果存在）
                    if (this.editFormSubmitHandler) {
                        form.removeEventListener('submit', this.editFormSubmitHandler);
                    }

                    // 创建新的监听器
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

    setupCategoryFilter() {
        const categoryFilter = document.getElementById('category-filter');
        if (categoryFilter) {
            categoryFilter.addEventListener('change', (e) => {
                const categoryId = e.target.value;
                this.loadDocuments(categoryId || null);
            });
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

// 在初始化时设置分类筛选
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(() => {
        if (documentManager) {
            documentManager.initialize().then(() => {
                // 初始化完成后设置分类筛选
                documentManager.setupCategoryFilter();
            });
        }
    }, 100);
});

// 确保全局可访问
window.documentManager = documentManager;
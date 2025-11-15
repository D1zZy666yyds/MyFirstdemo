// 仪表盘组件
const DashboardSection = {
    template: `
        <div class="dashboard">
            <h2>仪表盘</h2>
            <div class="stats-grid">
                <div class="stat-card">
                    <h3>文档总数</h3>
                    <p class="stat-number">{{ stats.documentCount || 0 }}</p>
                </div>
                <div class="stat-card">
                    <h3>分类数量</h3>
                    <p class="stat-number">{{ stats.categoryCount || 0 }}</p>
                </div>
                <div class="stat-card">
                    <h3>标签数量</h3>
                    <p class="stat-number">{{ stats.tagCount || 0 }}</p>
                </div>
                <div class="stat-card">
                    <h3>收藏数量</h3>
                    <p class="stat-number">{{ stats.favoriteCount || 0 }}</p>
                </div>
            </div>
            
            <div class="recent-documents">
                <h3>最近文档</h3>
                <div v-if="recentDocuments.length === 0" class="empty-state">
                    暂无文档，<a href="#documents" @click="$parent.showSection('documents')">创建第一篇文档</a>
                </div>
                <div v-else class="document-list">
                    <div v-for="doc in recentDocuments" :key="doc.id" class="document-item">
                        <h4>{{ doc.title }}</h4>
                        <p class="doc-meta">{{ doc.date }} • {{ doc.category }}</p>
                    </div>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            stats: {},
            recentDocuments: []
        };
    },
    async mounted() {
        await this.loadDashboardData();
    },
    methods: {
        async loadDashboardData() {
            try {
                const result = await authApi.getDashboardStats();
                if (result.success) {
                    this.stats = result;
                    this.recentDocuments = result.recentDocuments || [];
                }
            } catch (error) {
                console.error('加载仪表盘数据失败:', error);
            }
        }
    }
};

// 文档管理组件
const DocumentsSection = {
    template: `
        <div class="documents-section">
            <div class="section-header">
                <h2>文档管理</h2>
                <button class="btn-primary" @click="showCreateForm = true">新建文档</button>
            </div>

            <!-- 创建文档表单 -->
            <div v-if="showCreateForm" class="modal-overlay">
                <div class="modal">
                    <h3>创建新文档</h3>
                    <form @submit.prevent="createDocument">
                        <div class="form-group">
                            <label>标题:</label>
                            <input type="text" v-model="newDocument.title" required>
                        </div>
                        <div class="form-group">
                            <label>分类:</label>
                            <select v-model="newDocument.categoryId">
                                <option value="">请选择分类</option>
                                <option v-for="category in categories" :key="category.id" :value="category.id">
                                    {{ category.name }}
                                </option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>内容:</label>
                            <textarea v-model="newDocument.content" rows="10" required></textarea>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn-primary">保存</button>
                            <button type="button" @click="showCreateForm = false" class="btn-secondary">取消</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- 文档列表 -->
            <div class="document-list">
                <div v-if="documents.length === 0" class="empty-state">
                    暂无文档，点击"新建文档"开始创建
                </div>
                <div v-else>
                    <div v-for="doc in documents" :key="doc.id" class="document-card">
                        <div class="doc-header">
                            <h3>{{ doc.title }}</h3>
                            <div class="doc-actions">
                                <button @click="editDocument(doc)" class="btn-small">编辑</button>
                                <button @click="deleteDocument(doc.id)" class="btn-small btn-danger">删除</button>
                            </div>
                        </div>
                        <p class="doc-content-preview">{{ doc.content.substring(0, 100) }}...</p>
                        <div class="doc-meta">
                            <span>分类: {{ getCategoryName(doc.categoryId) }}</span>
                            <span>创建时间: {{ formatDate(doc.createdTime) }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            showCreateForm: false,
            documents: [],
            categories: [],
            newDocument: {
                title: '',
                content: '',
                categoryId: '',
                userId: null
            }
        };
    },
    async mounted() {
        await this.loadData();
    },
    methods: {
        async loadData() {
            try {
                const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                this.newDocument.userId = currentUser.id;

                // 加载文档列表
                const docsResult = await documentApi.getUserDocuments(currentUser.id);
                if (docsResult.success) {
                    this.documents = docsResult.data;
                }

                // 加载分类列表
                const catsResult = await categoryApi.getUserCategories(currentUser.id);
                if (catsResult.success) {
                    this.categories = catsResult.data;
                }
            } catch (error) {
                console.error('加载数据失败:', error);
            }
        },

        async createDocument() {
            try {
                const result = await documentApi.createDocument(this.newDocument);
                if (result.success) {
                    alert('文档创建成功！');
                    this.showCreateForm = false;
                    this.newDocument = { title: '', content: '', categoryId: '', userId: this.newDocument.userId };
                    await this.loadData(); // 重新加载文档列表
                }
            } catch (error) {
                console.error('创建文档失败:', error);
                alert('创建文档失败: ' + error.message);
            }
        },

        async deleteDocument(documentId) {
            if (confirm('确定要删除这个文档吗？')) {
                try {
                    const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                    const result = await documentApi.deleteDocument(documentId, currentUser.id);
                    if (result.success) {
                        alert('文档删除成功！');
                        await this.loadData();
                    }
                } catch (error) {
                    console.error('删除文档失败:', error);
                    alert('删除文档失败: ' + error.message);
                }
            }
        },

        editDocument(doc) {
            // 跳转到编辑页面或打开编辑模态框
            alert('编辑功能开发中...');
        },

        getCategoryName(categoryId) {
            const category = this.categories.find(cat => cat.id === categoryId);
            return category ? category.name : '未分类';
        },

        formatDate(dateString) {
            return new Date(dateString).toLocaleDateString();
        }
    }
};

// 分类管理组件
const CategoriesSection = {
    template: `
        <div class="categories-section">
            <div class="section-header">
                <h2>分类管理</h2>
                <button class="btn-primary" @click="showCreateForm = true">新建分类</button>
            </div>

            <!-- 创建分类表单 -->
            <div v-if="showCreateForm" class="modal-overlay">
                <div class="modal">
                    <h3>创建新分类</h3>
                    <form @submit.prevent="createCategory">
                        <div class="form-group">
                            <label>分类名称:</label>
                            <input type="text" v-model="newCategory.name" required>
                        </div>
                        <div class="form-group">
                            <label>父分类:</label>
                            <select v-model="newCategory.parentId">
                                <option value="">无父分类（根分类）</option>
                                <option v-for="category in categories" :key="category.id" :value="category.id">
                                    {{ category.name }}
                                </option>
                            </select>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn-primary">保存</button>
                            <button type="button" @click="showCreateForm = false" class="btn-secondary">取消</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- 分类列表 -->
            <div class="category-tree">
                <div v-if="categories.length === 0" class="empty-state">
                    暂无分类，点击"新建分类"开始创建
                </div>
                <div v-else>
                    <div v-for="category in categories" :key="category.id" class="category-item">
                        <span>{{ category.name }}</span>
                        <div class="category-actions">
                            <button @click="deleteCategory(category.id)" class="btn-small btn-danger">删除</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            showCreateForm: false,
            categories: [],
            newCategory: {
                name: '',
                parentId: '',
                userId: null
            }
        };
    },
    async mounted() {
        await this.loadCategories();
    },
    methods: {
        async loadCategories() {
            try {
                const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                this.newCategory.userId = currentUser.id;

                const result = await categoryApi.getUserCategories(currentUser.id);
                if (result.success) {
                    this.categories = result.data;
                }
            } catch (error) {
                console.error('加载分类失败:', error);
            }
        },

        async createCategory() {
            try {
                const result = await categoryApi.createCategory(this.newCategory);
                if (result.success) {
                    alert('分类创建成功！');
                    this.showCreateForm = false;
                    this.newCategory = { name: '', parentId: '', userId: this.newCategory.userId };
                    await this.loadCategories();
                }
            } catch (error) {
                console.error('创建分类失败:', error);
                alert('创建分类失败: ' + error.message);
            }
        },

        async deleteCategory(categoryId) {
            if (confirm('确定要删除这个分类吗？分类下的文档将变为未分类状态。')) {
                try {
                    const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                    const result = await categoryApi.deleteCategory(categoryId, currentUser.id);
                    if (result.success) {
                        alert('分类删除成功！');
                        await this.loadCategories();
                    }
                } catch (error) {
                    console.error('删除分类失败:', error);
                    alert('删除分类失败: ' + error.message);
                }
            }
        }
    }
};

// 标签管理组件
const TagsSection = {
    template: `
        <div class="tags-section">
            <div class="section-header">
                <h2>标签管理</h2>
                <button class="btn-primary" @click="showCreateForm = true">新建标签</button>
            </div>

            <!-- 创建标签表单 -->
            <div v-if="showCreateForm" class="modal-overlay">
                <div class="modal">
                    <h3>创建新标签</h3>
                    <form @submit.prevent="createTag">
                        <div class="form-group">
                            <label>标签名称:</label>
                            <input type="text" v-model="newTag.name" required>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn-primary">保存</button>
                            <button type="button" @click="showCreateForm = false" class="btn-secondary">取消</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- 标签列表 -->
            <div class="tags-grid">
                <div v-if="tags.length === 0" class="empty-state">
                    暂无标签，点击"新建标签"开始创建
                </div>
                <div v-else class="tags-container">
                    <div v-for="tag in tags" :key="tag.id" class="tag-item">
                        <span class="tag-badge">{{ tag.name }}</span>
                        <button @click="deleteTag(tag.id)" class="btn-small btn-danger">删除</button>
                    </div>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            showCreateForm: false,
            tags: [],
            newTag: {
                name: '',
                userId: null
            }
        };
    },
    async mounted() {
        await this.loadTags();
    },
    methods: {
        async loadTags() {
            try {
                const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                this.newTag.userId = currentUser.id;

                const result = await tagApi.getUserTags(currentUser.id);
                if (result.success) {
                    this.tags = result.data;
                }
            } catch (error) {
                console.error('加载标签失败:', error);
            }
        },

        async createTag() {
            try {
                const result = await tagApi.createTag(this.newTag);
                if (result.success) {
                    alert('标签创建成功！');
                    this.showCreateForm = false;
                    this.newTag = { name: '', userId: this.newTag.userId };
                    await this.loadTags();
                }
            } catch (error) {
                console.error('创建标签失败:', error);
                alert('创建标签失败: ' + error.message);
            }
        },

        async deleteTag(tagId) {
            if (confirm('确定要删除这个标签吗？')) {
                try {
                    const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                    const result = await tagApi.deleteTag(tagId, currentUser.id);
                    if (result.success) {
                        alert('标签删除成功！');
                        await this.loadTags();
                    }
                } catch (error) {
                    console.error('删除标签失败:', error);
                    alert('删除标签失败: ' + error.message);
                }
            }
        }
    }
};

// 搜索组件
const SearchSection = {
    template: `
        <div class="search-section">
            <h2>文档搜索</h2>
            
            <div class="search-box">
                <input type="text" v-model="searchKeyword" @input="handleSearch" placeholder="输入关键词搜索文档...">
                <button @click="performSearch" class="btn-primary">搜索</button>
            </div>

            <div v-if="searchResults.length > 0" class="search-results">
                <h3>搜索结果 ({{ searchResults.length }} 个)</h3>
                <div v-for="doc in searchResults" :key="doc.id" class="search-result-item">
                    <h4>{{ doc.title }}</h4>
                    <p class="result-preview">{{ doc.content.substring(0, 150) }}...</p>
                    <div class="result-meta">
                        <span>分类: {{ doc.categoryName }}</span>
                        <span>创建时间: {{ formatDate(doc.createdTime) }}</span>
                    </div>
                </div>
            </div>

            <div v-else-if="hasSearched" class="empty-state">
                没有找到相关文档
            </div>
        </div>
    `,
    data() {
        return {
            searchKeyword: '',
            searchResults: [],
            hasSearched: false
        };
    },
    methods: {
        async performSearch() {
            if (!this.searchKeyword.trim()) {
                alert('请输入搜索关键词');
                return;
            }

            try {
                const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                const result = await searchApi.search(this.searchKeyword, currentUser.id, 20);

                if (result.success) {
                    this.searchResults = result.data;
                    this.hasSearched = true;
                }
            } catch (error) {
                console.error('搜索失败:', error);
                alert('搜索失败: ' + error.message);
            }
        },

        handleSearch() {
            // 可以在这里实现实时搜索
        },

        formatDate(dateString) {
            return new Date(dateString).toLocaleDateString();
        }
    }
};
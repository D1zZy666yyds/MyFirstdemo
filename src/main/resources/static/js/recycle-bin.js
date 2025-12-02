// recycle-bin.js - 修复版

class RecycleBinManager {
    constructor() {
        this.deletedDocuments = [];
        this.selectedItems = new Set();
        this.isInitialized = false;
        this.isLoading = false;
        this.stats = {
            total: 0,
            deletedToday: 0,
            deletedThisWeek: 0,
            totalSize: 0
        };
    }

    async initialize() {
        if (this.isInitialized) return;

        try {
            // 等待认证管理器就绪
            if (typeof authManager === 'undefined') {
                setTimeout(() => this.initialize(), 500);
                return;
            }

            const isAuthenticated = await authManager.checkAuthStatus();
            if (!isAuthenticated) {
                console.warn('用户未登录，回收站功能暂不可用');
                return;
            }

            this.createRecycleBinPage();
            this.bindEvents();
            this.isInitialized = true;

            // 如果当前在回收站页面，直接加载数据
            if (window.location.hash === '#recycle-bin') {
                await this.loadRecycleBin();
            }

        } catch (error) {
            console.error('回收站管理器初始化失败:', error);
        }
    }

    createRecycleBinPage() {
        const mainContent = document.querySelector('.main-content');
        if (!mainContent) return;

        if (document.getElementById('recycle-bin-page')) return;

        const recycleBinPage = document.createElement('div');
        recycleBinPage.id = 'recycle-bin-page';
        recycleBinPage.className = 'page recycle-bin-page';
        recycleBinPage.innerHTML = `
            <div class="recycle-bin-header">
                <h2>
                    <i class="recycle-icon">🗑️</i>
                    回收站
                    <span id="refresh-status" class="refresh-status"></span>
                </h2>
                <div class="header-actions">
                    <button class="btn-secondary btn-refresh" title="刷新">
                        ↻ 刷新
                    </button>
                    <button class="btn-danger btn-clear-all" id="clear-all-btn">
                        🗑️ 清空回收站
                    </button>
                </div>
            </div>

            <div id="batch-actions" class="batch-actions" style="display: none;">
                <div class="batch-info">
                    <span>已选中 <span id="batch-count" class="batch-count">0</span> 个项目</span>
                    <button class="btn-small btn-clear-selection">取消选择</button>
                </div>
                <div class="batch-buttons">
                    <button class="btn-batch btn-restore-selected restore-all">
                        ↺ 恢复选中
                    </button>
                    <button class="btn-batch btn-delete-selected delete-all">
                        🗑️ 彻底删除
                    </button>
                </div>
            </div>

            <div class="recycle-bin-stats" id="recycle-stats"></div>

            <div id="recycle-bin-content" class="recycle-bin-content">
                <div class="loading-state">
                    <div class="spinner"></div>
                    <p>正在加载回收站数据...</p>
                </div>
            </div>
        `;

        mainContent.appendChild(recycleBinPage);
    }

    bindEvents() {
        // 页面切换事件
        document.addEventListener('click', (e) => {
            const recycleBinLink = e.target.closest('a[href="#recycle-bin"]');
            if (recycleBinLink) {
                e.preventDefault();
                this.showRecycleBinPage();
            }
        });

        // 刷新按钮
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-refresh')) {
                this.loadRecycleBin();
            }
        });

        // 清空回收站
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-clear-all')) {
                this.showClearAllConfirm();
            }
        });

        // 清除选择
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-clear-selection')) {
                this.clearSelection();
            }
        });

        // 批量恢复
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-restore-selected')) {
                this.batchRestore();
            }
        });

        // 批量删除
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-delete-selected')) {
                this.batchDelete();
            }
        });

        // 监听文档删除事件
        document.addEventListener('documentDeleted', () => {
            this.updateBadgeCount();
        });
    }

    async showRecycleBinPage() {
        this.hideAllPages();
        const page = document.getElementById('recycle-bin-page');
        if (page) {
            page.classList.add('active');
            this.updateActiveNav('#recycle-bin');
            await this.loadRecycleBin();
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

    async loadRecycleBin() {
        if (this.isLoading) return;

        try {
            this.isLoading = true;
            this.showLoadingState();

            const userId = this.getCurrentUserId();
            if (!userId) {
                this.showError('请先登录');
                return;
            }

            // 更新刷新状态
            this.updateRefreshStatus('正在加载...');

            const response = await axios.get(`/api/document/deleted/${userId}`);

            if (response.data.success) {
                this.deletedDocuments = response.data.data || [];
                this.updateRecycleBinDisplay();
                this.updateStats();
                this.updateBadgeCount();
                this.updateRefreshStatus('已更新');
            } else {
                throw new Error(response.data.message || '加载失败');
            }
        } catch (error) {
            console.error('加载回收站失败:', error);
            this.showError('加载回收站失败: ' + this.getErrorMessage(error));
            this.showEmptyState();
        } finally {
            this.isLoading = false;
        }
    }

    showLoadingState() {
        const container = document.getElementById('recycle-bin-content');
        if (!container) return;

        container.innerHTML = `
            <div class="loading-state">
                <div class="spinner"></div>
                <p>正在加载回收站数据...</p>
            </div>
        `;
    }

    updateRefreshStatus(text) {
        const statusEl = document.getElementById('refresh-status');
        if (statusEl) {
            statusEl.textContent = text;
            setTimeout(() => {
                statusEl.textContent = '';
            }, 2000);
        }
    }

    updateRecycleBinDisplay() {
        const container = document.getElementById('recycle-bin-content');
        if (!container) return;

        if (!this.deletedDocuments.length) {
            this.showEmptyState();
            document.getElementById('clear-all-btn').style.display = 'none';
            return;
        }

        document.getElementById('clear-all-btn').style.display = 'inline-block';

        let html = `
            <div class="recycle-bin-table">
                <table>
                    <thead>
                        <tr>
                            <th width="40">
                                <input type="checkbox" id="select-all">
                            </th>
                            <th>文档信息</th>
                            <th width="120">删除时间</th>
                            <th width="150">操作</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        this.deletedDocuments.forEach(doc => {
            const isSelected = this.selectedItems.has(doc.id);
            const deletedTime = this.formatTime(doc.deletedTime || doc.updatedTime);

            html += `
                <tr data-doc-id="${doc.id}" class="${isSelected ? 'selected' : ''}">
                    <td>
                        <input type="checkbox" 
                               ${isSelected ? 'checked' : ''}
                               data-doc-id="${doc.id}">
                    </td>
                    <td>
                        <div class="doc-info">
                            <div class="doc-title" title="${this.escapeHtml(doc.title)}">
                                ${this.escapeHtml(doc.title)}
                            </div>
                            <div class="doc-meta">
                                <span>📁 ${doc.categoryName || '未分类'}</span>
                                <span>📝 ${doc.contentType || '文档'}</span>
                                <span>📅 ${this.formatTime(doc.createdTime)}</span>
                            </div>
                            ${doc.tags?.length ? `
                                <div class="doc-tags">
                                    ${doc.tags.map(tag =>
                `<span class="doc-tag">${this.escapeHtml(tag)}</span>`
            ).join('')}
                                </div>
                            ` : ''}
                        </div>
                    </td>
                    <td>${deletedTime}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="btn-action btn-restore" data-doc-id="${doc.id}">
                                ↺ 恢复
                            </button>
                            <button class="btn-action btn-delete" data-doc-id="${doc.id}">
                                🗑️ 删除
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        container.innerHTML = html;
        this.bindTableEvents();
        this.updateBatchActions();
    }

    bindTableEvents() {
        // 全选/取消全选
        const selectAll = document.getElementById('select-all');
        if (selectAll) {
            selectAll.addEventListener('change', (e) => {
                const checkboxes = document.querySelectorAll('#recycle-bin-content input[type="checkbox"][data-doc-id]');
                checkboxes.forEach(checkbox => {
                    checkbox.checked = e.target.checked;
                    const docId = parseInt(checkbox.dataset.docId);
                    if (e.target.checked) {
                        this.selectedItems.add(docId);
                    } else {
                        this.selectedItems.delete(docId);
                    }
                });
                this.updateBatchActions();
                this.updateRowSelection();
            });
        }

        // 单个选择
        document.querySelectorAll('#recycle-bin-content input[type="checkbox"][data-doc-id]').forEach(checkbox => {
            checkbox.addEventListener('change', (e) => {
                const docId = parseInt(e.target.dataset.docId);
                if (e.target.checked) {
                    this.selectedItems.add(docId);
                } else {
                    this.selectedItems.delete(docId);
                }
                this.updateBatchActions();
                this.updateRowSelection();
            });
        });

        // 恢复按钮
        document.querySelectorAll('.btn-restore').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const docId = parseInt(e.target.dataset.docId);
                this.restoreDocument(docId);
            });
        });

        // 删除按钮
        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const docId = parseInt(e.target.dataset.docId);
                this.permanentDelete(docId);
            });
        });
    }

    updateRowSelection() {
        document.querySelectorAll('#recycle-bin-content tbody tr').forEach(row => {
            const docId = parseInt(row.dataset.docId);
            if (this.selectedItems.has(docId)) {
                row.classList.add('selected');
            } else {
                row.classList.remove('selected');
            }
        });
    }

    showEmptyState() {
        const container = document.getElementById('recycle-bin-content');
        if (!container) return;

        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">🗑️</div>
                <h3>回收站是空的</h3>
                <p>已删除的文档将在这里显示</p>
                <button class="btn-secondary btn-refresh-empty">刷新查看</button>
            </div>
        `;

        document.querySelector('.btn-refresh-empty')?.addEventListener('click', () => {
            this.loadRecycleBin();
        });

        document.getElementById('recycle-stats').innerHTML = '';
        document.getElementById('batch-actions').style.display = 'none';
    }

    updateStats() {
        const statsContainer = document.getElementById('recycle-stats');
        if (!statsContainer) return;

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const thisWeek = new Date();
        thisWeek.setDate(thisWeek.getDate() - 7);

        this.stats.total = this.deletedDocuments.length;
        this.stats.deletedToday = this.deletedDocuments.filter(doc => {
            const deletedDate = new Date(doc.deletedTime || doc.updatedTime);
            return deletedDate >= today;
        }).length;

        this.stats.deletedThisWeek = this.deletedDocuments.filter(doc => {
            const deletedDate = new Date(doc.deletedTime || doc.updatedTime);
            return deletedDate >= thisWeek;
        }).length;

        this.stats.totalSize = this.calculateTotalSize();

        statsContainer.innerHTML = `
            <div class="stat-item">
                <h3>总项目数</h3>
                <div class="stat-number">${this.stats.total}</div>
            </div>
            <div class="stat-item">
                <h3>今日删除</h3>
                <div class="stat-number">${this.stats.deletedToday}</div>
            </div>
            <div class="stat-item">
                <h3>本周删除</h3>
                <div class="stat-number">${this.stats.deletedThisWeek}</div>
            </div>
            <div class="stat-item">
                <h3>占用空间</h3>
                <div class="stat-number">${this.formatSize(this.stats.totalSize)}</div>
            </div>
        `;
    }

    calculateTotalSize() {
        return this.deletedDocuments.length * 1024;
    }

    updateBatchActions() {
        const batchActions = document.getElementById('batch-actions');
        const batchCount = document.getElementById('batch-count');

        if (!batchActions || !batchCount) return;

        const count = this.selectedItems.size;
        if (count > 0) {
            batchCount.textContent = count;
            batchActions.style.display = 'flex';
        } else {
            batchActions.style.display = 'none';
        }
    }

    clearSelection() {
        this.selectedItems.clear();
        document.querySelectorAll('#recycle-bin-content input[type="checkbox"]').forEach(cb => {
            cb.checked = false;
        });
        this.updateBatchActions();
        this.updateRowSelection();
    }

    async restoreDocument(docId) {
        if (!await this.showConfirm('确定要恢复这个文档吗？')) return;

        try {
            const userId = this.getCurrentUserId();
            if (!userId) {
                this.showError('请先登录');
                return;
            }

            // 修复API路径
            const response = await axios.put(`/api/document/restore/${docId}?userId=${userId}`);

            if (response.data.success) {
                this.showSuccess('文档恢复成功');
                await this.loadRecycleBin();
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('恢复文档失败:', error);
            this.showError('恢复文档失败: ' + this.getErrorMessage(error));
        }
    }

    async permanentDelete(docId) {
        if (!await this.showConfirm('确定要彻底删除这个文档吗？此操作不可撤销！', 'warning')) return;

        try {
            const userId = this.getCurrentUserId();
            if (!userId) {
                this.showError('请先登录');
                return;
            }

            // 修复API路径
            const response = await axios.delete(`/api/document/permanent/${docId}?userId=${userId}`);

            if (response.data.success) {
                this.showSuccess('文档已彻底删除');
                await this.loadRecycleBin();
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('彻底删除失败:', error);
            this.showError('彻底删除失败: ' + this.getErrorMessage(error));
        }
    }

    async batchRestore() {
        const count = this.selectedItems.size;
        if (count === 0) return;

        if (!await this.showConfirm(`确定要恢复选中的 ${count} 个文档吗？`)) return;

        try {
            const userId = this.getCurrentUserId();
            const results = await Promise.allSettled(
                Array.from(this.selectedItems).map(docId =>
                    axios.put(`/api/document/restore/${docId}?userId=${userId}`)
                )
            );

            const successCount = results.filter(r => r.status === 'fulfilled' && r.value.data.success).length;
            const failedCount = results.length - successCount;

            if (failedCount > 0) {
                this.showInfo(`批量恢复完成：成功 ${successCount} 个，失败 ${failedCount} 个`);
            } else {
                this.showSuccess(`成功恢复 ${successCount} 个文档`);
            }

            await this.loadRecycleBin();
            this.clearSelection();

        } catch (error) {
            console.error('批量恢复失败:', error);
            this.showError('批量恢复失败: ' + this.getErrorMessage(error));
        }
    }

    async batchDelete() {
        const count = this.selectedItems.size;
        if (count === 0) return;

        if (!await this.showConfirm(`确定要彻底删除选中的 ${count} 个文档吗？此操作不可撤销！`, 'warning')) return;

        try {
            const userId = this.getCurrentUserId();
            const results = await Promise.allSettled(
                Array.from(this.selectedItems).map(docId =>
                    axios.delete(`/api/document/permanent/${docId}?userId=${userId}`)
                )
            );

            const successCount = results.filter(r => r.status === 'fulfilled' && r.value.data.success).length;
            const failedCount = results.length - successCount;

            if (failedCount > 0) {
                this.showInfo(`批量删除完成：成功 ${successCount} 个，失败 ${failedCount} 个`);
            } else {
                this.showSuccess(`成功删除 ${successCount} 个文档`);
            }

            await this.loadRecycleBin();
            this.clearSelection();

        } catch (error) {
            console.error('批量删除失败:', error);
            this.showError('批量删除失败: ' + this.getErrorMessage(error));
        }
    }

    async clearRecycleBin() {
        try {
            const userId = this.getCurrentUserId();
            if (!userId) {
                this.showError('请先登录');
                return;
            }

            // 修复API路径
            const response = await axios.delete('/api/document/recycle-bin/clear', {
                params: { userId: userId }
            });

            if (response.data.success) {
                this.showSuccess('回收站已清空');
                await this.loadRecycleBin();
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('清空回收站失败:', error);
            this.showError('清空回收站失败: ' + this.getErrorMessage(error));
        }
    }

    async showClearAllConfirm() {
        if (this.deletedDocuments.length === 0) {
            this.showInfo('回收站已经是空的');
            return;
        }

        if (await this.showConfirm(`确定要清空整个回收站吗？将彻底删除 ${this.deletedDocuments.length} 个项目，此操作不可撤销！`, 'warning')) {
            await this.clearRecycleBin();
        }
    }

    getCurrentUserId() {
        return authManager?.getCurrentUserId?.() || localStorage.getItem('userId');
    }

    formatTime(timeString) {
        if (!timeString) return '未知时间';

        const date = new Date(timeString);
        const now = new Date();
        const diff = now - date;
        const diffDays = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (diff < 60 * 1000) return '刚刚';
        if (diff < 3600 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`;
        if (diff < 24 * 3600 * 1000) return `${Math.floor(diff / (3600 * 1000))}小时前`;
        if (diffDays === 1) return '昨天';
        if (diffDays < 7) return `${diffDays}天前`;

        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    formatSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    getErrorMessage(error) {
        if (error.response?.data?.message) return error.response.data.message;
        if (error.message) return error.message;
        return '未知错误';
    }

    async showConfirm(message, type = 'info') {
        return new Promise((resolve) => {
            const confirmed = confirm((type === 'warning' ? '⚠️ ' : '') + message);
            resolve(confirmed);
        });
    }

    updateBadgeCount() {
        const badge = document.getElementById('recycle-bin-badge');
        if (!badge) return;

        const count = this.deletedDocuments.length;
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }

    showError(message) {
        console.error('回收站错误:', message);
        alert('❌ ' + message);
    }

    showSuccess(message) {
        console.log('回收站成功:', message);
        alert('✅ ' + message);
    }

    showInfo(message) {
        console.log('回收站信息:', message);
        alert('ℹ️ ' + message);
    }
}

// 创建全局实例
const recycleBinManager = new RecycleBinManager();

// 初始化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => recycleBinManager.initialize(), 500);
    });
} else {
    setTimeout(() => recycleBinManager.initialize(), 500);
}
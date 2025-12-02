/**
 * 知识图谱管理器
 */
class KnowledgeGraphManager {
    constructor() {
        this.chart = null;
        this.currentGraphData = null;
        this.currentGraphType = 'full';
        this.currentLayout = 'force';
        this.initialized = false;
        // 不自动初始化，等待页面切换时再初始化
    }

    // 页面显示时初始化
    async init() {
        console.log('初始化知识图谱管理器');

        // 先初始化图表
        this.initChart();

        // 延迟加载数据
        await this.delayedLoadGraphData();
    }

    // 延迟加载图谱数据，确保认证完成
    async delayedLoadGraphData() {
        console.log('开始延迟加载图谱数据...');

        // 等待主应用初始化完成
        if (!window.app) {
            console.log('等待主应用初始化...');
            setTimeout(() => {
                this.delayedLoadGraphData();
            }, 500);
            return;
        }

        // 检查认证状态
        try {
            const isAuthenticated = await authManager.checkAuthStatus();
            if (isAuthenticated) {
                console.log('用户已认证，加载图谱数据');
                await this.loadGraphData();
                this.initialized = true;
            } else {
                console.log('用户未认证，等待认证完成');
                setTimeout(() => {
                    this.delayedLoadGraphData();
                }, 1000);
            }
        } catch (error) {
            console.error('检查认证状态失败:', error);
            setTimeout(() => {
                this.delayedLoadGraphData();
            }, 2000);
        }
    }

    initChart() {
        const chartDom = document.getElementById('knowledge-graph');
        if (!chartDom) {
            console.error('知识图谱容器未找到');
            return;
        }

        // 初始化ECharts实例
        this.chart = echarts.init(chartDom);

        // 设置默认配置
        const option = {
            tooltip: {
                trigger: 'item',
                formatter: function (params) {
                    if (params.dataType === 'node') {
                        return `
                            <div style="padding: 8px;">
                                <strong>${params.data.name}</strong><br/>
                                <span style="color: #666;">类型: ${params.data.category || '未知'}</span>
                            </div>
                        `;
                    }
                    // 修复线条显示问题
                    if (params.dataType === 'edge') {
                        const sourceName = params.data.sourceName || params.data.source;
                        const targetName = params.data.targetName || params.data.target;
                        const relationName = params.data.name || params.data.value || '关联';
                        return `
                            <div style="padding: 8px;">
                                <strong>${relationName}</strong><br/>
                                <span style="color: #666;">${sourceName} → ${targetName}</span>
                            </div>
                        `;
                    }
                    return '';
                }
            },
            legend: {
                data: ['文档', '分类', '标签'],
                textStyle: { color: '#666' },
                top: 10
            },
            animation: true,
            series: [{
                type: 'graph',
                layout: 'force',
                symbolSize: 50,
                roam: true,
                focusNodeAdjacency: true,
                label: {
                    show: true,
                    position: 'right',
                    formatter: '{b}',
                    fontSize: 12
                },
                edgeLabel: {
                    show: true,
                    formatter: '{c}',
                    fontSize: 10
                },
                force: {
                    repulsion: 100,
                    gravity: 0.1,
                    edgeLength: 100
                },
                data: [],
                links: [],
                categories: [
                    { name: '文档', itemStyle: { color: '#91cc75' } },
                    { name: '分类', itemStyle: { color: '#5470c6' } },
                    { name: '标签', itemStyle: { color: '#fac858' } }
                ],
                lineStyle: {
                    color: 'source',
                    curveness: 0.3
                },
                emphasis: {
                    focus: 'adjacency',
                    lineStyle: {
                        width: 3
                    }
                }
            }]
        };

        this.chart.setOption(option);

        // 添加点击事件
        this.chart.on('click', (params) => {
            if (params.dataType === 'node') {
                this.showNodeDetails(params.data);
            }
        });

        // 窗口调整大小时重绘图表
        window.addEventListener('resize', () => {
            if (this.chart) {
                this.chart.resize();
            }
        });
    }

    async loadGraphData() {
        try {
            // 确保用户已登录
            if (!authManager.isAuthenticated()) {
                console.log('用户未登录，等待认证...');
                return;
            }

            const userId = authManager.getCurrentUserId();
            console.log('加载知识图谱数据，用户ID:', userId);

            this.showLoading(true);
            const response = await axios.get(`/api/knowledge-graph/full/${userId}`);

            console.log('知识图谱API响应:', response.data);

            if (response.data.success) {
                this.currentGraphData = response.data.data;
                this.renderGraph();
                this.updateStats();
                this.loadGraphAnalysis();
                this.initialized = true;
            } else {
                throw new Error(response.data.message);
            }
        } catch (error) {
            console.error('加载知识图谱数据失败:', error);

            // 如果是认证问题，等待重试
            if (error.message.includes('用户未登录')) {
                console.log('认证失败，等待重试...');
                setTimeout(() => {
                    this.delayedLoadGraphData();
                }, 2000);
            } else {
                this.showError('加载知识图谱失败: ' + error.message);
            }
        } finally {
            this.showLoading(false);
        }
    }

    renderGraph() {
        if (!this.currentGraphData || !this.chart) return;

        try {
            // 确保节点数据有正确的 category 字段
            const safeNodes = (this.currentGraphData.nodes || []).map(node => {
                return {
                    ...node,
                    category: node.category || this.determineCategory(node.id)
                };
            });

            // 重新构建完整的配置选项，确保数据结构正确
            const option = {
                tooltip: {
                    trigger: 'item',
                    formatter: function (params) {
                        if (params.dataType === 'node') {
                            return `
                                <div style="padding: 8px;">
                                    <strong>${params.data.name}</strong><br/>
                                    <span style="color: #666;">类型: ${params.data.category || '未知'}</span>
                                </div>
                            `;
                        }
                        // 修复线条显示问题
                        if (params.dataType === 'edge') {
                            const sourceName = params.data.sourceName || params.data.source;
                            const targetName = params.data.targetName || params.data.target;
                            const relationName = params.data.name || params.data.value || '关联';
                            return `
                                <div style="padding: 8px;">
                                    <strong>${relationName}</strong><br/>
                                    <span style="color: #666;">${sourceName} → ${targetName}</span>
                                </div>
                            `;
                        }
                        return '';
                    }
                },
                legend: {
                    data: ['文档', '分类', '标签'],
                    textStyle: { color: '#666' },
                    top: 10
                },
                animation: true,
                series: [{
                    type: 'graph',
                    layout: this.currentLayout,
                    symbolSize: (data) => {
                        return this.getSymbolSize(data);
                    },
                    roam: true,
                    focusNodeAdjacency: true,
                    label: {
                        show: true,
                        position: 'right',
                        formatter: '{b}',
                        fontSize: 12
                    },
                    edgeLabel: {
                        show: true,
                        formatter: '{c}',
                        fontSize: 10
                    },
                    force: {
                        repulsion: 100,
                        gravity: 0.1,
                        edgeLength: 100
                    },
                    data: safeNodes,
                    links: this.currentGraphData.links || [],
                    categories: [
                        { name: '文档', itemStyle: { color: '#91cc75' } },
                        { name: '分类', itemStyle: { color: '#5470c6' } },
                        { name: '标签', itemStyle: { color: '#fac858' } }
                    ],
                    lineStyle: {
                        color: 'source',
                        curveness: 0.3
                    },
                    emphasis: {
                        focus: 'adjacency',
                        lineStyle: {
                            width: 3
                        }
                    }
                }]
            };

            // 使用新的配置，不合并旧配置
            this.chart.setOption(option, {
                notMerge: true,
                lazyUpdate: false
            });

            // 更新统计信息
            this.updateStats();
        } catch (error) {
            console.error('渲染图表失败:', error);
            this.showError('渲染图表失败: ' + error.message);
        }
    }

    // 获取符号大小 - 修复 category 读取问题
    getSymbolSize(data) {
        if (!data) {
            return 25; // 默认大小
        }

        const category = data.category ? data.category.toString() : this.determineCategory(data.id);

        switch (this.currentGraphType) {
            case 'full':
                if (category === '分类') return 40;
                if (category === '文档') return 30;
                if (category === '标签') return 25;
                return 25;
            case 'documents':
                return 35;
            case 'tags':
                return Math.max(20, Math.min(60, (data.value || 1) * 2));
            case 'learning':
                return 20 + ((data.index || 0) * 2);
            default:
                return 25;
        }
    }

    // 根据节点ID确定分类
    determineCategory(nodeId) {
        if (typeof nodeId === 'string') {
            if (nodeId.startsWith('category_')) return '分类';
            if (nodeId.startsWith('document_')) return '文档';
            if (nodeId.startsWith('tag_')) return '标签';
            if (nodeId.startsWith('doc_')) return '文档';
        }
        return '未知';
    }

    adjustGraphStyle(option) {
        if (!option || !option.series || !option.series[0]) return;

        // 使用统一的 symbolSize 函数
        option.series[0].symbolSize = (data) => {
            return this.getSymbolSize(data);
        };
    }

    async changeGraphType() {
        const graphTypeSelect = document.getElementById('graph-type');
        if (!graphTypeSelect) return;

        const graphType = graphTypeSelect.value;
        this.currentGraphType = graphType;

        try {
            const userId = authManager.getCurrentUserId();
            let endpoint = '';

            switch (graphType) {
                case 'full':
                    endpoint = `/api/knowledge-graph/full/${userId}`;
                    break;
                case 'documents':
                    endpoint = `/api/knowledge-graph/document-relations/${userId}`;
                    break;
                case 'tags':
                    const tagResponse = await axios.get(`/api/knowledge-graph/tag-cloud/${userId}`);
                    if (tagResponse.data.success) {
                        this.renderTagCloud(tagResponse.data.data);
                    }
                    return;
                case 'learning':
                    endpoint = `/api/knowledge-graph/learning-path/${userId}`;
                    break;
            }

            if (endpoint) {
                this.showLoading(true);
                const response = await axios.get(endpoint);
                if (response.data.success) {
                    this.currentGraphData = response.data.data;
                    this.renderGraph();
                }
            }
        } catch (error) {
            console.error('切换图谱类型失败:', error);
            this.showError('切换失败: ' + error.message);
        } finally {
            this.showLoading(false);
        }
    }

    renderTagCloud(tagData) {
        if (!this.chart) return;

        const option = {
            tooltip: {
                show: true,
                formatter: function (params) {
                    return `${params.name}: ${params.value}个文档`;
                }
            },
            series: [{
                type: 'wordCloud',
                shape: 'circle',
                sizeRange: [12, 60],
                rotationRange: [0, 0],
                gridSize: 8,
                drawOutOfBound: false,
                textStyle: {
                    color: function () {
                        return 'rgb(' + [
                            Math.round(Math.random() * 160),
                            Math.round(Math.random() * 160),
                            Math.round(Math.random() * 160)
                        ].join(',') + ')';
                    }
                },
                data: tagData
            }]
        };

        this.chart.setOption(option, {
            notMerge: true,
            lazyUpdate: false
        });
    }

    changeLayout() {
        const layoutSelect = document.getElementById('graph-layout');
        if (!layoutSelect) return;

        this.currentLayout = layoutSelect.value;
        this.renderGraph();
    }

    searchNodes(keyword) {
        if (!this.currentGraphData || !this.chart || !keyword) {
            this.renderGraph();
            return;
        }

        const filteredNodes = this.currentGraphData.nodes.filter(node =>
            node.name && node.name.toLowerCase().includes(keyword.toLowerCase())
        );

        const filteredLinks = this.currentGraphData.links.filter(link =>
            filteredNodes.some(node => node.id === link.source) &&
            filteredNodes.some(node => node.id === link.target)
        );

        // 创建新的配置而不是修改现有配置
        const option = {
            series: [{
                data: filteredNodes,
                links: filteredLinks
            }]
        };

        this.chart.setOption(option, {
            notMerge: false,
            lazyUpdate: true
        });

        this.updateStats(filteredNodes.length, filteredLinks.length);
    }

    showNodeDetails(nodeData) {
        const detailsContainer = document.getElementById('node-details');
        if (!detailsContainer) return;

        let detailsHtml = '';

        const category = nodeData.category || this.determineCategory(nodeData.id);

        switch (category) {
            case '文档':
                detailsHtml = this.getDocumentDetails(nodeData);
                break;
            case '分类':
                detailsHtml = this.getCategoryDetails(nodeData);
                break;
            case '标签':
                detailsHtml = this.getTagDetails(nodeData);
                break;
            default:
                detailsHtml = this.getDefaultDetails(nodeData);
        }

        detailsContainer.innerHTML = detailsHtml;
    }

    getDocumentDetails(nodeData) {
        const docId = nodeData.id.replace('document_', '').replace('doc_', '');
        return `
            <div class="node-info">
                <div class="node-title">${this.escapeHtml(nodeData.name)}</div>
                <div class="node-meta">
                    <div><strong>类型:</strong> 文档</div>
                </div>
                <div class="node-actions">
                    <button onclick="knowledgeGraphManager.viewDocument(${docId})" class="btn-small">查看文档</button>
                    <button onclick="knowledgeGraphManager.editDocument(${docId})" class="btn-small">编辑文档</button>
                </div>
            </div>
        `;
    }

    getCategoryDetails(nodeData) {
        const categoryId = nodeData.id.replace('category_', '');
        return `
            <div class="node-info">
                <div class="node-title">${this.escapeHtml(nodeData.name)}</div>
                <div class="node-meta">
                    <div><strong>类型:</strong> 分类</div>
                </div>
                <div class="node-actions">
                    <button onclick="knowledgeGraphManager.viewCategoryDocuments(${categoryId})" class="btn-small">查看文档</button>
                </div>
            </div>
        `;
    }

    getTagDetails(nodeData) {
        const tagId = nodeData.id.replace('tag_', '');
        return `
            <div class="node-info">
                <div class="node-title">${this.escapeHtml(nodeData.name)}</div>
                <div class="node-meta">
                    <div><strong>类型:</strong> 标签</div>
                </div>
                <div class="node-actions">
                    <button onclick="knowledgeGraphManager.viewTagDocuments(${tagId})" class="btn-small">查看文档</button>
                </div>
            </div>
        `;
    }

    getDefaultDetails(nodeData) {
        const category = nodeData.category || this.determineCategory(nodeData.id);
        return `
            <div class="node-info">
                <div class="node-title">${this.escapeHtml(nodeData.name)}</div>
                <div class="node-meta">
                    <div><strong>类型:</strong> ${category}</div>
                </div>
            </div>
        `;
    }

    // 修复：查看文档
    async viewDocument(documentId) {
        try {
            console.log('查看文档，文档ID:', documentId);

            // 检查文档管理器是否存在
            if (!window.documentManager) {
                console.log('文档管理器未加载，跳转到文档页面...');
                window.location.hash = 'documents';

                setTimeout(async () => {
                    try {
                        // 等待文档管理器初始化
                        await waitForElementGlobal('#documents-list', 10, 100);

                        if (window.documentManager) {
                            if (!window.documentManager.isInitialized) {
                                await window.documentManager.initialize();
                            }
                            await window.documentManager.viewDocument(documentId);
                        } else {
                            alert('文档管理器未加载，请刷新页面');
                        }
                    } catch (error) {
                        console.error('查看文档失败:', error);
                        alert('查看文档失败: ' + error.message);
                    }
                }, 300);
                return;
            }

            // 文档管理器已存在，直接调用
            if (!window.documentManager.isInitialized) {
                await window.documentManager.initialize();
            }

            await window.documentManager.viewDocument(documentId);
        } catch (error) {
            console.error('查看文档失败:', error);
            this.showError('查看文档失败: ' + error.message);
        }
    }

    // 修复：编辑文档
    async editDocument(documentId) {
        try {
            console.log('编辑文档，文档ID:', documentId);

            // 检查文档管理器是否存在
            if (!window.documentManager) {
                console.log('文档管理器未加载，跳转到文档页面...');
                window.location.hash = 'documents';

                setTimeout(async () => {
                    try {
                        await waitForElementGlobal('#documents-list', 10, 100);

                        if (window.documentManager) {
                            if (!window.documentManager.isInitialized) {
                                await window.documentManager.initialize();
                            }
                            await window.documentManager.editDocument(documentId);
                        } else {
                            alert('文档管理器未加载，请刷新页面');
                        }
                    } catch (error) {
                        console.error('编辑文档失败:', error);
                        alert('编辑文档失败: ' + error.message);
                    }
                }, 300);
                return;
            }

            // 文档管理器已存在
            if (!window.documentManager.isInitialized) {
                await window.documentManager.initialize();
            }

            await window.documentManager.editDocument(documentId);
        } catch (error) {
            console.error('编辑文档失败:', error);
            this.showError('编辑文档失败: ' + error.message);
        }
    }

    // 修复：查看分类文档
    async viewCategoryDocuments(categoryId) {
        try {
            console.log('跳转到文档页面，分类ID:', categoryId);

            // 跳转到文档页面
            window.location.hash = 'documents';

            // 等待页面切换，然后设置分类筛选
            setTimeout(() => {
                // 使用全局函数来执行后续操作
                setCategoryFilterAndLoad(categoryId);
            }, 300);
        } catch (error) {
            console.error('跳转到分类文档失败:', error);
            this.showError('跳转失败: ' + error.message);
        }
    }

    // 修复：查看标签文档
    async viewTagDocuments(tagId) {
        try {
            console.log('跳转到搜索页面，标签ID:', tagId);

            // 跳转到搜索页面
            window.location.hash = 'search';

            // 等待页面切换，然后设置标签筛选
            setTimeout(() => {
                // 使用全局函数来执行后续操作
                setTagFilterAndSearch(tagId);
            }, 300);
        } catch (error) {
            console.error('跳转到标签文档失败:', error);
            this.showError('跳转失败: ' + error.message);
        }
    }

    updateStats(nodeCount = null, linkCount = null) {
        const nodeCountElement = document.getElementById('node-count');
        const linkCountElement = document.getElementById('link-count');

        if (nodeCountElement && linkCountElement) {
            const actualNodeCount = nodeCount !== null ? nodeCount : (this.currentGraphData?.nodes?.length || 0);
            const actualLinkCount = linkCount !== null ? linkCount : (this.currentGraphData?.links?.length || 0);

            nodeCountElement.textContent = `节点: ${actualNodeCount}`;
            linkCountElement.textContent = `关系: ${actualLinkCount}`;
        }
    }

    async loadGraphAnalysis() {
        try {
            const userId = authManager.getCurrentUserId();
            if (!userId) return;

            // 加载中心节点分析
            const centralResponse = await axios.get(`/api/knowledge-graph/central-nodes/${userId}`);
            if (centralResponse.data.success) {
                const centralNodes = centralResponse.data.data.slice(0, 3).map(node => node.title).join(', ');
                const centralNodesElement = document.getElementById('central-nodes');
                if (centralNodesElement) {
                    centralNodesElement.textContent = centralNodes || '无';
                }
            }

            // 加载关联密度
            const densityResponse = await axios.get(`/api/knowledge-graph/relation-density/${userId}`);
            if (densityResponse.data.success) {
                const relationDensityElement = document.getElementById('relation-density');
                if (relationDensityElement) {
                    relationDensityElement.textContent = densityResponse.data.data.density + '%';
                }
            }

            // 加载知识聚类
            const clusterResponse = await axios.get(`/api/knowledge-graph/knowledge-clusters/${userId}`);
            if (clusterResponse.data.success) {
                const knowledgeClustersElement = document.getElementById('knowledge-clusters');
                if (knowledgeClustersElement) {
                    knowledgeClustersElement.textContent = clusterResponse.data.data.totalClusters + ' 个';
                }
            }

        } catch (error) {
            console.error('加载图谱分析失败:', error);
        }
    }

    async rebuildGraph() {
        try {
            const userId = authManager.getCurrentUserId();
            if (!userId) {
                this.showError('用户未登录');
                return;
            }

            if (!confirm('确定要重建知识图谱吗？这可能需要一些时间。')) {
                return;
            }

            this.showLoading(true);
            const response = await axios.post(`/api/knowledge-graph/rebuild/${userId}`);

            if (response.data.success) {
                this.showSuccess('知识图谱重建完成');
                // 重新加载数据
                setTimeout(() => {
                    this.loadGraphData();
                }, 1000);
            }
        } catch (error) {
            console.error('重建知识图谱失败:', error);
            this.showError('重建失败: ' + error.message);
        } finally {
            this.showLoading(false);
        }
    }

    exportGraph() {
        if (!this.currentGraphData) {
            this.showError('没有可导出的图谱数据');
            return;
        }

        try {
            const dataStr = JSON.stringify(this.currentGraphData, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });

            const link = document.createElement('a');
            link.href = URL.createObjectURL(dataBlob);
            link.download = `knowledge-graph-${new Date().toISOString().split('T')[0]}.json`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(link.href);

            this.showSuccess('图谱数据导出成功');
        } catch (error) {
            console.error('导出图谱数据失败:', error);
            this.showError('导出失败: ' + error.message);
        }
    }

    showLoading(loading) {
        const chartDom = document.getElementById('knowledge-graph');
        if (!chartDom) return;

        if (loading) {
            this.chart.showLoading('default', {
                text: '加载中...',
                color: '#5470c6',
                textColor: '#333',
                maskColor: 'rgba(255, 255, 255, 0.8)'
            });
        } else {
            this.chart.hideLoading();
        }
    }

    showError(message) {
        console.error('知识图谱错误:', message);
        alert('错误: ' + message);
    }

    showSuccess(message) {
        console.log('知识图谱成功:', message);
        alert('成功: ' + message);
    }

    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // 页面切换时重新调整图表大小
    onPageShow() {
        if (this.chart) {
            setTimeout(() => {
                this.chart.resize();
            }, 100);
        }

        // 如果页面切换时还未初始化，重新尝试加载
        if (!this.initialized) {
            this.delayedLoadGraphData();
        }
    }
}

// ==================== 全局辅助函数 ====================

// 等待元素出现的全局函数
function waitForElementGlobal(selector, maxAttempts = 10, interval = 100) {
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

// 全局设置分类筛选的函数
async function setCategoryFilterAndLoad(categoryId) {
    try {
        console.log('设置分类筛选，分类ID:', categoryId);

        // 等待分类筛选器加载
        await waitForElementGlobal('#category-filter', 15, 200);

        const categoryFilter = document.getElementById('category-filter');
        if (categoryFilter) {
            categoryFilter.value = categoryId;
            console.log('已设置分类筛选');

            // 触发文档加载
            if (window.documentManager) {
                if (!window.documentManager.isInitialized) {
                    await window.documentManager.initialize();
                }
                await window.documentManager.loadDocuments(categoryId);

                // 显示提示
                const categoryName = categoryFilter.options[categoryFilter.selectedIndex]?.text || '该分类';
                showNotification('success', `已筛选 ${categoryName} 的文档`);
            } else {
                console.error('文档管理器未加载');
                showNotification('error', '文档管理器未加载，请刷新页面');
            }
        } else {
            console.error('分类筛选器未找到');
            showNotification('error', '页面元素未加载完成');
        }
    } catch (error) {
        console.error('设置分类筛选失败:', error);
        showNotification('error', '设置分类筛选失败: ' + error.message);
    }
}

// 全局设置标签筛选的函数
async function setTagFilterAndSearch(tagId) {
    try {
        console.log('设置标签筛选，标签ID:', tagId);

        // 等待搜索页面元素加载
        await waitForElementGlobal('#search-tag', 15, 200);
        await waitForElementGlobal('#global-search', 15, 200);

        const tagFilter = document.getElementById('search-tag');
        const searchInput = document.getElementById('global-search');

        if (tagFilter && searchInput) {
            tagFilter.value = tagId;
            console.log('已设置标签筛选');

            // 获取标签名称
            let tagName = '';
            if (window.tagManager && window.tagManager.tags) {
                const tag = window.tagManager.tags.find(t => t.id === tagId);
                if (tag) {
                    tagName = tag.name;
                    searchInput.value = tag.name;
                }
            }

            // 触发搜索
            if (window.app && typeof window.app.performSearch === 'function') {
                await window.app.performSearch();
                showNotification('success', tagName ? `已搜索标签 "${tagName}" 的文档` : '已搜索标签文档');
            } else {
                console.error('主应用未初始化');
                showNotification('error', '应用未初始化');
            }
        } else {
            console.error('搜索页面元素未找到');
            showNotification('error', '页面元素未加载完成');
        }
    } catch (error) {
        console.error('设置标签筛选失败:', error);
        showNotification('error', '设置标签筛选失败: ' + error.message);
    }
}

// 简单的全局通知函数
function showNotification(type, message) {
    if (type === 'success') {
        alert('成功: ' + message);
    } else {
        alert('错误: ' + message);
    }
}

// 全局跳转到分类文档的函数（供其他模块调用）
function navigateToCategoryDocuments(categoryId) {
    if (window.knowledgeGraphManager) {
        window.knowledgeGraphManager.viewCategoryDocuments(categoryId);
    } else {
        // 如果知识图谱管理器不存在，直接执行跳转逻辑
        setCategoryFilterAndLoad(categoryId);
    }
}

// 全局跳转到标签文档的函数（供其他模块调用）
function navigateToTagDocuments(tagId) {
    if (window.knowledgeGraphManager) {
        window.knowledgeGraphManager.viewTagDocuments(tagId);
    } else {
        // 如果知识图谱管理器不存在，直接执行跳转逻辑
        setTagFilterAndSearch(tagId);
    }
}

// ==================== 全局初始化 ====================

// 全局初始化 - 简化版本
document.addEventListener('DOMContentLoaded', () => {
    // 检查ECharts是否已加载
    if (typeof echarts === 'undefined') {
        console.error('ECharts未加载，请确保在knowledge-graph.js之前引入ECharts库');
        return;
    }

    // 只创建实例，不立即初始化
    window.knowledgeGraphManager = new KnowledgeGraphManager();

    console.log('知识图谱管理器已创建，等待页面切换时初始化');
});
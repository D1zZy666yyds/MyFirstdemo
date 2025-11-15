// 创建Vue应用
const { createApp } = Vue;

const app = createApp({
    data() {
        return {
            currentSection: 'dashboard',
            currentUser: null
        };
    },
    components: {
        'dashboard-section': DashboardSection,
        'documents-section': DocumentsSection,
        'categories-section': CategoriesSection,
        'tags-section': TagsSection,
        'search-section': SearchSection
    },
    async mounted() {
        await this.checkAuth();

        // 监听URL hash变化
        window.addEventListener('hashchange', this.handleHashChange);
        this.handleHashChange();
    },
    methods: {
        async checkAuth() {
            try {
                const user = localStorage.getItem('currentUser');
                if (user) {
                    this.currentUser = JSON.parse(user);
                } else {
                    // 如果没有登录，跳转到登录页
                    window.location.href = '/login';
                }
            } catch (error) {
                console.error('检查认证状态失败:', error);
                window.location.href = '/login';
            }
        },

        handleHashChange() {
            const hash = window.location.hash.substring(1) || 'dashboard';
            this.currentSection = hash;
        },

        showSection(section) {
            this.currentSection = section;
            window.location.hash = section;
        },

        async logout() {
            try {
                await authApi.logout();
            } catch (error) {
                console.error('退出失败:', error);
            } finally {
                localStorage.removeItem('currentUser');
                localStorage.removeItem('token');
                window.location.href = '/login';
            }
        }
    }
});

app.mount('#app');
class AuthManager {
    constructor() {
        this.currentUser = null;
        this.baseURL = '/api/v1';
        this.init();
    }

    init() {
        console.log('AuthManager 初始化');
        // 不自动检查，由各个页面自己控制
    }

    async checkAuthStatus() {
        try {
            console.log('检查认证状态...');
            const response = await axios.get(`${this.baseURL}/check-auth`);
            console.log('认证状态响应:', response.data);

            if (response.data.success && response.data.data) {
                this.currentUser = response.data.data;
                console.log('用户已登录:', this.currentUser);
                this.updateUI();
                return true;
            } else {
                console.log('用户未登录:', response.data.message);
                this.currentUser = null;
                return false;
            }
        } catch (error) {
            console.error('认证状态检查失败:', error);
            console.log('错误详情:', error.response?.data);
            this.currentUser = null;

            // 如果是401错误，说明未登录，这是正常的
            if (error.response?.status === 401) {
                console.log('用户未登录（401）');
                return false;
            }

            this.showError('检查认证状态失败: ' + (error.message || '网络错误'));
            return false;
        }
    }

    async login(username, password) {
        try {
            console.log('开始登录，用户名:', username);

            const response = await axios.post(`${this.baseURL}/login`, {
                username: username,
                password: password
            }, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            console.log('登录响应:', response.data);

            if (response.data.success && response.data.data) {
                this.currentUser = response.data.data;
                console.log('登录成功，用户信息:', this.currentUser);
                this.updateUI();

                // 显示成功消息
                this.showSuccess('登录成功！正在跳转...');

                // 延迟跳转，确保session设置完成
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1000);

                return true;
            } else {
                this.showError('登录失败: ' + (response.data.message || '未知错误'));
                return false;
            }
        } catch (error) {
            console.error('登录请求失败:', error);
            console.log('错误响应:', error.response);

            let errorMessage = '登录失败: ';
            if (error.response) {
                errorMessage += error.response.data.message || `HTTP ${error.response.status}`;
            } else if (error.request) {
                errorMessage += '网络错误，请检查服务器连接';
            } else {
                errorMessage += error.message;
            }

            this.showError(errorMessage);
            return false;
        }
    }

    async logout() {
        try {
            await axios.post(`${this.baseURL}/logout`);
        } catch (error) {
            console.error('登出失败:', error);
        } finally {
            this.currentUser = null;
            this.redirectToLogin();
        }
    }

    updateUI() {
        const usernameEl = document.getElementById('username');
        if (usernameEl && this.currentUser) {
            usernameEl.textContent = this.currentUser.username || '用户';
        }
    }

    redirectToLogin() {
        const currentPage = window.location.pathname.split('/').pop();
        if (currentPage !== 'login.html') {
            console.log('重定向到登录页面');
            window.location.href = 'login.html';
        }
    }

    showError(message) {
        console.error('认证错误:', message);
        // 使用简单的 alert
        alert('错误: ' + message);
    }

    showSuccess(message) {
        console.log('成功:', message);
        // 使用简单的 alert
        alert('成功: ' + message);
    }

    // 获取当前用户ID的安全方法
    getCurrentUserId() {
        if (this.currentUser && this.currentUser.id) {
            return this.currentUser.id;
        }
        console.error('无法获取用户ID，currentUser:', this.currentUser);

        // 添加调试信息
        console.log('当前session状态:', {
            hasSession: document.cookie.includes('JSESSIONID'),
            currentUser: this.currentUser
        });

        throw new Error('用户未登录或用户信息不完整');
    }

    // 检查是否已登录
    isAuthenticated() {
        return this.currentUser !== null && this.currentUser.id !== undefined && this.currentUser.id !== null;
    }
}

// 全局认证实例
const authManager = new AuthManager();


// 全局登出函数
function logout() {
    if (confirm('确定要退出登录吗？')) {
        authManager.logout();
    }
}



class LoginManager {
    constructor() {
        this.init();
    }

    init() {
        console.log('LoginManager 初始化');
        this.setupEventListeners();
        this.checkRememberMe();
    }

    setupEventListeners() {
        // 登录表单提交
        const loginForm = document.getElementById('login-form');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                console.log('登录表单提交');
                this.handleLogin();
            });
        }

        // 输入框回车提交
        const passwordInput = document.getElementById('password');
        if (passwordInput) {
            passwordInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    console.log('密码框回车提交');
                    this.handleLogin();
                }
            });
        }

        // 注册表单提交
        const registerForm = document.getElementById('register-form');
        if (registerForm) {
            registerForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleRegister();
            });
        }

        // 实时验证
        this.setupRealTimeValidation();
    }

    setupRealTimeValidation() {
        // 用户名实时验证
        const usernameInput = document.getElementById('username');
        if (usernameInput) {
            usernameInput.addEventListener('blur', () => {
                this.validateField('username', usernameInput.value);
            });
        }

        // 密码实时验证
        const passwordInput = document.getElementById('password');
        if (passwordInput) {
            passwordInput.addEventListener('blur', () => {
                this.validateField('password', passwordInput.value);
            });
        }
    }

    validateField(field, value) {
        const errorElement = document.getElementById(`${field}-error`);
        if (!errorElement) return true;

        let isValid = true;
        let message = '';

        switch (field) {
            case 'username':
                isValid = value.length >= 3 && value.length <= 20;
                message = isValid ? '' : '用户名应为3-20个字符';
                break;
            case 'password':
                isValid = value.length >= 6;
                message = isValid ? '' : '密码至少需要6个字符';
                break;
        }

        errorElement.textContent = message;
        return isValid;
    }

    async handleLogin() {
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        const rememberMe = document.getElementById('remember-me').checked;

        console.log('登录尝试:', { username, passwordLength: password.length, rememberMe });

        // 清除之前的信息
        this.clearMessages();

        // 基础验证
        if (!username) {
            this.showFieldError('username', '请输入用户名');
            return;
        }

        if (!password) {
            this.showFieldError('password', '请输入密码');
            return;
        }

        if (!this.validateField('username', username) || !this.validateField('password', password)) {
            this.showMessage('请修正表单错误', 'error');
            return;
        }

        // 显示加载状态
        this.setLoginButtonLoading(true);

        try {
            console.log('调用登录API...');
            const success = await authManager.login(username, password);
            console.log('登录结果:', success);

            if (success) {
                if (rememberMe) {
                    this.saveCredentials(username);
                } else {
                    this.clearCredentials();
                }
                // 成功消息在authManager中显示
            } else {
                this.showMessage('登录失败，请检查用户名和密码', 'error');
            }

        } catch (error) {
            console.error('登录过程异常:', error);
            this.showMessage('登录过程发生异常: ' + error.message, 'error');
        } finally {
            this.setLoginButtonLoading(false);
        }
    }

    async handleRegister() {
        const username = document.getElementById('reg-username').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const password = document.getElementById('reg-password').value;
        const confirmPassword = document.getElementById('reg-confirm-password').value;

        console.log('注册尝试:', { username, email, passwordLength: password.length });

        // 验证输入
        if (!this.validateUsername(username)) {
            this.showError('用户名应为3-20个字符');
            return;
        }

        if (!this.validateEmail(email)) {
            this.showError('请输入有效的邮箱地址');
            return;
        }

        if (!this.validatePassword(password)) {
            this.showError('密码至少需要6个字符');
            return;
        }

        if (password !== confirmPassword) {
            this.showError('两次输入的密码不一致');
            return;
        }

        this.setLoginButtonLoading(true);

        try {
            const response = await axios.post('/api/v1/register', {
                username: username,
                email: email,
                password: password
            }, {
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            console.log('注册响应:', response.data);

            if (response.data.success) {
                this.showSuccess('注册成功！请使用新账号登录');
                closeRegisterModal();

                // 自动填充登录表单
                document.getElementById('username').value = username;
                document.getElementById('password').value = password;
            } else {
                this.showError('注册失败: ' + (response.data.message || '未知错误'));
            }
        } catch (error) {
            console.error('注册失败:', error);
            const message = error.response?.data?.message || '注册失败，请稍后重试';
            this.showError(message);
        } finally {
            this.setLoginButtonLoading(false);
        }
    }

    validateUsername(username) {
        return username.length >= 3 && username.length <= 20;
    }

    validatePassword(password) {
        return password.length >= 6;
    }

    validateEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    showFieldError(field, message) {
        const errorElement = document.getElementById(`${field}-error`);
        if (errorElement) {
            errorElement.textContent = message;
        }
    }

    showMessage(message, type = 'info') {
        const container = document.getElementById('login-message');
        if (container) {
            container.textContent = message;
            container.className = `message-container message-${type}`;
        } else {
            if (type === 'error') {
                alert('错误: ' + message);
            } else if (type === 'success') {
                alert('成功: ' + message);
            } else {
                alert(message);
            }
        }
    }

    clearMessages() {
        // 清除所有错误消息
        document.querySelectorAll('.error-message').forEach(el => {
            el.textContent = '';
        });

        // 清除主消息
        const messageContainer = document.getElementById('login-message');
        if (messageContainer) {
            messageContainer.textContent = '';
            messageContainer.className = 'message-container';
        }
    }

    setLoginButtonLoading(loading) {
        const loginBtn = document.getElementById('login-btn');
        const loginText = document.getElementById('login-text');
        const loadingSpinner = document.getElementById('login-loading');

        if (loginBtn) {
            if (loading) {
                loginBtn.disabled = true;
                if (loginText) loginText.style.display = 'none';
                if (loadingSpinner) loadingSpinner.style.display = 'block';
            } else {
                loginBtn.disabled = false;
                if (loginText) loginText.style.display = 'block';
                if (loadingSpinner) loadingSpinner.style.display = 'none';
            }
        }
    }

    saveCredentials(username) {
        localStorage.setItem('rememberedUsername', username);
    }

    clearCredentials() {
        localStorage.removeItem('rememberedUsername');
    }

    checkRememberMe() {
        const rememberedUsername = localStorage.getItem('rememberedUsername');
        if (rememberedUsername) {
            document.getElementById('username').value = rememberedUsername;
            document.getElementById('remember-me').checked = true;
        }
    }

    showError(message) {
        alert('错误: ' + message);
    }

    showSuccess(message) {
        alert('成功: ' + message);
    }
}

// 全局函数
function showRegisterModal() {
    document.getElementById('register-modal').style.display = 'flex';
    // 清除注册表单消息
    const registerMessage = document.getElementById('register-message');
    if (registerMessage) {
        registerMessage.textContent = '';
        registerMessage.className = 'message-container';
    }
}

function closeRegisterModal() {
    document.getElementById('register-modal').style.display = 'none';
    document.getElementById('register-form').reset();
    // 清除错误消息
    document.querySelectorAll('#register-form .error-message').forEach(el => {
        el.textContent = '';
    });
}

function showForgotPassword() {
    alert('请联系管理员重置密码');
}

// 点击模态框外部关闭
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        e.target.style.display = 'none';
    }
});

// 初始化登录管理器
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM加载完成，初始化登录管理器');
    window.loginManager = new LoginManager();
});

// 测试函数
function testLogin() {
    console.log('执行测试登录');
    document.getElementById('username').value = 'test';
    document.getElementById('password').value = '123456';
    console.log('已填充测试用户');
}
window.testLogin = testLogin;
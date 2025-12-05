/**
 * 密码管理模块 - 与document.js、category.js保持相同架构
 * 文件: password.js
 */
class PasswordManager {
    constructor() {
        this.isInitialized = false;
        this.currentUser = null;
    }

    /**
     * 初始化密码管理模块
     */
    async initialize() {
        if (this.isInitialized) return;

        console.log('🚀 初始化密码管理模块...');

        try {
            // 检查认证状态
            const isAuthenticated = await authManager.checkAuthStatus();
            if (!isAuthenticated) {
                console.warn('用户未登录，无法修改密码');
                this.showNotification('请先登录系统', 'warning');
                authManager.redirectToLogin();
                return;
            }

            // 获取当前用户信息
            this.currentUser = authManager.getCurrentUser();
            if (!this.currentUser) {
                console.warn('无法获取用户信息');
                this.showNotification('用户信息获取失败', 'error');
                return;
            }

            this.isInitialized = true;
            console.log('✅ 密码管理模块初始化完成');

        } catch (error) {
            console.error('❌ 密码管理模块初始化失败:', error);
            this.showNotification('初始化失败: ' + error.message, 'error');
        }
    }

    /**
     * 显示修改密码模态框
     */
    showChangePasswordModal() {
        if (!authManager.isAuthenticated()) {
            this.showNotification('请先登录系统', 'warning');
            authManager.redirectToLogin();
            return;
        }

        const modalContent = `
            <div class="modal">
                <div class="modal-content password-modal">
                    <div class="modal-header">
                        <h3>修改密码</h3>
                        <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
                    </div>
                    <div class="modal-body">
                        <form id="change-password-form" class="password-form">
                            <div class="user-info">
                                <div class="user-avatar">👤</div>
                                <div class="user-details">
                                    <div class="username">${this.escapeHtml(this.currentUser?.username || '用户')}</div>
                                    <div class="user-email">${this.escapeHtml(this.currentUser?.email || '')}</div>
                                </div>
                            </div>
                            
                            <div class="form-group">
                                <label for="old-password">
                                    <span class="label-icon">🔑</span>
                                    原密码 *
                                </label>
                                <div class="password-input-container">
                                    <input type="password" id="old-password" required class="form-input" 
                                           placeholder="请输入当前密码" autocomplete="current-password"
                                           minlength="6" maxlength="50">
                                </div>
                                <div id="old-password-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="new-password">
                                    <span class="label-icon">🛡️</span>
                                    新密码 *
                                </label>
                                <div class="password-input-container">
                                    <input type="password" id="new-password" required class="form-input" 
                                           placeholder="请输入新密码（至少6位）" autocomplete="new-password"
                                           minlength="6" maxlength="50">
                                </div>
                                
                                <!-- 密码强度指示器 -->
                                <div class="password-strength-container">
                                    <div class="strength-meter">
                                        <div class="strength-bar" id="strength-bar"></div>
                                    </div>
                                    <div class="strength-labels">
                                        <span class="strength-label" data-strength="weak">弱</span>
                                        <span class="strength-label" data-strength="fair">一般</span>
                                        <span class="strength-label" data-strength="good">良好</span>
                                        <span class="strength-label" data-strength="strong">强</span>
                                    </div>
                                </div>
                                
                                <div id="new-password-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="confirm-password">
                                    <span class="label-icon">✓</span>
                                    确认新密码 *
                                </label>
                                <div class="password-input-container">
                                    <input type="password" id="confirm-password" required class="form-input" 
                                           placeholder="请再次输入新密码" autocomplete="new-password"
                                           minlength="6" maxlength="50">
                                </div>
                                <div id="confirm-password-error" class="error-message"></div>
                            </div>
                            
                            <!-- 密码要求 -->
                            <div class="password-requirements">
                                <h4><span class="requirement-icon">📋</span> 密码要求</h4>
                                <ul class="requirement-list">
                                    <li class="requirement-item" data-requirement="length">
                                        <span class="requirement-icon">○</span>
                                        至少6个字符
                                    </li>
                                    <li class="requirement-item" data-requirement="uppercase">
                                        <span class="requirement-icon">○</span>
                                        包含大写字母
                                    </li>
                                    <li class="requirement-item" data-requirement="lowercase">
                                        <span class="requirement-icon">○</span>
                                        包含小写字母
                                    </li>
                                    <li class="requirement-item" data-requirement="number">
                                        <span class="requirement-icon">○</span>
                                        包含数字
                                    </li>
                                    <li class="requirement-item" data-requirement="special">
                                        <span class="requirement-icon">○</span>
                                        包含特殊字符
                                    </li>
                                </ul>
                            </div>
                            
                            <!-- 安全提示 -->
                            <div class="security-tip">
                                <span class="tip-icon">⚠️</span>
                                <p><strong>安全提示：</strong>建议使用包含字母、数字和特殊字符的组合密码，避免使用简单或常见的密码。</p>
                            </div>
                            
                            <div id="password-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary" id="submit-password-btn">
                                    <span class="btn-text">确定修改</span>
                                    <span class="btn-loading" style="display: none;">
                                        <span class="loading-spinner-small"></span> 修改中...
                                    </span>
                                </button>
                                <button type="button" onclick="this.closest('.modal').remove()" class="btn-secondary">取消</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        const modalContainer = document.getElementById('modal-container') || this.createModalContainer();
        modalContainer.innerHTML = modalContent;

        // 设置表单事件
        this.setupPasswordForm();

        // 初始聚焦到原密码输入框
        setTimeout(() => {
            const oldPasswordInput = document.getElementById('old-password');
            if (oldPasswordInput) {
                oldPasswordInput.focus();
            }
        }, 100);
    }

    /**
     * 创建模态框容器
     */
    createModalContainer() {
        const container = document.createElement('div');
        container.id = 'modal-container';
        container.className = 'modal-container';
        document.body.appendChild(container);
        return container;
    }

    /**
     * 设置密码表单提交事件
     */
    setupPasswordForm() {
        const form = document.getElementById('change-password-form');
        if (!form) return;

        // 表单提交事件
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleChangePassword();
        });

        // 实时验证
        this.setupRealTimeValidation();

        // 密码强度检查
        this.setupPasswordStrengthChecker();

        // 添加密码显示/隐藏切换
        this.addPasswordToggle();
    }

    /**
     * 设置实时验证
     */
    setupRealTimeValidation() {
        const newPassword = document.getElementById('new-password');
        const confirmPassword = document.getElementById('confirm-password');

        if (newPassword) {
            newPassword.addEventListener('input', () => {
                this.validatePasswordStrength();
                this.validatePasswordMatch();
                this.validatePasswordRequirements();
            });
        }

        if (confirmPassword) {
            confirmPassword.addEventListener('input', () => {
                this.validatePasswordMatch();
            });
        }

        // 原密码输入验证
        const oldPassword = document.getElementById('old-password');
        if (oldPassword) {
            oldPassword.addEventListener('input', () => {
                this.validateOldPassword();
            });
        }
    }

    /**
     * 设置密码强度检查器
     */
    setupPasswordStrengthChecker() {
        const passwordInput = document.getElementById('new-password');
        if (!passwordInput) return;

        passwordInput.addEventListener('input', () => {
            this.calculatePasswordStrength(passwordInput.value);
        });
    }

    /**
     * 计算密码强度
     */
    calculatePasswordStrength(password) {
        const strengthBar = document.getElementById('strength-bar');
        const labels = document.querySelectorAll('.strength-label');

        if (!strengthBar || !labels.length) return;

        let strength = 0;

        // 长度检查
        if (password.length >= 6) strength += 20;
        if (password.length >= 8) strength += 10;
        if (password.length >= 12) strength += 10;

        // 字符类型检查
        if (/[A-Z]/.test(password)) strength += 15;  // 大写字母
        if (/[a-z]/.test(password)) strength += 15;  // 小写字母
        if (/[0-9]/.test(password)) strength += 15;  // 数字
        if (/[^A-Za-z0-9]/.test(password)) strength += 15;  // 特殊字符

        // 复杂度加分
        const complexity = [];
        if (/[A-Z]/.test(password)) complexity.push('uppercase');
        if (/[a-z]/.test(password)) complexity.push('lowercase');
        if (/[0-9]/.test(password)) complexity.push('number');
        if (/[^A-Za-z0-9]/.test(password)) complexity.push('special');

        if (complexity.length >= 3) strength += 10;

        // 限制最大强度为100
        strength = Math.min(strength, 100);

        // 更新强度条
        strengthBar.style.width = `${strength}%`;

        // 更新强度颜色和标签
        let strengthLevel = 'weak';
        let strengthColor = 'var(--danger-500)';

        if (strength < 30) {
            strengthLevel = 'weak';
            strengthColor = 'var(--danger-500)';
        } else if (strength < 60) {
            strengthLevel = 'fair';
            strengthColor = 'var(--warning-500)';
        } else if (strength < 85) {
            strengthLevel = 'good';
            strengthColor = 'var(--info-500)';
        } else {
            strengthLevel = 'strong';
            strengthColor = 'var(--success-500)';
        }

        strengthBar.style.backgroundColor = strengthColor;

        // 更新标签状态
        labels.forEach(label => {
            label.classList.remove('active');
            if (label.dataset.strength === strengthLevel) {
                label.classList.add('active');
            }
        });
    }

    /**
     * 验证原密码
     */
    validateOldPassword() {
        const oldPassword = document.getElementById('old-password');
        const errorElement = document.getElementById('old-password-error');

        if (!oldPassword || !errorElement) return;

        if (!oldPassword.value.trim()) {
            errorElement.textContent = '请输入原密码';
            return false;
        } else {
            errorElement.textContent = '';
            return true;
        }
    }

    /**
     * 验证密码强度
     */
    validatePasswordStrength() {
        const newPassword = document.getElementById('new-password');
        const errorElement = document.getElementById('new-password-error');

        if (!newPassword || !errorElement) return;

        if (!newPassword.value.trim()) {
            errorElement.textContent = '请输入新密码';
            return false;
        }

        if (newPassword.value.length < 6) {
            errorElement.textContent = '密码至少需要6个字符';
            return false;
        }

        errorElement.textContent = '';
        return true;
    }

    /**
     * 验证密码要求
     */
    validatePasswordRequirements() {
        const password = document.getElementById('new-password')?.value || '';
        const requirements = {
            length: password.length >= 6,
            uppercase: /[A-Z]/.test(password),
            lowercase: /[a-z]/.test(password),
            number: /[0-9]/.test(password),
            special: /[^A-Za-z0-9]/.test(password)
        };

        Object.entries(requirements).forEach(([type, isValid]) => {
            const item = document.querySelector(`.requirement-item[data-requirement="${type}"]`);
            if (item) {
                const icon = item.querySelector('.requirement-icon');
                if (icon) {
                    icon.textContent = isValid ? '✓' : '○';
                    icon.style.color = isValid ? 'var(--success-500)' : 'var(--neutral-400)';
                }
                item.style.color = isValid ? 'var(--success-600)' : 'var(--neutral-500)';
            }
        });

        return Object.values(requirements).every(v => v);
    }

    /**
     * 验证密码是否匹配
     */
    validatePasswordMatch() {
        const newPassword = document.getElementById('new-password');
        const confirmPassword = document.getElementById('confirm-password');
        const errorElement = document.getElementById('confirm-password-error');

        if (!newPassword || !confirmPassword || !errorElement) return true;

        if (confirmPassword.value && newPassword.value !== confirmPassword.value) {
            errorElement.textContent = '两次输入的密码不一致';
            return false;
        } else {
            errorElement.textContent = '';
            return true;
        }
    }

    /**
     * 添加密码显示/隐藏切换
     */
    addPasswordToggle() {
        const passwordInputs = document.querySelectorAll('.password-input-container');

        passwordInputs.forEach(container => {
            if (container.querySelector('.password-toggle')) return;

            const input = container.querySelector('input[type="password"]');
            if (!input) return;

            const toggle = document.createElement('button');
            toggle.type = 'button';
            toggle.className = 'password-toggle';
            toggle.innerHTML = '<span class="eye-icon">👁️</span>';
            toggle.title = '显示/隐藏密码';
            toggle.setAttribute('aria-label', '切换密码可见性');

            toggle.addEventListener('click', () => {
                const isPassword = input.type === 'password';
                input.type = isPassword ? 'text' : 'password';
                toggle.classList.toggle('active', isPassword);
                toggle.innerHTML = isPassword
                    ? '<span class="eye-icon">👁️‍🗨️</span>'
                    : '<span class="eye-icon">👁️</span>';
                toggle.title = isPassword ? '隐藏密码' : '显示密码';
            });

            container.appendChild(toggle);
        });
    }

    /**
     * 处理密码修改
     */
    async handleChangePassword() {
        try {
            // 获取表单数据
            const oldPassword = document.getElementById('old-password')?.value || '';
            const newPassword = document.getElementById('new-password')?.value || '';
            const confirmPassword = document.getElementById('confirm-password')?.value || '';

            // 清除之前的错误消息
            this.clearErrors();

            // 验证所有输入
            const validations = [
                { condition: !oldPassword, field: 'old-password', message: '请输入原密码' },
                { condition: !newPassword, field: 'new-password', message: '请输入新密码' },
                { condition: newPassword.length < 6, field: 'new-password', message: '新密码至少需要6个字符' },
                { condition: !confirmPassword, field: 'confirm-password', message: '请确认新密码' },
                { condition: newPassword !== confirmPassword, field: 'confirm-password', message: '两次输入的密码不一致' }
            ];

            let isValid = true;
            validations.forEach(validation => {
                if (validation.condition) {
                    this.showFieldError(validation.field, validation.message);
                    isValid = false;
                }
            });

            if (!isValid) {
                return;
            }

            // 验证密码要求
            const meetsRequirements = this.validatePasswordRequirements();
            if (!meetsRequirements) {
                this.showFormError('请确保新密码满足所有要求');
                return;
            }

            // 显示加载状态
            this.setSubmitButtonLoading(true);

            // 调用API修改密码
            const response = await axios.post('/api/v1/change-password', {
                oldPassword: oldPassword,
                newPassword: newPassword
            }, {
                headers: {
                    'Authorization': `Bearer ${authManager.getToken()}`,
                    'Content-Type': 'application/json'
                }
            });

            console.log('✅ 修改密码响应:', response.data);

            if (response.data.success) {
                this.showFormSuccess('🎉 密码修改成功！');

                // 密码修改成功后跳转到登录界面
                setTimeout(() => {
                    this.handlePasswordChangeSuccess();
                }, 1500);

            } else {
                throw new Error(response.data.message || '修改密码失败');
            }

        } catch (error) {
            console.error('❌ 修改密码失败:', error);
            this.handlePasswordChangeError(error);

        } finally {
            this.setSubmitButtonLoading(false);
        }
    }

    /**
     * 处理密码修改成功
     */
    handlePasswordChangeSuccess() {
        // 移除模态框
        const modal = document.querySelector('.modal');
        if (modal) {
            modal.remove();
        }

        // 显示成功通知
        this.showNotification('密码修改成功，请使用新密码重新登录', 'success');

        // 清除所有用户数据
        this.clearUserData();

        // 跳转到登录页面
        setTimeout(() => {
            this.redirectToLogin();
        }, 2000);
    }

    /**
     * 处理密码修改错误
     */
    handlePasswordChangeError(error) {
        const errorMsg = error.response?.data?.message || error.message;

        if (errorMsg.includes('原密码错误') || errorMsg.includes('old password') ||
            errorMsg.includes('invalid password') || errorMsg.includes('当前密码错误')) {
            this.showFormError('原密码错误，请重新输入');
            this.showFieldError('old-password', '原密码错误');
            this.focusAndSelectField('old-password');

        } else if (errorMsg.includes('未登录') || errorMsg.includes('not logged') ||
            errorMsg.includes('unauthorized') || errorMsg.includes('token')) {
            this.showFormError('登录已过期，请重新登录');
            setTimeout(() => {
                authManager.logout();
            }, 2000);

        } else if (errorMsg.includes('too short') || errorMsg.includes('至少需要') ||
            errorMsg.includes('6个字符')) {
            this.showFormError('新密码至少需要6个字符');
            this.showFieldError('new-password', '密码至少需要6个字符');
            this.focusAndSelectField('new-password');

        } else if (errorMsg.includes('same as') || errorMsg.includes('与旧密码相同')) {
            this.showFormError('新密码不能与原密码相同');
            this.showFieldError('new-password', '新密码不能与原密码相同');
            this.focusAndSelectField('new-password');

        } else if (errorMsg.includes('common') || errorMsg.includes('常见密码')) {
            this.showFormError('密码太常见，请使用更复杂的密码');
            this.showFieldError('new-password', '请使用更复杂的密码');
            this.focusAndSelectField('new-password');

        } else if (error.response?.status === 429) {
            this.showFormError('尝试次数过多，请稍后再试');

        } else {
            this.showFormError('修改密码失败: ' + errorMsg);
        }
    }

    /**
     * 聚焦并选中字段
     */
    focusAndSelectField(fieldId) {
        const field = document.getElementById(fieldId);
        if (field) {
            field.focus();
            field.select();
        }
    }

    /**
     * 清除用户数据
     */
    clearUserData() {
        console.log('🧹 清除用户数据...');

        // 清除localStorage中的用户数据
        const userKeys = ['userToken', 'username', 'userId', 'userRole', 'lastLogin', 'authExpires', 'userEmail'];
        userKeys.forEach(key => localStorage.removeItem(key));

        // 清除sessionStorage
        sessionStorage.clear();

        // 清除cookie
        document.cookie.split(";").forEach(function(c) {
            document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
        });

        console.log('✅ 用户数据已清除');
    }

    /**
     * 跳转到登录页面
     */
    redirectToLogin() {
        // 根据项目结构设置正确的登录页面路径
        const loginPages = ['login.html', 'index.html', 'signin.html', '/', './login.html'];

        let loginPage = loginPages[0];

        // 尝试找到存在的页面
        loginPages.forEach(page => {
            try {
                const url = new URL(page, window.location.origin);
                if (url.href !== window.location.href) {
                    loginPage = page;
                }
            } catch (e) {
                // 使用相对路径
                loginPage = page;
            }
        });

        console.log(`🔗 跳转到登录页面: ${loginPage}`);

        // 使用完整跳转确保清除所有状态
        window.location.href = loginPage;
    }

    /**
     * 清除错误消息
     */
    clearErrors() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(el => {
            el.textContent = '';
        });

        const messageDiv = document.getElementById('password-message');
        if (messageDiv) {
            messageDiv.innerHTML = '';
            messageDiv.className = 'message-container';
        }
    }

    /**
     * 显示字段错误
     */
    showFieldError(fieldId, message) {
        const errorElement = document.getElementById(`${fieldId}-error`);
        if (errorElement) {
            errorElement.textContent = message;
        }
    }

    /**
     * 显示表单成功消息
     */
    showFormSuccess(message) {
        const messageDiv = document.getElementById('password-message');
        if (messageDiv) {
            messageDiv.innerHTML = `<div class="message-success">${message}</div>`;
            messageDiv.className = 'message-container message-success';
        }
    }

    /**
     * 显示表单错误消息
     */
    showFormError(message) {
        const messageDiv = document.getElementById('password-message');
        if (messageDiv) {
            messageDiv.innerHTML = `<div class="message-error">${message}</div>`;
            messageDiv.className = 'message-container message-error';
        }
    }

    /**
     * 设置提交按钮加载状态
     */
    setSubmitButtonLoading(loading) {
        const submitBtn = document.getElementById('submit-password-btn');
        if (submitBtn) {
            const btnText = submitBtn.querySelector('.btn-text');
            const btnLoading = submitBtn.querySelector('.btn-loading');

            if (loading) {
                submitBtn.disabled = true;
                if (btnText) btnText.style.display = 'none';
                if (btnLoading) btnLoading.style.display = 'flex';
            } else {
                submitBtn.disabled = false;
                if (btnText) btnText.style.display = 'flex';
                if (btnLoading) btnLoading.style.display = 'none';
            }
        }
    }

    /**
     * 显示通知
     */
    showNotification(message, type = 'info') {
        console.log(`📢 ${type.toUpperCase()}: ${message}`);

        // 创建通知元素
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span class="notification-icon">${this.getNotificationIcon(type)}</span>
                <span class="notification-message">${message}</span>
            </div>
            <button class="notification-close" onclick="this.parentElement.remove()">&times;</button>
        `;

        // 添加到页面
        const container = document.getElementById('notification-container') || this.createNotificationContainer();
        container.appendChild(notification);

        // 自动移除
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 5000);
    }

    /**
     * 获取通知图标
     */
    getNotificationIcon(type) {
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };
        return icons[type] || '📢';
    }

    /**
     * 创建通知容器
     */
    createNotificationContainer() {
        const container = document.createElement('div');
        container.id = 'notification-container';
        container.className = 'notification-container';
        document.body.appendChild(container);
        return container;
    }

    /**
     * 转义HTML
     */
    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    /**
     * 销毁实例
     */
    destroy() {
        this.isInitialized = false;
        this.currentUser = null;
        console.log('♻️ 密码管理模块已销毁');
    }
}

// 全局密码管理器实例
const passwordManager = new PasswordManager();

/**
 * 全局函数 - 显示修改密码模态框
 */
function showChangePasswordModal() {
    if (window.passwordManager && passwordManager.isInitialized) {
        passwordManager.showChangePasswordModal();
    } else {
        console.error('密码管理器未初始化');
        if (window.showNotification) {
            showNotification('系统正在初始化，请稍后重试', 'error');
        } else {
            alert('系统正在初始化，请稍后重试');
        }
    }
}

/**
 * 初始化密码管理模块
 */
async function initializePasswordManager() {
    try {
        await passwordManager.initialize();
        console.log('✅ 密码管理模块已就绪');
    } catch (error) {
        console.error('❌ 密码管理模块初始化失败:', error);
    }
}

// DOM加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 检查是否在登录页面，如果是则跳过初始化
    const isLoginPage = window.location.pathname.includes('login') ||
        window.location.pathname.includes('index') ||
        !document.querySelector('.nav-user');

    if (!isLoginPage) {
        initializePasswordManager();
    }

    // 将密码管理器挂载到全局
    window.passwordManager = passwordManager;
    window.showChangePasswordModal = showChangePasswordModal;

    console.log('🔐 密码管理模块已加载');
});

// 导出模块（如果使用模块系统）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { PasswordManager, passwordManager };
}

// 确保全局可访问
window.PasswordManager = PasswordManager;
window.passwordManager = passwordManager;
window.showChangePasswordModal = showChangePasswordModal;
window.initializePasswordManager = initializePasswordManager;
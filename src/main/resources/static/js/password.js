/**
 * 密码管理模块 - 与document.js、category.js保持相同架构
 */
class PasswordManager {
    constructor() {
        this.isInitialized = false;
    }

    /**
     * 初始化密码管理模块
     */
    async initialize() {
        if (this.isInitialized) return;

        console.log('初始化密码管理模块...');

        try {
            // 检查认证状态
            const isAuthenticated = await authManager.checkAuthStatus();
            if (!isAuthenticated) {
                console.warn('用户未登录，无法修改密码');
                this.showError('请先登录系统');
                authManager.redirectToLogin();
                return;
            }

            this.isInitialized = true;
            console.log('密码管理模块初始化完成');
        } catch (error) {
            console.error('密码管理模块初始化失败:', error);
            this.showError('初始化失败: ' + error.message);
        }
    }

    /**
     * 显示修改密码模态框
     */
    showChangePasswordModal() {
        if (!authManager.isAuthenticated()) {
            alert('请先登录系统');
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
                            <div class="form-group">
                                <label for="old-password">原密码 *</label>
                                <input type="password" id="old-password" required class="form-input" 
                                       placeholder="请输入当前密码" autocomplete="current-password">
                                <div id="old-password-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="new-password">新密码 *</label>
                                <input type="password" id="new-password" required class="form-input" 
                                       placeholder="请输入新密码（至少6位）" autocomplete="new-password">
                                <div class="input-hint">密码至少需要6个字符</div>
                                <div id="new-password-error" class="error-message"></div>
                            </div>
                            
                            <div class="form-group">
                                <label for="confirm-password">确认新密码 *</label>
                                <input type="password" id="confirm-password" required class="form-input" 
                                       placeholder="请再次输入新密码" autocomplete="new-password">
                                <div id="confirm-password-error" class="error-message"></div>
                            </div>
                            
                            <div id="password-message" class="message-container"></div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn-primary">确定修改</button>
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
            this.setupPasswordForm();
        } else {
            console.error('模态框容器未找到');
        }
    }

    /**
     * 设置密码表单提交事件
     */
    setupPasswordForm() {
        const form = document.getElementById('change-password-form');
        if (!form) return;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleChangePassword();
        });

        // 实时验证
        this.setupRealTimeValidation();
    }

    /**
     * 设置实时验证
     */
    setupRealTimeValidation() {
        const newPassword = document.getElementById('new-password');
        const confirmPassword = document.getElementById('confirm-password');

        if (newPassword && confirmPassword) {
            confirmPassword.addEventListener('input', () => {
                this.validatePasswordMatch();
            });

            newPassword.addEventListener('input', () => {
                this.validatePasswordStrength();
            });
        }
    }

    /**
     * 验证密码强度
     */
    validatePasswordStrength() {
        const newPassword = document.getElementById('new-password');
        const errorElement = document.getElementById('new-password-error');

        if (!newPassword || !errorElement) return;

        if (newPassword.value && newPassword.value.length < 6) {
            errorElement.textContent = '密码至少需要6个字符';
            return false;
        } else {
            errorElement.textContent = '';
            return true;
        }
    }

    /**
     * 验证密码是否匹配
     */
    validatePasswordMatch() {
        const newPassword = document.getElementById('new-password');
        const confirmPassword = document.getElementById('confirm-password');
        const errorElement = document.getElementById('confirm-password-error');

        if (!newPassword || !confirmPassword || !errorElement) return;

        if (newPassword.value && confirmPassword.value &&
            newPassword.value !== confirmPassword.value) {
            errorElement.textContent = '两次输入的密码不一致';
            return false;
        } else {
            errorElement.textContent = '';
            return true;
        }
    }

    /**
     * 处理密码修改
     */
    async handleChangePassword() {
        try {
            // 获取表单数据
            const oldPassword = document.getElementById('old-password').value;
            const newPassword = document.getElementById('new-password').value;
            const confirmPassword = document.getElementById('confirm-password').value;

            // 清除之前的错误消息
            this.clearErrors();

            // 基础验证
            let isValid = true;

            if (!oldPassword) {
                this.showFieldError('old-password', '请输入原密码');
                isValid = false;
            }

            if (!newPassword) {
                this.showFieldError('new-password', '请输入新密码');
                isValid = false;
            } else if (newPassword.length < 6) {
                this.showFieldError('new-password', '新密码至少需要6个字符');
                isValid = false;
            }

            if (!confirmPassword) {
                this.showFieldError('confirm-password', '请确认新密码');
                isValid = false;
            } else if (newPassword !== confirmPassword) {
                this.showFieldError('confirm-password', '两次输入的密码不一致');
                isValid = false;
            }

            if (!isValid) {
                return;
            }

            // 显示加载状态
            this.setSubmitButtonLoading(true);

            // 调用API修改密码
            const response = await axios.post('/api/v1/change-password', {
                oldPassword: oldPassword,
                newPassword: newPassword
            });

            console.log('修改密码响应:', response.data);

            if (response.data.success) {
                this.showFormSuccess('密码修改成功！');

                // 清空表单
                document.getElementById('old-password').value = '';
                document.getElementById('new-password').value = '';
                document.getElementById('confirm-password').value = '';

                // 密码修改成功后跳转到登录界面
                setTimeout(() => {
                    const modal = document.querySelector('.modal');
                    if (modal) {
                        modal.remove();
                    }

                    // 显示成功提示
                    alert('密码修改成功，请使用新密码重新登录');

                    // 清除所有用户数据
                    this.clearUserData();

                    // 跳转到登录页面
                    this.redirectToLogin();

                }, 1500);

            } else {
                throw new Error(response.data.message);
            }

        } catch (error) {
            console.error('修改密码失败:', error);

            // 特殊处理各种错误情况
            const errorMsg = error.response?.data?.message || error.message;

            if (errorMsg.includes('原密码错误') || errorMsg.includes('old password')) {
                this.showFormError('原密码错误，请重新输入');
                this.showFieldError('old-password', '原密码错误');
                document.getElementById('old-password').focus();
                document.getElementById('old-password').select();

            } else if (errorMsg.includes('用户未登录') || errorMsg.includes('未登录')) {
                this.showFormError('登录已过期，请重新登录');
                setTimeout(() => {
                    this.redirectToLogin();
                }, 2000);

            } else if (errorMsg.includes('至少需要6个字符')) {
                this.showFormError('新密码至少需要6个字符');
                this.showFieldError('new-password', '密码至少需要6个字符');
                document.getElementById('new-password').focus();

            } else {
                this.showFormError('修改密码失败: ' + errorMsg);
            }

        } finally {
            this.setSubmitButtonLoading(false);
        }
    }

    /**
     * 清除用户数据
     */
    clearUserData() {
        // 清除localStorage中的用户数据
        localStorage.removeItem('userToken');
        localStorage.removeItem('username');
        localStorage.removeItem('userId');
        localStorage.removeItem('userRole');
        localStorage.removeItem('lastLogin');
        localStorage.removeItem('authExpires');

        // 清除所有localStorage数据（可选）
        // localStorage.clear();

        // 清除sessionStorage
        sessionStorage.clear();

        // 清除可能的cookie
        document.cookie.split(";").forEach(function(c) {
            document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
        });
    }

    /**
     * 跳转到登录页面
     */
    redirectToLogin() {
        // 请将下面的URL替换为您的实际登录页面路径
        const loginPage = 'login.html'; // 或 'index.html'、'signin.html' 等

        // 使用完整跳转确保清除所有状态
        setTimeout(() => {
            window.location.href = loginPage;
        }, 1000);
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
            messageDiv.innerHTML = `<div class="message success">✅ ${message}</div>`;
        }
    }

    /**
     * 显示表单错误消息
     */
    showFormError(message) {
        const messageDiv = document.getElementById('password-message');
        if (messageDiv) {
            messageDiv.innerHTML = `<div class="message error">❌ ${message}</div>`;
        }
    }

    /**
     * 设置提交按钮加载状态
     */
    setSubmitButtonLoading(loading) {
        const submitBtn = document.querySelector('#change-password-form .btn-primary');
        if (submitBtn) {
            if (loading) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="loading-spinner-small"></span> 修改中...';
            } else {
                submitBtn.disabled = false;
                submitBtn.textContent = '确定修改';
            }
        }
    }

    /**
     * 显示错误消息
     */
    showError(message) {
        console.error('密码管理错误:', message);
        alert('错误: ' + message);
    }

    /**
     * 显示成功消息
     */
    showSuccess(message) {
        console.log('密码管理成功:', message);
        alert('成功: ' + message);
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
}

// 全局密码管理器实例
const passwordManager = new PasswordManager();

// 全局函数 - 与showCreateDocumentModal风格一致
function showChangePasswordModal() {
    if (window.passwordManager) {
        passwordManager.showChangePasswordModal();
    } else {
        console.error('passwordManager 未初始化');
        alert('系统正在初始化，请稍后重试');
    }
}

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    // 将密码管理器挂载到全局
    window.passwordManager = passwordManager;

    console.log('密码管理模块已加载');
});

// 确保全局可访问
window.showChangePasswordModal = showChangePasswordModal;
window.passwordManager = passwordManager;
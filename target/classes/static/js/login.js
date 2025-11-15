const API_BASE_URL = 'http://localhost:8080/api';

// 切换标签
function switchTab(tab) {
    if (tab === 'login') {
        document.getElementById('loginForm').classList.add('active');
        document.getElementById('registerForm').classList.remove('active');
        document.querySelectorAll('.tab-button')[0].classList.add('active');
        document.querySelectorAll('.tab-button')[1].classList.remove('active');
    } else {
        document.getElementById('registerForm').classList.add('active');
        document.getElementById('loginForm').classList.remove('active');
        document.querySelectorAll('.tab-button')[1].classList.add('active');
        document.querySelectorAll('.tab-button')[0].classList.remove('active');
    }
}

// 登录功能
async function login(e) {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    console.log('登录:', username, password);

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const result = await response.json();
        console.log('登录结果:', result);

        if (result.success) {
            // 保存用户信息到localStorage（仅用于显示）
            localStorage.setItem('currentUser', JSON.stringify(result.data));
            alert('登录成功！');
            window.location.href = '/'; // 跳转到首页
        } else {
            alert('登录失败: ' + result.message);
        }
    } catch (error) {
        console.error('登录错误:', error);
        alert('登录失败: ' + error.message);
    }
}

// 注册处理 - 使用公开接口
async function handleRegister(event) {
    event.preventDefault();
    console.log('开始注册...');

    const username = document.getElementById('regUsername').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;

    try {
        const response = await fetch('/public/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });

        const result = await response.json();
        console.log('注册响应:', result);

        if (result.success) {
            alert('注册成功！请登录');
            switchToLogin();
        } else {
            alert('注册失败: ' + result.message);
        }
    } catch (error) {
        console.error('注册错误:', error);
        alert('注册失败: ' + error.message);
    }
}

// 检查登录状态
window.addEventListener('DOMContentLoaded', () => {
    // 如果是登录页面，不检查
    if (window.location.pathname === '/login') {
        return;
    }

    // 简单检查localStorage
    const currentUser = localStorage.getItem('currentUser');
    if (!currentUser) {
        window.location.href = '/login';
    }
});
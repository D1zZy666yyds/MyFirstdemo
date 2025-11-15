import api from './index';

export const systemApi = {
    // 获取所有系统设置
    getAllSettings() {
        return api.get('/system/settings');
    },

    // 获取系统信息
    getSystemInfo() {
        return api.get('/system/info');
    },

    // 根据键获取设置
    getSettingByKey(key) {
        return api.get(`/system/settings/${key}`);
    },

    // 更新系统设置
    updateSetting(key, value, userId) {
        return api.put(`/system/settings/${key}`, {
            value,
            userId
        });
    },

    // 创建系统设置
    createSetting(setting, userId) {
        return api.post('/system/settings', setting, {
            params: { userId }
        });
    },

    // 删除系统设置
    deleteSetting(key, userId) {
        return api.delete(`/system/settings/${key}`, {
            params: { userId }
        });
    },

    // 初始化默认设置
    initializeSettings(userId) {
        return api.post('/system/initialize', null, {
            params: { userId }
        });
    }
};
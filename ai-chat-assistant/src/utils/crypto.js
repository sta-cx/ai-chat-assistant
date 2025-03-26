/**
 * 简单的加密工具类
 * 注意：这只是基本的加密，如果需要更高级的加密，建议使用专业的加密库
 */

const ENCRYPTION_KEY = import.meta.env.VITE_STORAGE_ENCRYPTION_KEY || 'default-key';

export const encrypt = (text) => {
  if (!text) return text;
  try {
    // 简单的凯撒密码加密，实际项目中应使用更强的加密算法
    const encrypted = text.split('').map(char => {
      const code = char.charCodeAt(0);
      return String.fromCharCode(code + 1);
    }).join('');
    return btoa(encrypted);
  } catch (error) {
    console.error('加密失败:', error);
    return text;
  }
};

export const decrypt = (text) => {
  if (!text) return text;
  try {
    const decrypted = atob(text).split('').map(char => {
      const code = char.charCodeAt(0);
      return String.fromCharCode(code - 1);
    }).join('');
    return decrypted;
  } catch (error) {
    console.error('解密失败:', error);
    return text;
  }
};

export const secureStorage = {
  setItem(key, value) {
    try {
      const encryptedValue = encrypt(JSON.stringify(value));
      localStorage.setItem(key, encryptedValue);
    } catch (error) {
      console.error('存储数据失败:', error);
    }
  },

  getItem(key) {
    try {
      const value = localStorage.getItem(key);
      if (!value) return null;
      const decryptedValue = decrypt(value);
      return JSON.parse(decryptedValue);
    } catch (error) {
      console.error('读取数据失败:', error);
      return null;
    }
  },

  removeItem(key) {
    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.error('删除数据失败:', error);
    }
  },

  clear() {
    try {
      localStorage.clear();
    } catch (error) {
      console.error('清除数据失败:', error);
    }
  }
}; 
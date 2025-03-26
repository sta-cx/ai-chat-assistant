import axios from 'axios'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 用于格式化Deepseek模型输出的辅助函数
export const formatDeepseekResponse = (text) => {
  if (!text) return text;
  
  // 1. 先移除思考过程标签内容
  let formatted = text.replace(/<think>[\s\S]*?<\/think>/g, '');
  
  // 2. 处理"5×5"这样的数学表达式，给乘法符号加上特殊标记
  formatted = formatted.replace(/(\d+)\s*(?:[×xX*]|\\times)\s*(\d+)/g, (match, p1, p2) => {
    return `${p1} × ${p2}`;
  });
  
  // 2.1 处理各种分数表达式
  formatted = formatted.replace(/(\d+)\s*[—–\-]\s*?\/\s*(\d+)/g, (match, p1, p2) => {
    return `${p1}/${p2}`;
  });
  
  // 3. 处理步骤标题
  formatted = formatted.replace(/###\s*(步骤\s*\d+|Step\s*\d+)[:：]?\s*(.*?)(?=\n|$)/gi, (match, step, title) => {
    return `\n### **${step}**：${title || ''}\n`;
  });
  
  // 4. 处理数学公式
  formatted = formatted.replace(/\\\[([\s\S]*?)\\\]/g, (match, content) => {
    return `\n$$${content.trim()}$$\n`;
  });
  
  return formatted;
};

// 获取聊天响应
export const getChatResponse = (message) => {
  return request({
    url: '/ai/chat',
    method: 'post',
    data: {
      message
    }
  });
};

// 使用EventSource实现流式打字效果聊天API
export const getStreamingChatResponse = async (message, userId = '1', onData, onThinking, onComplete) => {
  try {
    const response = await fetch('/api/ai/typingAsk', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        userId: userId,
        message: message
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP错误! 状态: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let thinkingMode = false;
    
    try {
      while (true) {
        const { value, done } = await reader.read();
        
        if (done) {
          onComplete && onComplete();
          break;
        }
        
        buffer += decoder.decode(value, { stream: true });
        
        const eventRegex = /data: (.*?)(?:\n\n|\r\n\r\n)/gs;
        let match;
        let newBuffer = buffer;
        
        while ((match = eventRegex.exec(buffer)) !== null) {
          const rawContent = match[1].trim();
          newBuffer = newBuffer.replace(match[0], '');
          
          if (rawContent.includes('<think>')) {
            const thinkContent = extractThinkContent(rawContent);
            if (thinkContent) {
              onThinking && onThinking(thinkContent);
              const nonThinkContent = removeThinkContent(rawContent);
              if (nonThinkContent && nonThinkContent.trim()) {
                const formattedContent = formatDeepseekResponse(nonThinkContent);
                onData && onData(formattedContent);
              }
              continue;
            }
          }
          
          if (rawContent === '[DONE]') {
            onComplete && onComplete();
            continue;
          }
          
          const formattedContent = formatDeepseekResponse(rawContent);
          onData && onData(formattedContent);
        }
        
        buffer = newBuffer;
      }
    } finally {
      reader.releaseLock();
    }
  } catch (error) {
    console.error('流式读取错误:', error);
    onData && onData(null, error);
    onComplete && onComplete();
  }
};

// 辅助函数：提取<think>标签中的内容
function extractThinkContent(text) {
  const thinkRegex = /<think>([\s\S]*?)<\/think>/g;
  let match;
  let extracted = '';
  
  while ((match = thinkRegex.exec(text)) !== null) {
    extracted += match[1].trim() + '\n';
  }
  
  return extracted.trim();
}

// 辅助函数：移除<think>标签及其内容
function removeThinkContent(text) {
  return text.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
} 
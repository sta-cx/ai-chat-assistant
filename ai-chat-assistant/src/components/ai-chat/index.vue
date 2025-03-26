<template>
  <div class="ai-chat-container" :class="{'ai-chat-collapsed': collapsed}">
    <transition name="fade">
      <div class="ai-chat-toggle" @click="toggleCollapse" :title="collapsed ? '展开AI助手' : '收起AI助手'">
        <el-icon>
          <component :is="collapsed ? 'ArrowRight' : 'ArrowLeft'" />
        </el-icon>
        <template v-if="!collapsed">
          <span class="toggle-text">收起</span>
        </template>
      </div>
    </transition>
    
    <transition name="slide-fade">
      <div class="ai-chat-content" v-if="!collapsed">
        <div class="ai-chat-header">
          <span><el-icon><Opportunity /></el-icon> AI助手</span>
          <div class="header-actions">
            <el-icon @click="clearChatHistory" title="清空聊天记录"><Delete /></el-icon>
            <el-icon @click="toggleCollapse"><Close /></el-icon>
          </div>
        </div>
        
        <div class="ai-chat-messages" ref="messageContainer">
          <transition-group name="message-list" tag="div">
            <div v-for="(message, index) in messages" 
                 :key="index" 
                 :class="['ai-chat-message', message.type === 'user' ? 'user-message' : 'ai-message']"
                 v-show="message.content.trim() || message.thinking">
              <div class="message-avatar" v-if="message.type === 'ai'">
                <div class="avatar-icon ai-avatar">AI</div>
              </div>
              <div class="message-avatar" v-else>
                <div class="avatar-icon user-avatar">
                  <el-icon><User /></el-icon>
                </div>
              </div>
              
              <div class="message-body">
                <div class="message-name">
                  {{ message.type === 'user' ? '您' : 'AI助手' }}
                  <span v-if="message.hasThinking && !message.thinking" 
                        class="thinking-toggle" 
                        @click="toggleThinking(index)">
                    <el-icon>
                      <component :is="message.showThinking ? 'ArrowUp' : 'ArrowDown'" />
                    </el-icon>
                    {{ message.showThinking ? '隐藏思考过程' : '查看思考过程' }}
                  </span>
                </div>
                
                <div class="message-content" v-if="!message.thinking && message.type === 'user'">
                  {{ message.content }}
                </div>
                
                <div class="message-content" v-if="!message.thinking && message.type === 'ai'">
                  <markdown-renderer :content="message.content"></markdown-renderer>
                </div>
                
                <transition name="thinking-fade">
                  <div class="message-thinking-container" v-if="message.hasThinking && message.showThinking">
                    <div class="message-content thinking-content">
                      <div class="thinking-label">
                        <el-icon><InfoFilled /></el-icon>
                        思考过程
                      </div>
                      <div class="thinking-text">
                        <markdown-renderer :content="message.thinkingContent"></markdown-renderer>
                      </div>
                    </div>
                  </div>
                </transition>
                
                <div class="message-content thinking-content" v-if="message.thinking">
                  <div class="thinking-label">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    模型思考中...
                  </div>
                  <div class="thinking-text">
                    <markdown-renderer :content="message.content"></markdown-renderer>
                  </div>
                </div>
              </div>
            </div>
          </transition-group>
          
          <div v-if="isProcessing" class="ai-chat-message ai-message">
            <div class="message-avatar">
              <div class="avatar-icon ai-avatar">AI</div>
            </div>
            <div class="message-body">
              <div class="message-name">AI助手</div>
              <div class="message-content typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="ai-chat-input">
          <el-input
            type="textarea"
            :rows="2"
            placeholder="请输入您的问题..."
            v-model="inputMessage"
            @keyup.enter.native="sendMessage"
            :disabled="isProcessing"
          ></el-input>
          <el-button type="primary" :icon="Promotion" @click="sendMessage" :loading="isProcessing">
            发送
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getStreamingChatResponse } from '@/api/ai-chat'
import MarkdownRenderer from '@/components/markdown/index.vue'
import { 
  Delete, 
  Close, 
  User, 
  Loading, 
  InfoFilled, 
  ArrowUp, 
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Opportunity
} from '@element-plus/icons-vue'

// 状态
const collapsed = ref(false)
const inputMessage = ref('')
const messages = ref([
  { 
    type: 'ai', 
    content: '您好，我是AI助手，有什么可以帮助您的？', 
    thinking: false, 
    hasThinking: false, 
    thinkingContent: '', 
    showThinking: false 
  }
])
const isProcessing = ref(false)
const messageContainer = ref(null)
const userId = ref(localStorage.getItem('userId') || '1')
const conversationId = ref(localStorage.getItem('aiChatConversationId') || Date.now().toString())

// 方法
const toggleCollapse = () => {
  collapsed.value = !collapsed.value
  nextTick(() => {
    window.dispatchEvent(new Event('resize'))
    window.dispatchEvent(new CustomEvent('ai-chat-visibility-changed', {
      detail: {
        visible: !collapsed.value
      }
    }))
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

const toggleThinking = (index) => {
  if (messages.value[index]) {
    messages.value[index].showThinking = !messages.value[index].showThinking
  }
}

const clearChatHistory = () => {
  messages.value = [
    { 
      type: 'ai', 
      content: '您好，我是AI助手，有什么可以帮助您的？', 
      thinking: false, 
      hasThinking: false, 
      thinkingContent: '', 
      showThinking: false 
    }
  ]
  conversationId.value = Date.now().toString()
  localStorage.setItem('aiChatConversationId', conversationId.value)
  localStorage.removeItem(`aiChat_history_${userId.value}`)
}

const saveChatHistory = () => {
  try {
    const messagesToSave = messages.value
      .filter(m => !m.thinking)
      .map(m => ({
        ...m,
        showThinking: false
      }))
    
    const historyToSave = {
      conversationId: conversationId.value,
      messages: messagesToSave.slice(-50)
    }
    
    localStorage.setItem(`aiChat_history_${userId.value}`, JSON.stringify(historyToSave))
  } catch (error) {
    console.error('保存聊天历史失败:', error)
  }
}

const loadChatHistory = () => {
  try {
    const savedHistory = localStorage.getItem(`aiChat_history_${userId.value}`)
    if (savedHistory) {
      const historyData = JSON.parse(savedHistory)
      if (historyData?.messages?.length > 0) {
        messages.value = historyData.messages
        conversationId.value = historyData.conversationId || conversationId.value
        localStorage.setItem('aiChatConversationId', conversationId.value)
        scrollToBottom()
      }
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error)
  }
}

const sendMessage = async (event) => {
  if (event?.type === 'keyup' && event.shiftKey) return
  
  const message = inputMessage.value.trim()
  if (!message || isProcessing.value) return
  
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: message,
    thinking: false,
    hasThinking: false,
    thinkingContent: '',
    showThinking: false
  })
  
  inputMessage.value = ''
  isProcessing.value = true
  scrollToBottom()
  
  // 添加AI消息占位
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    thinking: false,
    hasThinking: false,
    thinkingContent: '',
    showThinking: false
  })
  
  try {
    await getStreamingChatResponse(
      message,
      userId.value,
      (text) => {
        if (text === null) return
        messages.value[aiMessageIndex].content = text
        scrollToBottom()
      },
      (thinkingText) => {
        messages.value[aiMessageIndex].thinking = true
        messages.value[aiMessageIndex].hasThinking = true
        messages.value[aiMessageIndex].thinkingContent = thinkingText
        scrollToBottom()
      },
      () => {
        messages.value[aiMessageIndex].thinking = false
        isProcessing.value = false
        saveChatHistory()
        scrollToBottom()
      }
    )
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value[aiMessageIndex].content = '抱歉，发生了一些错误，请稍后重试。'
    messages.value[aiMessageIndex].thinking = false
    isProcessing.value = false
  }
}

// 生命周期
onMounted(() => {
  nextTick(() => {
    document.querySelector('.ai-chat-container').classList.add('ai-chat-ready')
    window.dispatchEvent(new CustomEvent('ai-chat-visibility-changed', {
      detail: {
        visible: !collapsed.value
      }
    }))
    loadChatHistory()
  })
})
</script>

<style scoped>
.ai-chat-container {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  display: flex;
  z-index: 1000;
  transition: all 0.3s ease;
}

.ai-chat-collapsed {
  width: 40px;
}

.ai-chat-content {
  width: 400px;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
}

.ai-chat-toggle {
  position: absolute;
  left: -40px;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  border-radius: 4px 0 0 4px;
}

.ai-chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
}

.ai-chat-header .header-actions {
  display: flex;
  gap: 12px;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.ai-chat-message {
  display: flex;
  margin-bottom: 16px;
  gap: 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.ai-avatar {
  background: #409eff;
  color: #fff;
}

.user-avatar {
  background: #67c23a;
  color: #fff;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.message-content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  word-break: break-word;
}

.thinking-content {
  background: #fff7e6;
}

.thinking-label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
  margin-bottom: 8px;
}

.thinking-toggle {
  margin-left: 8px;
  color: #409eff;
  cursor: pointer;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ai-chat-input {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 8px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  display: inline-block;
  animation: typing 1s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}

.thinking-fade-enter-active,
.thinking-fade-leave-active {
  transition: all 0.3s ease;
}

.thinking-fade-enter-from,
.thinking-fade-leave-to {
  max-height: 0;
  opacity: 0;
}

.message-list-enter-active,
.message-list-leave-active {
  transition: all 0.3s ease;
}

.message-list-enter-from,
.message-list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style> 
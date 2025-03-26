<template>
  <div class="markdown-content" v-html="renderedContent"></div>
</template>

<script setup>
import { ref, watchEffect } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

const props = defineProps({
  content: {
    type: String,
    required: true
  }
})

const renderedContent = ref('')

// 配置marked选项
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (e) {
        console.error(e)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// 监听content变化并更新渲染内容
watchEffect(() => {
  if (props.content) {
    renderedContent.value = marked(props.content)
  }
})
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  font-size: 14px;
}

.markdown-content :deep(p) {
  margin: 8px 0;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 16px 0 8px;
  font-weight: 600;
}

.markdown-content :deep(pre) {
  background-color: #282c34;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-content :deep(code) {
  font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', monospace;
  font-size: 13px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}

.markdown-content :deep(blockquote) {
  margin: 8px 0;
  padding: 0 16px;
  color: #666;
  border-left: 4px solid #ddd;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.markdown-content :deep(th) {
  background-color: #f5f5f5;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.markdown-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}
</style> 
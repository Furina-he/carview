<template>
  <div class="assistant-page">
    <DataCard title="智能助手" class="chat-container">
      <div class="chat-messages" ref="msgContainer">
        <div v-for="(msg, i) in messages" :key="i"
             class="chat-msg" :class="msg.role">
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="input" placeholder="请输入您的问题..."
                  @keyup.enter="sendMessage" />
        <el-button type="primary" @click="sendMessage">发送</el-button>
      </div>
    </DataCard>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import DataCard from '@/components/DataCard.vue'
import qaData from './data/qa.json'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([
  { role: 'assistant', content: '您好！我是 CarView 智能助手，请问有什么可以帮助您的？' }
])
const input = ref('')
const msgContainer = ref<HTMLElement | null>(null)

function findAnswer(question: string): string {
  const q = question.toLowerCase()
  for (const item of qaData as any[]) {
    for (const keyword of item.keywords) {
      if (q.includes(keyword.toLowerCase())) {
        return item.answer
      }
    }
  }
  return '抱歉，我暂时无法回答这个问题。您可以尝试问我关于超速规则、围栏设置、告警级别、油耗分析等方面的问题。'
}

async function sendMessage() {
  if (!input.value.trim()) return
  const question = input.value.trim()
  messages.value.push({ role: 'user', content: question })
  input.value = ''

  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }

  setTimeout(async () => {
    const answer = findAnswer(question)
    messages.value.push({ role: 'assistant', content: answer })
    await nextTick()
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  }, 500)
}
</script>

<style scoped lang="scss">
.assistant-page { padding: 12px; height: 100%; }

.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.chat-msg {
  display: flex;
  &.user { justify-content: flex-end; }
  &.assistant { justify-content: flex-start; }

  .msg-bubble {
    max-width: 70%;
    padding: 10px 14px;
    border-radius: 10px;
    font-size: 14px;
    line-height: 1.5;
  }

  &.user .msg-bubble {
    background: rgba(0, 242, 255, 0.15);
    color: #e6f7ff;
    border-bottom-right-radius: 2px;
  }

  &.assistant .msg-bubble {
    background: rgba(255, 255, 255, 0.06);
    color: rgba(230, 247, 255, 0.85);
    border-bottom-left-radius: 2px;
  }
}

.chat-input {
  display: flex;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 242, 255, 0.1);
}
</style>

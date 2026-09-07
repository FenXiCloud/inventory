<template>
  <div class="ai-assistant" :style="positionStyle">
    <!-- 悬浮按钮 -->
    <div
      class="ai-float-btn"
      @mousedown="startDrag"
      @click="handleClick"
    >
      <t-icon name="chat" />
      <span class="ai-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
    </div>

    <!-- 对话面板 -->
    <div class="ai-panel" v-show="isOpen">
      <div class="ai-panel-header" @mousedown="startDragPanel">
        <span>AI助手</span>
        <span class="ai-close-btn" @click.stop="togglePanel"><t-icon name="close" /></span>
      </div>

      <div class="ai-panel-body" ref="chatBody">
        <!-- 欢迎消息 -->
        <div class="ai-message ai-message-bot" v-if="messages.length === 0">
          <div class="ai-message-content">
            您好！我是AI助手，可以帮您快速创建销售订单。请告诉我：
            <br>• 客户名称
            <br>• 商品信息
            <br>• 数量和单价
            <br><br>例如："给张三开个销售单，买5箱可乐"
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, index) in messages" :key="index" class="ai-message" :class="msg.role === 'user' ? 'ai-message-user' : 'ai-message-bot'">
          <div class="ai-message-content" v-html="formatMessage(msg.content)"></div>
        </div>

        <!-- 加载状态 -->
        <div class="ai-message ai-message-bot" v-if="loading">
          <div class="ai-message-content">
            <t-loading size="small" />
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="ai-panel-footer">
        <div class="ai-input-area">
          <t-input
            v-model="inputText"
            placeholder="输入消息..."
            @enter="sendMessage"
            :disabled="loading"
          />
          <t-button theme="primary" @click="sendMessage" :loading="loading" :disabled="!inputText.trim()">
            发送
          </t-button>
        </div>
        <div class="ai-actions">
          <t-upload
            :show-upload-list="false"
            :before-upload="beforeImageUpload"
            @change="handleImageUpload"
            accept="image/*"
          >
            <t-button theme="default" variant="text" size="small">
              <t-icon name="image" /> 图片
            </t-button>
          </t-upload>
          <t-button theme="default" variant="text" size="small" @click="startVoice" :class="{ 'recording': isRecording }">
            <t-icon name="microphone" /> {{ isRecording ? '录音中...' : '语音' }}
          </t-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import AiApi from '@js/api/ai/AiAssistant';

export default {
  name: 'AiAssistant',
  setup() {
    const isOpen = ref(false);
    const messages = ref([]);
    const inputText = ref('');
    const loading = ref(false);
    const sessionId = ref(null);
    const chatBody = ref(null);
    const unreadCount = ref(0);
    const isRecording = ref(false);
    let mediaRecorder = null;
    let audioChunks = [];

    // 拖动相关状态
    const position = ref({ x: window.innerWidth - 80, y: window.innerHeight / 2 - 28 });
    const isDragging = ref(false);
    const dragStartPos = ref({ x: 0, y: 0 });
    const dragOffset = ref({ x: 0, y: 0 });
    const hasMoved = ref(false);

    // 计算位置样式
    const positionStyle = computed(() => ({
      left: position.value.x + 'px',
      top: position.value.y + 'px',
      transform: 'none'
    }));

    // 开始拖动悬浮按钮
    const startDrag = (e) => {
      if (e.button !== 0) return; // 只响应左键
      isDragging.value = true;
      hasMoved.value = false;
      dragStartPos.value = { x: e.clientX, y: e.clientY };
      dragOffset.value = {
        x: e.clientX - position.value.x,
        y: e.clientY - position.value.y
      };
      e.preventDefault();
    };

    // 开始拖动面板
    const startDragPanel = (e) => {
      if (e.button !== 0) return;
      isDragging.value = true;
      hasMoved.value = false;
      dragStartPos.value = { x: e.clientX, y: e.clientY };
      // 计算面板左上角位置
      const panelLeft = position.value.x - 380 - 16; // 面板宽度 + 间距
      const panelTop = position.value.y - 260; // 面板高度的一半
      dragOffset.value = {
        x: e.clientX - panelLeft,
        y: e.clientY - panelTop
      };
      e.preventDefault();
    };

    // 拖动中
    const onDrag = (e) => {
      if (!isDragging.value) return;

      const dx = e.clientX - dragStartPos.value.x;
      const dy = e.clientY - dragStartPos.value.y;

      // 如果移动距离超过5px，认为是拖动
      if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
        hasMoved.value = true;
      }

      let newX = e.clientX - dragOffset.value.x;
      let newY = e.clientY - dragOffset.value.y;

      // 限制在窗口范围内
      const btnSize = 56;
      newX = Math.max(0, Math.min(newX, window.innerWidth - btnSize));
      newY = Math.max(0, Math.min(newY, window.innerHeight - btnSize));

      position.value = { x: newX, y: newY };
    };

    // 结束拖动
    const endDrag = () => {
      isDragging.value = false;
    };

    // 点击处理（区分拖动和点击）
    const handleClick = () => {
      if (!hasMoved.value) {
        togglePanel();
      }
    };

    // 切换面板显示
    const togglePanel = () => {
      isOpen.value = !isOpen.value;
      if (isOpen.value) {
        unreadCount.value = 0;
        nextTick(() => scrollToBottom());
      }
    };

    // 滚动到底部
    const scrollToBottom = () => {
      if (chatBody.value) {
        chatBody.value.scrollTop = chatBody.value.scrollHeight;
      }
    };

    // 发送消息
    const sendMessage = async () => {
      const text = inputText.value.trim();
      if (!text || loading.value) return;

      messages.value.push({ role: 'user', content: text });
      inputText.value = '';
      loading.value = true;
      nextTick(() => scrollToBottom());

      try {
        const res = await AiApi.chat({
          sessionId: sessionId.value,
          message: text
        });

        if (res.success) {
          sessionId.value = res.data.sessionId;
          const aiResponse = res.data.aiResponse;
          messages.value.push({
            role: 'assistant',
            content: aiResponse.message || JSON.stringify(aiResponse)
          });
          handleAiAction(aiResponse);
        } else {
          messages.value.push({
            role: 'assistant',
            content: '抱歉，处理您的请求时出现错误。'
          });
        }
      } catch (error) {
        console.error('AI请求失败:', error);
        messages.value.push({
          role: 'assistant',
          content: '网络错误，请稍后重试。'
        });
      } finally {
        loading.value = false;
        nextTick(() => scrollToBottom());
      }
    };

    // 处理AI响应的action
    const handleAiAction = (aiResponse) => {
      switch (aiResponse.action) {
        case 'draft':
          if (aiResponse.data?.order) {
            MessagePlugin.success('订单草稿已生成，请确认后提交');
          }
          break;
        case 'ask':
          break;
        case 'error':
          MessagePlugin.error(aiResponse.message);
          break;
      }
    };

    // 格式化消息内容
    const formatMessage = (content) => {
      if (!content) return '';
      return content.replace(/\n/g, '<br>');
    };

    // 图片上传前处理
    const beforeImageUpload = (file) => {
      const isImage = file.type.startsWith('image/');
      const isLt10M = file.size / 1024 / 1024 < 10;

      if (!isImage) {
        MessagePlugin.error('只能上传图片文件');
        return false;
      }
      if (!isLt10M) {
        MessagePlugin.error('图片大小不能超过10MB');
        return false;
      }
      return true;
    };

    // 处理图片上传
    const handleImageUpload = async (context) => {
      const file = context.file;
      if (!file) return;

      loading.value = true;
      messages.value.push({ role: 'user', content: '[图片]' });
      nextTick(() => scrollToBottom());

      try {
        const reader = new FileReader();
        reader.onload = async (e) => {
          const base64 = e.target.result;
          const res = await AiApi.chat({
            sessionId: sessionId.value,
            message: '请识别这张图片中的订单信息：' + base64
          });

          if (res.success) {
            sessionId.value = res.data.sessionId;
            messages.value.push({
              role: 'assistant',
              content: res.data.aiResponse.message
            });
          }
          loading.value = false;
          nextTick(() => scrollToBottom());
        };
        reader.readAsDataURL(file);
      } catch (error) {
        console.error('图片处理失败:', error);
        messages.value.push({
          role: 'assistant',
          content: '图片处理失败，请重试。'
        });
        loading.value = false;
      }
    };

    // 开始语音录音
    const startVoice = async () => {
      if (isRecording.value) {
        stopVoice();
        return;
      }

      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];

        mediaRecorder.ondataavailable = (event) => {
          audioChunks.push(event.data);
        };

        mediaRecorder.onstop = async () => {
          const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
          MessagePlugin.info('语音识别功能开发中...');
          stream.getTracks().forEach(track => track.stop());
        };

        mediaRecorder.start();
        isRecording.value = true;
      } catch (error) {
        console.error('录音失败:', error);
        MessagePlugin.error('无法访问麦克风');
      }
    };

    // 停止语音录音
    const stopVoice = () => {
      if (mediaRecorder && isRecording.value) {
        mediaRecorder.stop();
        isRecording.value = false;
      }
    };

    // 监听消息变化，自动滚动
    watch(messages, () => {
      nextTick(() => scrollToBottom());
    }, { deep: true });

    // 挂载时添加全局事件监听
    onMounted(() => {
      document.addEventListener('mousemove', onDrag);
      document.addEventListener('mouseup', endDrag);
    });

    // 卸载时移除全局事件监听
    onUnmounted(() => {
      document.removeEventListener('mousemove', onDrag);
      document.removeEventListener('mouseup', endDrag);
    });

    return {
      isOpen,
      messages,
      inputText,
      loading,
      sessionId,
      chatBody,
      unreadCount,
      isRecording,
      positionStyle,
      togglePanel,
      sendMessage,
      formatMessage,
      beforeImageUpload,
      handleImageUpload,
      startVoice,
      stopVoice,
      startDrag,
      startDragPanel,
      handleClick
    };
  }
};
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  z-index: 9999;
}

.ai-float-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
  user-select: none;
}

.ai-float-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.ai-float-btn:active {
  transform: scale(0.95);
}

.ai-float-btn .t-icon {
  color: white;
  font-size: 24px;
  pointer-events: none;
}

.ai-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4d4f;
  color: white;
  font-size: 12px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.ai-panel {
  position: absolute;
  bottom: 0;
  right: 70px;
  width: 380px;
  height: 520px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-panel-header {
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  cursor: move;
  user-select: none;
}

.ai-close-btn {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.ai-close-btn .t-icon {
  font-size: 20px;
}

.ai-close-btn:hover {
  opacity: 0.8;
}

.ai-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}

.ai-message {
  margin-bottom: 12px;
  display: flex;
}

.ai-message-user {
  justify-content: flex-end;
}

.ai-message-bot {
  justify-content: flex-start;
}

.ai-message-content {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
}

.ai-message-user .ai-message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-message-bot .ai-message-content {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.ai-panel-footer {
  padding: 12px;
  border-top: 1px solid #e8e8e8;
  background: white;
}

.ai-input-area {
  display: flex;
  gap: 8px;
}

.ai-input-area .t-input {
  flex: 1;
}

.ai-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.ai-actions .t-button {
  color: #666;
}

.ai-actions .t-button:hover {
  color: #667eea;
}

.recording {
  color: #ff4d4f !important;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* 滚动条样式 */
.ai-panel-body::-webkit-scrollbar {
  width: 6px;
}

.ai-panel-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.ai-panel-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.ai-panel-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

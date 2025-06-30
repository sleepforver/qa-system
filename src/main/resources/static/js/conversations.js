async function loadConversations() {
    const conversationsList = document.getElementById('conversations-list');
    conversationsList.innerHTML = ''; // 清空现有会话列表

    try {
        const response = await fetch('/api/conversations');
        if (!response.ok) throw new Error('无法加载会话列表');

        const conversations = await response.json();

        conversations.forEach(conversation => {
            const conversationItem = document.createElement('div');
            conversationItem.className = 'conversation-item';
            conversationItem.dataset.conversationId = conversation.id;
            conversationItem.innerHTML = `
        <div class="conversation-header flex justify-between">
            <div>
                <span class="conversation-name">${conversation.name || '未命名会话'}</span>
                <span class="conversation-time">${new Date(conversation.start_time).toLocaleDateString()}</span>
            </div>
            <button class="conversation-delete text-red-500 cursor-pointer">🗑️</button>
        </div>
        <div class="conversation-summary">加载中...</div>
    `;

            // 删除按钮点击事件
            conversationItem.querySelector('.conversation-delete').addEventListener('click', (e) => {
                e.stopPropagation(); // 阻止冒泡
                if (confirm('确定删除该会话及所有聊天记录吗？')) {
                    deleteConversation(conversation.id);
                }
            });

            conversationItem.addEventListener('click', () => loadConversation(conversation.id));
            conversationsList.appendChild(conversationItem);

            // 🔁 自动加载摘要（方式一）
            // generateAndSetConversationSummary(conversation.id);
        });
    } catch (error) {
        console.error('加载会话失败:', error);
    }
}

// 📤 调用 /summary 接口并设置摘要
async function generateAndSetConversationSummary(conversationId) {
    try {
        const response = await fetch(`/api/conversation/${conversationId}/summary`);
        if (!response.ok) throw new Error('摘要加载失败');

        const summary = await response.text(); // 摘要返回的是 text/plain 类型
        const conversationItem = document.querySelector(`[data-conversation-id="${conversationId}"]`);
        const summaryElement = conversationItem.querySelector('.conversation-summary');
        if (summaryElement) {
            summaryElement.textContent = summary.length > 100 ? summary.substring(0, 100) + '...' : summary;
        }
    } catch (error) {
        console.error('生成摘要失败:', error);
    }
}

// 📥 加载指定会话内容
async function loadConversation(conversationId) {
    const chatBox = document.getElementById('chat-box');
    chatBox.innerHTML = ''; // 清空聊天框

    try {
        const response = await fetch(`/api/chat-history/${conversationId}`);
        if (!response.ok) throw new Error('无法加载会话内容');

        const chatHistory = await response.json();

        chatHistory.forEach(chat => {
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${chat.user_id ? 'user' : 'ai'}`;
            messageDiv.innerHTML = `
                <div class="message-content">
                    <p>${chat.user_message || chat.ai_response}</p>
                    <span class="message-time">${new Date(chat.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
            `;
            chatBox.appendChild(messageDiv);
        });

        chatBox.scrollTop = chatBox.scrollHeight;
    } catch (error) {
        console.error('加载会话内容失败:', error);
    }
}

// ➕ 创建新会话
async function createNewConversation() {
    try {
        const response = await fetch('/api/new-conversation', { method: 'POST' });
        if (!response.ok) throw new Error('创建会话失败');

        const data = await response.json();
        loadConversation(data.conversationId);
        loadConversations();
    } catch (error) {
        console.error('创建会话失败:', error);
    }
}

async function deleteConversation(conversationId) {
    try {
        const response = await fetch(`/api/delete-conversation/${conversationId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('删除失败');
        }

        // 重新加载会话列表
        loadConversations();

        // 如果当前会话是正在显示的会话，清空聊天框
        const currentId = localStorage.getItem('currentConversationId');
        if (currentId === conversationId.toString()) {
            document.getElementById('chat-box').innerHTML = '';
        }
    } catch (error) {
        console.error('删除会话失败:', error);
        alert('删除失败，请重试');
    }
}
// // 🧩 页面加载完成后执行初始化
// document.addEventListener('DOMContentLoaded', function () {
//     loadConversations();
//     const newBtn = document.getElementById('new-conversation-btn');
//     if (newBtn) {
//         newBtn.addEventListener('click', createNewConversation);
//     }
// });

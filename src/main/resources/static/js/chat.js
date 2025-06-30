document.addEventListener('DOMContentLoaded', function () {
    const chatForm = document.getElementById('chat-form');
    const promptInput = document.getElementById('prompt');
    const newConversationBtn = document.getElementById('new-conversation-btn');
    const conversationsList = document.getElementById('conversations-list');
    const chatBox = document.getElementById('chat-box');
    const loadingIndicator = document.getElementById('loading-indicator');

    // ✅ 自定义确认框相关元素（需确保 DOM 中存在 #confirmModal 等元素）
    const confirmModal = document.getElementById('confirmModal');
    const confirmMessage = document.getElementById('confirmMessage');
    const confirmYes = document.getElementById('confirmYes');
    const confirmNo = document.getElementById('confirmNo');

    let onConfirmCallback = null;

    // 显示确认框
    function showCustomConfirm(message, callback) {
        confirmMessage.textContent = message;
        confirmModal.classList.remove('hidden');
        confirmModal.classList.add('modal-enter');
        onConfirmCallback = callback;
    }

    // 隐藏确认框
    function hideCustomConfirm() {
        confirmModal.classList.add('hidden');
        confirmModal.classList.remove('modal-enter');
        onConfirmCallback = null;
    }

    // 确定按钮点击
    confirmYes.addEventListener('click', () => {
        if (onConfirmCallback) onConfirmCallback(true);
        hideCustomConfirm();
    });

    // 取消按钮点击
    confirmNo.addEventListener('click', () => {
        if (onConfirmCallback) onConfirmCallback(false);
        hideCustomConfirm();
    });

    // 点击遮罩层关闭
    confirmModal.addEventListener('click', (e) => {
        if (e.target === confirmModal) hideCustomConfirm();
    });

    // 显示/隐藏加载指示器
    function showLoading() {
        loadingIndicator.classList.remove('hidden');
    }

    function hideLoading() {
        loadingIndicator.classList.add('hidden');
    }

    // Load conversations when page loads
    loadConversations();

    // New conversation button click
    newConversationBtn.addEventListener('click', async function () {
        showLoading();
        try {
            const response = await fetch('/api/new-conversation', {
                method: 'POST'
            });
            const data = await response.json();
            const conversationId = data.conversationId;
            localStorage.setItem('currentConversationId', conversationId);
            loadConversations();
            loadChatHistory(conversationId);
        } catch (error) {
            console.error('创建新会话失败:', error);
            showError('创建新会话失败，请重试');
        } finally {
            hideLoading();
        }

        // 🔁 页面加载时自动新建会话
        const currentConversationId = localStorage.getItem('currentConversationId');
        if (!currentConversationId) {
            newConversationBtn.click(); // 触发新建会话逻辑
        } else {
            // 若已有会话ID但未加载内容（如刷新页面），加载当前会话
            loadChatHistory(currentConversationId);
        }
    });

    // 显示错误消息
    function showError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.innerHTML = `<i class="fas fa-exclamation-circle mr-2"></i>${message}`;
        chatBox.appendChild(errorDiv);
        setTimeout(() => {
            errorDiv.remove();
        }, 5000);
    }

    // 显示成功消息
    function showSuccess(message) {
        const successDiv = document.createElement('div');
        successDiv.className = 'success-message';
        successDiv.innerHTML = `<i class="fas fa-check-circle mr-2"></i>${message}`;
        chatBox.appendChild(successDiv);
        setTimeout(() => {
            successDiv.remove();
        }, 3000);
    }

    // Load conversations
    async function loadConversations() {
        try {
            const response = await fetch('/api/conversations');
            const conversations = await response.json();

            const limitedConversations = conversations.slice(0, 10);
            conversationsList.innerHTML = '';

            limitedConversations.forEach(conversation => {
                const li = document.createElement('li');
                li.className = 'conversation-item hover-lift';

                const button = document.createElement('button');
                button.className = 'w-full text-left text-white relative';
                button.innerHTML = `
                    <div class="conversation-name">${conversation.name}</div>
                    <div class="conversation-time">${formatDate(conversation.startTime)}</div>
                    <div class="conversation-summary">${conversation.summary || '新对话'}</div>
                `;
                button.dataset.conversationId = conversation.id;

                const deleteButton = document.createElement('button');
                deleteButton.className = 'conversation-delete';
                deleteButton.innerHTML = '<i class="fas fa-trash"></i>';
                deleteButton.dataset.conversationId = conversation.id;

                deleteButton.addEventListener('click', async (e) => {
                    e.stopPropagation();
                    showCustomConfirm('确定删除该会话及所有聊天记录吗？', async (confirmed) => {
                        if (confirmed) {
                            await deleteConversation(conversation.id);
                            loadConversations();
                        }
                    });
                });

                button.addEventListener('click', function () {
                    // 移除所有活动状态
                    document.querySelectorAll('.conversation-item').forEach(item => {
                        item.classList.remove('active');
                    });
                    // 添加活动状态到当前项
                    li.classList.add('active');
                    loadChatHistory(this.dataset.conversationId);
                });

                li.appendChild(button);
                li.appendChild(deleteButton);
                conversationsList.appendChild(li);
            });

            if (conversations.length > 10) {
                const showMoreButton = document.createElement('button');
                showMoreButton.className = 'w-full text-left text-white bg-white/10 hover:bg-white/20 rounded-lg p-3 transition-all duration-200';
                showMoreButton.innerHTML = '<i class="fas fa-chevron-down mr-2"></i>查看更多';
                showMoreButton.onclick = () => loadMoreConversations(conversations);
                conversationsList.appendChild(showMoreButton);
            }
        } catch (error) {
            console.error('加载会话列表失败:', error);
            showError('加载会话列表失败，请刷新页面重试');
        }
    }

    // 格式化日期函数
    function formatDate(dateStr) {
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) {
            return 'Invalid Date';
        }
        const now = new Date();
        const diffTime = Math.abs(now - date);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 1) {
            return '今天';
        } else if (diffDays === 2) {
            return '昨天';
        } else if (diffDays <= 7) {
            return `${diffDays - 1}天前`;
        } else {
            return date.toLocaleDateString();
        }
    }

    // 加载更多会话的函数
    async function loadMoreConversations(allConversations) {
        conversationsList.innerHTML = ''; // 清空现有内容

        allConversations.forEach(conversation => {
            const li = document.createElement('li');
            li.className = 'conversation-item hover-lift';

            const button = document.createElement('button');
            button.className = 'w-full text-left text-white';
            button.innerHTML = `
                <div class="conversation-name">${conversation.name}</div>
                <div class="conversation-time">${formatDate(conversation.startTime)}</div>
                <div class="conversation-summary">${conversation.summary || '新对话'}</div>
            `;
            button.dataset.conversationId = conversation.id;

            button.addEventListener('click', function () {
                loadChatHistory(this.dataset.conversationId);
            });

            li.appendChild(button);
            conversationsList.appendChild(li);
        });
    }

    // 通过会话ID加载会话
    async function loadChatHistory(conversationId) {
        try {
            localStorage.setItem('currentConversationId', conversationId);
            const response = await fetch(`/api/chat-history/${conversationId}`);
            const chatHistory = await response.json();
            chatBox.innerHTML = '';

            // 添加欢迎消息（如果没有聊天记录）
            if (chatHistory.length === 0) {
                const welcomeDiv = document.createElement('div');
                welcomeDiv.className = 'flex justify-center mb-8';
                welcomeDiv.innerHTML = `
                    <div class="bg-white/10 backdrop-blur-md rounded-2xl p-6 max-w-md text-center border border-white/20">
                        <div class="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-robot text-white text-2xl"></i>
                        </div>
                        <h3 class="text-white font-semibold mb-2">欢迎使用智能问答系统</h3>
                        <p class="text-white/70 text-sm">我是您的AI助手，有什么可以帮助您的吗？</p>
                    </div>
                `;
                chatBox.appendChild(welcomeDiv);
            }

            chatHistory.forEach(chat => {
                if (chat.userMessage) {
                    // 用户消息
                    const userMessageDiv = document.createElement('div');
                    userMessageDiv.className = 'message-container user';
                    userMessageDiv.innerHTML = `
                        <div class="message-user">
                            <div class="flex items-center mb-2">
                                <div class="user-avatar">
                                    <i class="fas fa-user text-white text-sm"></i>
                                </div>
                                <span class="font-semibold text-white/90">你</span>
                            </div>
                            <p class="text-white leading-relaxed">${chat.userMessage}</p>
                            <p class="message-time">${new Date(chat.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                        </div>
                    `;
                    chatBox.appendChild(userMessageDiv);
                }

                if (chat.aiResponse) {
                    // AI 回复
                    const aiMessageDiv = document.createElement('div');
                    aiMessageDiv.className = 'message-container';
                    aiMessageDiv.innerHTML = `
                        <div class="ai-avatar">
                            <i class="fas fa-robot text-white text-sm"></i>
                        </div>
                        <div class="message-deepseek">
                            <div class="flex items-center mb-2">
                                <span class="font-semibold text-white/90">AI助手</span>
                            </div>
                            <p class="text-white leading-relaxed">${chat.aiResponse}</p>
                            <p class="message-time">${new Date(chat.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                        </div>
                    `;
                    chatBox.appendChild(aiMessageDiv);
                }
            });

            chatBox.scrollTop = chatBox.scrollHeight;
        } catch (error) {
            console.error('加载聊天历史失败:', error);
            showError('加载聊天历史失败，请重试');
        }
    }

    // 发送消息
    chatForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        const prompt = promptInput.value.trim();
        if (!prompt) return;

        const conversationId = localStorage.getItem('currentConversationId');
        if (!conversationId) {
            showError('请先创建或选择一个会话');
            return;
        }

        promptInput.value = ''; // 立即清空输入框
        promptInput.focus(); // 重新聚焦输入框
        await sendMessage(prompt, conversationId); // 异步发送消息
    });

    // 发送消息
    async function sendMessage(prompt, conversationId) {
        // 添加用户消息
        const userMessageDiv = document.createElement('div');
        userMessageDiv.className = 'message-container user';
        userMessageDiv.innerHTML = `
            <div class="message-user">
                <div class="flex items-center mb-2">
                    <div class="user-avatar">
                        <i class="fas fa-user text-white text-sm"></i>
                    </div>
                    <span class="font-semibold text-white/90">你</span>
                </div>
                <p class="text-white leading-relaxed">${prompt}</p>
                <p class="message-time">${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
            </div>`;
        chatBox.appendChild(userMessageDiv);
        chatBox.scrollTop = chatBox.scrollHeight;

        // 添加AI消息容器
        const uniqueId = Date.now();
        const aiMessageDiv = document.createElement('div');
        aiMessageDiv.className = 'message-container';
        aiMessageDiv.innerHTML = `
            <div class="ai-avatar">
                <i class="fas fa-robot text-white text-sm"></i>
            </div>
            <div class="message-deepseek">
                <div class="flex items-center mb-2">
                    <span class="font-semibold text-white/90">AI助手</span>
                    <div class="ml-2 animate-pulse">
                        <i class="fas fa-circle text-xs text-white/50"></i>
                    </div>
                </div>
                <div id="ai-response-text-${uniqueId}" class="ai-response-text text-white leading-relaxed"></div>
                <p class="message-time" id="ai-response-time-${uniqueId}"></p>
            </div>`;
        chatBox.appendChild(aiMessageDiv);
        chatBox.scrollTop = chatBox.scrollHeight;

        const aiResponseText = document.getElementById(`ai-response-text-${uniqueId}`);
        const aiResponseTime = document.getElementById(`ai-response-time-${uniqueId}`);

        const controller = new AbortController();
        const idleTimeout = 15000; // 15秒空闲超时
        let timeoutHandler = null;
        function resetTimeout() {
            if (timeoutHandler) clearTimeout(timeoutHandler);
            timeoutHandler = setTimeout(() => {
                controller.abort();
            }, idleTimeout);
        }
        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream'
                },
                body: JSON.stringify({ prompt, conversationId }),
                signal: controller.signal
            });
            if (!response.body) {
                aiResponseText.innerHTML = "<span class='text-red-400'>服务器繁忙，请重新请求。</span>";
                return;
            }
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = "";
            let fullResponse = "";
            let done = false;
            resetTimeout(); // 启动时先设一次
            function updateResponseTime() {
                if (aiResponseTime) {
                    aiResponseTime.textContent = new Date().toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit'
                    });
                }
            }
            function formatResponse(text) {
                let cleaned = text
                    .replace(/#/g, '')
                    .replace(/\*\*/g, '')
                    .replace(/```/g, '')
                    .replace(/`/g, '')
                    .replace(/>/g, '')
                    .replace(/---/g, '')
                    .replace(/-\s/g, '')
                    .replace(/\*\s/g, '')
                    .replace(/•/g, '');
                return cleaned
                    .replace(/\n{2,}/g, '<br><br>')
                    .replace(/\n/g, ' ')
                    .replace(/(<br>\s*){2,}/g, '<br><br>')
                    .trim();
            }
            function processChunk(chunk) {
                buffer += chunk;
                const eventEnd = buffer.indexOf('\n\n');
                if (eventEnd === -1) return false;
                let eventData = buffer.substring(0, eventEnd).trim();
                buffer = buffer.substring(eventEnd + 2);
                if (eventData === '[DONE]') {
                    return true;
                }
                let cleanedData = eventData
                    .replace(/^(data:\s*)+|[\s\n]data:\s*/gi, '')
                    .replace(/\s+/g, ' ')
                    .trim();
                if (cleanedData) {
                    fullResponse += cleanedData + ' ';
                    aiResponseText.innerHTML = formatResponse(fullResponse.trim());
                    chatBox.scrollTop = chatBox.scrollHeight;
                }
                return false;
            }
            while (!done) {
                const { value, done: streamDone } = await reader.read();
                done = streamDone;
                if (value) {
                    const chunk = decoder.decode(value, { stream: !done });
                    const shouldBreak = processChunk(chunk);
                    resetTimeout(); // 每收到新数据就重置超时
                    if (shouldBreak) break;
                    while (buffer.includes('\n\n')) {
                        if (processChunk('')) break;
                    }
                }
            }
            if (timeoutHandler) clearTimeout(timeoutHandler); // 结束时清理
            if (buffer.trim()) {
                processChunk('');
            }
            updateResponseTime();
            loadConversations();
        } catch (error) {
            if (timeoutHandler) clearTimeout(timeoutHandler);
            if (error.name === 'AbortError') {
                aiResponseText.innerHTML = "<span class='text-red-400'>服务器繁忙，请重新请求。</span>";
            } else {
                aiResponseText.innerHTML = "<span class='text-red-400'>发送消息失败，请重试。</span>";
            }
            if (aiResponseTime) {
                aiResponseTime.textContent = new Date().toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit'
                });
            }
        }
    }

    // 删除会话
    async function deleteConversation(conversationId) {
        try {
            const response = await fetch(`/api/conversations/${conversationId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                showSuccess('会话删除成功');
                // 如果删除的是当前会话，清空聊天框
                const currentId = localStorage.getItem('currentConversationId');
                if (currentId === conversationId) {
                    localStorage.removeItem('currentConversationId');
                    chatBox.innerHTML = '';
                }
            } else {
                showError('删除会话失败，请重试');
            }
        } catch (error) {
            console.error('删除会话失败:', error);
            showError('删除会话失败，请重试');
        }
    }

    // 页面加载时自动加载当前会话
    const currentConversationId = localStorage.getItem('currentConversationId');
    if (currentConversationId) {
        loadChatHistory(currentConversationId);
    }
});

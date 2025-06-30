document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleSidebarBtn');
    const closeBtn = document.getElementById('closeSidebarBtn');

    toggleBtn.addEventListener('click', function() {
        sidebar.classList.remove('-translate-x-full');
        // 如果是首次打开且没有加载过数据，加载会话
        if(!sidebar.dataset.loaded) {
            loadMoreConversations();
            sidebar.dataset.loaded = 'true';
        }
    });

    closeBtn.addEventListener('click', function() {
        sidebar.classList.add('-translate-x-full');
    });

    // 实现无限滚动加载
    const recentConversations = document.getElementById('recent-conversations');
    let loading = false;
    let page = 1;
    const pageSize = 10;

    // 监听滚动事件实现无限加载
    sidebar.addEventListener('scroll', () => {
        if (sidebar.scrollTop + sidebar.clientHeight >= sidebar.scrollHeight - 10 && !loading) {
            loadMoreConversations(page++);
        }
    });

    // 加载更多会话的函数
    async function loadMoreConversations(currentPage = 1) {
        const loadingIndicator = document.getElementById('loading-indicator');
        loading = true;
        loadingIndicator.classList.remove('hidden');

        try {
            const response = await fetch(`/api/conversations?page=${currentPage}&size=${pageSize}`);
            if (!response.ok) throw new Error('加载失败');

            const conversations = await response.json();

            // 添加新会话
            conversations.forEach(conversation => {
                const conversationItem = createConversationItem(conversation);
                recentConversations.appendChild(conversationItem);
            });

            // 如果返回的数据量小于分页大小，说明已加载完所有数据
            if(conversations.length < pageSize) {
                // 移除滚动监听
                sidebar.removeEventListener('scroll', arguments.callee);
            }
        } catch (error) {
            console.error('加载更多会话失败:', error);
        } finally {
            loading = false;
            loadingIndicator.classList.add('hidden');
        }
    }

    // 创建会话项元素
    function createConversationItem(conversation) {
        const conversationItem = document.createElement('div');
        conversationItem.className = 'conversation-item';
        conversationItem.dataset.conversationId = conversation.id;
        conversationItem.innerHTML = `                <div class="conversation-header">
                    <span class="conversation-name">${conversation.name || '未命名会话'}</span>
                    <span class="conversation-time">${new Date(conversation.start_time).toLocaleDateString()}</span>
                </div>
                <div class="conversation-summary">加载中...</div>
            `;

        conversationItem.addEventListener('click', () => loadConversation(conversation.id));

        // 添加悬停效果
        conversationItem.addEventListener('mouseenter', () => {
            if(!conversation.summaryLoaded) {
                generateAndSetConversationSummary(conversation.id);
            }
        });

        return conversationItem;
    }
});

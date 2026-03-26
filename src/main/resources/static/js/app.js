const chatBox = document.getElementById("chatBox");
const historyList = document.getElementById("historyList");
const messageInput = document.getElementById("messageInput");
const sendButton = document.getElementById("sendButton");
const typingIndicator = document.getElementById("typingIndicator");
const newConversationButton = document.getElementById("newConversationButton");

const state = {
    currentConversationId: null,
    isStreaming: false
};

function escapeHtml(text) {
    return text
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatMessage(text) {
    if (!text) return "";

    let safe = escapeHtml(text);

    safe = safe.replace(/^### (.*)$/gm, "<h4>$1</h4>");
    safe = safe.replace(/^## (.*)$/gm, "<h3>$1</h3>");
    safe = safe.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");
    safe = safe.replace(/^\* (.*)$/gm, "<li>$1</li>");
    safe = safe.replace(/(<li>.*<\/li>)/gs, "<ul>$1</ul>");
    safe = safe.replace(/\n/g, "<br>");

    return safe;
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function nextFrame() {
    return new Promise(resolve => requestAnimationFrame(resolve));
}

function autoResizeTextarea() {
    messageInput.style.height = "auto";
    messageInput.style.height = `${Math.min(messageInput.scrollHeight, 180)}px`;
}

function scrollChatToBottom() {
    chatBox.scrollTop = chatBox.scrollHeight;
}

function createMessageBubble(sender, content = "") {
    const messageDiv = document.createElement("div");
    messageDiv.classList.add("message", sender);

    const bubbleDiv = document.createElement("div");
    bubbleDiv.classList.add("bubble");
    bubbleDiv.innerHTML = formatMessage(content);

    messageDiv.appendChild(bubbleDiv);
    chatBox.appendChild(messageDiv);

    scrollChatToBottom();
    return bubbleDiv;
}

function addMessage(content, sender) {
    createMessageBubble(sender, content);
}

function resetChatBox() {
    chatBox.innerHTML = "";
    addMessage("Bonjour 👋 Je suis ton assistant d’orientation scolaire. Pose-moi une question.", "bot");
}

function setStreamingState(isStreaming) {
    state.isStreaming = isStreaming;
    sendButton.disabled = isStreaming;
    messageInput.disabled = isStreaming;
    typingIndicator.classList.toggle("hidden", !isStreaming);
}

function highlightActiveConversation() {
    document.querySelectorAll(".history-item").forEach(item => {
        const id = Number(item.dataset.id);
        item.classList.toggle("active", id === state.currentConversationId);
    });
}

async function loadConversations() {
    try {
        const response = await fetch("/api/conversations");
        const conversations = await response.json();

        historyList.innerHTML = "";

        if (!Array.isArray(conversations) || conversations.length === 0) {
            historyList.innerHTML = `<p class="history-empty">Aucune conversation enregistrée.</p>`;
            return;
        }

        conversations.forEach(conversation => {
            const item = document.createElement("div");
            item.className = "history-item";
            item.dataset.id = conversation.id;

            item.innerHTML = `
                <div class="history-row">
                    <div>
                        <div class="history-title">Conversation #${conversation.id}</div>
                        <div class="history-date">${new Date(conversation.createdAt).toLocaleString("fr-FR")}</div>
                    </div>
                    <button class="delete-btn" title="Supprimer">🗑</button>
                </div>
            `;

            item.addEventListener("click", async () => {
                state.currentConversationId = conversation.id;
                await loadMessages(conversation.id);
                highlightActiveConversation();
            });

            const deleteBtn = item.querySelector(".delete-btn");
            deleteBtn.addEventListener("click", async (event) => {
                event.stopPropagation();

                const confirmed = window.confirm("Supprimer cette conversation ?");
                if (!confirmed) return;

                try {
                    await fetch(`/api/conversations/${conversation.id}`, {
                        method: "DELETE"
                    });

                    if (state.currentConversationId === conversation.id) {
                        state.currentConversationId = null;
                        resetChatBox();
                    }

                    await loadConversations();
                    highlightActiveConversation();
                } catch (error) {
                    console.error("Erreur suppression :", error);
                }
            });

            historyList.appendChild(item);
        });

        highlightActiveConversation();
    } catch (error) {
        console.error("Erreur chargement historique :", error);
    }
}

async function loadMessages(conversationId) {
    try {
        const response = await fetch(`/api/conversations/${conversationId}/messages`);
        const messages = await response.json();

        chatBox.innerHTML = "";

        if (!Array.isArray(messages) || messages.length === 0) {
            resetChatBox();
            return;
        }

        messages.forEach(message => {
            const sender = message.role === "USER" ? "user" : "bot";
            addMessage(message.content, sender);
        });
    } catch (error) {
        console.error("Erreur chargement messages :", error);
    }
}

function startNewConversation() {
    state.currentConversationId = null;
    resetChatBox();
    highlightActiveConversation();
    messageInput.focus();
}

async function renderChunkProgressively(botBubble, fullTextRef, textChunk) {
    for (let i = 0; i < textChunk.length; i++) {
        fullTextRef.value += textChunk[i];
        botBubble.innerHTML = formatMessage(fullTextRef.value);
        scrollChatToBottom();

        await nextFrame();
        await delay(8);
    }
}

function extractSseData(line) {
    if (!line.startsWith("data:")) {
        return null;
    }

    let data = line.slice(5);

    // SSE autorise un espace optionnel après "data:"
    if (data.startsWith(" ")) {
        data = data.slice(1);
    }

    return data;
}

async function sendStreamingMessage() {
    const message = messageInput.value.trim();

    if (!message || state.isStreaming) {
        return;
    }

    addMessage(message, "user");
    messageInput.value = "";
    autoResizeTextarea();

    const botBubble = createMessageBubble("bot", "");
    setStreamingState(true);

    try {
        const response = await fetch("/api/chat/stream", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                message: message,
                conversationId: state.currentConversationId
            })
        });

        const conversationIdHeader = response.headers.get("X-Conversation-Id");
        if (conversationIdHeader) {
            state.currentConversationId = Number(conversationIdHeader);
        }

        if (!response.ok || !response.body) {
            const errorText = await response.text();
            botBubble.innerHTML = formatMessage(errorText || "Erreur serveur.");
            await loadConversations();
            highlightActiveConversation();
            return;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");

        let buffer = "";
        const fullTextRef = { value: "" };

        while (true) {
            const { done, value } = await reader.read();

            if (done) break;

            buffer += decoder.decode(value, { stream: true });

            let boundaryIndex;
            while ((boundaryIndex = buffer.indexOf("\n\n")) !== -1) {
                const eventChunk = buffer.slice(0, boundaryIndex);
                buffer = buffer.slice(boundaryIndex + 2);

                const lines = eventChunk.split("\n");
                for (const line of lines) {
                    const data = extractSseData(line);
                    if (data === null) continue;
                    if (data === "[DONE]") continue;

                    const textChunk = data.replace(/\\n/g, "\n");
                    await renderChunkProgressively(botBubble, fullTextRef, textChunk);
                }
            }
        }

        if (buffer.length > 0) {
            const lines = buffer.split("\n");
            for (const line of lines) {
                const data = extractSseData(line);
                if (data === null) continue;
                if (data === "[DONE]") continue;

                const textChunk = data.replace(/\\n/g, "\n");
                await renderChunkProgressively(botBubble, fullTextRef, textChunk);
            }
        }

        await loadConversations();
        highlightActiveConversation();

    } catch (error) {
        console.error("Erreur streaming :", error);
        botBubble.innerHTML = formatMessage("Impossible de contacter le serveur ou l’IA.");
    } finally {
        setStreamingState(false);
        messageInput.focus();
    }
}

sendButton.addEventListener("click", sendStreamingMessage);

messageInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        sendStreamingMessage();
    }
});

messageInput.addEventListener("input", autoResizeTextarea);
newConversationButton.addEventListener("click", startNewConversation);

resetChatBox();
autoResizeTextarea();
loadConversations();
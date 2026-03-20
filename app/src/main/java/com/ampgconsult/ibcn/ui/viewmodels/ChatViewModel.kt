package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AIProvider
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.ChatMessage
import com.ampgconsult.ibcn.data.models.MarketplaceAsset
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiProvider: AIProvider
) : ViewModel() {

    private val _chatId = MutableStateFlow<String?>(null)
    val currentUserId = auth.currentUser?.uid ?: ""
    
    private val _isSellerTyping = MutableStateFlow(false)
    val isSellerTyping: StateFlow<Boolean> = _isSellerTyping.asStateFlow()

    private var currentAsset: MarketplaceAsset? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _chatId
        .filterNotNull()
        .flatMapLatest { id ->
            callbackFlow {
                val subscription = firestore.collection("messages")
                    .document(id)
                    .collection("chats")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                        trySend(messages)
                        
                        if (messages.isNotEmpty()) {
                            val lastMsg = messages.last()
                            if (lastMsg.senderId != currentUserId && !lastMsg.isAI) {
                                checkForAIResponse(id, lastMsg.text)
                            }
                        }
                    }
                awaitClose { subscription.remove() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setChatId(id: String) {
        _chatId.value = id
        fetchAssetContext(id)
    }

    private fun fetchAssetContext(chatId: String) {
        viewModelScope.launch {
            val room = firestore.collection("messages").document(chatId).get().await()
            val assetId = room.getString("assetContextId")
            if (assetId != null) {
                val assetDoc = firestore.collection("marketplace_assets").document(assetId).get().await()
                currentAsset = assetDoc.toObject(MarketplaceAsset::class.java)
            }
        }
    }

    fun sendMessage(text: String, isAI: Boolean = false) {
        val id = _chatId.value ?: return
        val uid = if (isAI) "ai_seller_bot" else (auth.currentUser?.uid ?: return)
        
        viewModelScope.launch {
            val messageDoc = firestore.collection("messages")
                .document(id)
                .collection("chats")
                .document()
            
            val message = ChatMessage(
                id = messageDoc.id,
                senderId = uid,
                senderName = if (isAI) "AI Assistant (Seller)" else (auth.currentUser?.displayName ?: "User"),
                text = text,
                timestamp = Timestamp.now(),
                isAI = isAI
            )
            
            messageDoc.set(message).await()
            
            firestore.collection("messages").document(id).update(
                mapOf(
                    "lastMessage" to text,
                    "updatedAt" to Timestamp.now(),
                    "lastMessageTimestamp" to Timestamp.now()
                )
            ).await()
        }
    }

    private fun checkForAIResponse(chatId: String, buyerMessage: String) {
        viewModelScope.launch {
            val asset = currentAsset ?: return@launch
            
            _isSellerTyping.value = true
            delay(1500)

            val prompt = """
                You are the AI Assistant for the seller of this asset:
                Title: ${asset.title}
                Description: ${asset.description}
                Price: ${asset.price} USD
                
                Buyer asked: $buyerMessage
                
                Reply professionally as the seller. Be helpful and concise.
            """.trimIndent()

            val response = aiProvider.getResponse(prompt, AgentType.PRODUCT_MANAGER)
            response.onSuccess { reply ->
                sendMessage(reply, isAI = true)
            }
            _isSellerTyping.value = false
        }
    }
}

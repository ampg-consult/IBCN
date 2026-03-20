package com.ampgconsult.ibcn.modules.knowledge_vault.repositories

import com.ampgconsult.ibcn.modules.knowledge_vault.models.VaultDocument
import com.ampgconsult.ibcn.modules.knowledge_vault.models.VaultNote
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeVaultRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun saveDocument(doc: VaultDocument): Result<Unit> {
        return try {
            firestore.collection("knowledge_vault_docs").document(doc.id).set(doc).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNote(note: VaultNote): Result<Unit> {
        return try {
            firestore.collection("knowledge_vault_notes").document(note.id).set(note).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocuments(authorUid: String): Result<List<VaultDocument>> {
        return try {
            val snapshot = firestore.collection("knowledge_vault_docs")
                .whereEqualTo("authorUid", authorUid).get().await()
            Result.success(snapshot.toObjects(VaultDocument::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchDocumentsByTag(tag: String): Result<List<VaultDocument>> {
        return try {
            val snapshot = firestore.collection("knowledge_vault_docs")
                .whereArrayContains("tags", tag).get().await()
            Result.success(snapshot.toObjects(VaultDocument::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

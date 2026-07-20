package app.springdata.coordinator.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.view.obj_library.objective_type.ObjectiveType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectiveTypesRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun getObjectiveTypesByLevel(level: ObjLevel): Result<List<ObjectiveType>> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val querySnapshot = firestore.collection("objective_type")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("level", level.toString())
                .get()
                .await()

            val objectiveTypes = querySnapshot.documents.mapNotNull { document ->
                document.getString("name")?.let { name ->
                    ObjectiveType(
                        id = document.id.hashCode().toLong(), // Use document ID hash as Long ID
                        name = name,
                        level = level.toString()
                    )
                }
            }

            Result.success(objectiveTypes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllObjectiveTypes(): Result<List<ObjectiveType>> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val querySnapshot = firestore.collection("objective_type")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val objectiveTypes = querySnapshot.documents.mapNotNull { document ->
                val name = document.getString("name")
                val level = document.getString("level")
                if (name != null && level != null) {
                    ObjectiveType(
                        id = document.id.hashCode().toLong(), // Use document ID hash as Long ID
                        name = name,
                        level = level
                    )
                } else null
            }

            Result.success(objectiveTypes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addObjectiveType(name: String, level: ObjLevel): Result<String> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(Exception("Objective type name cannot be empty"))
        }

        return try {
            // Check if name already exists for this level and user
            val existingQuerySnapshot = firestore.collection("objective_type")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("level", level.toString())
                .whereEqualTo("normalizedName", trimmedName.lowercase())
                .limit(1)
                .get()
                .await()

            if (!existingQuerySnapshot.isEmpty) {
                return Result.failure(Exception("An objective type with this name already exists for ${level.toString().lowercase()} level"))
            }

            val objectiveTypeData = hashMapOf(
                "name" to trimmedName,
                "normalizedName" to trimmedName.lowercase(),
                "level" to level.toString(),
                "userId" to currentUser.uid,
                "createdAt" to System.currentTimeMillis()
            )

            val documentRef = firestore.collection("objective_type")
                .add(objectiveTypeData)
                .await()

            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteObjectiveType(id: Long): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            // Since we're using document ID hash as the ID, we need to find the document
            // by querying for all user's objective types and finding the one with matching hash
            val querySnapshot = firestore.collection("objective_type")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val documentToDelete = querySnapshot.documents.find { document ->
                document.id.hashCode().toLong() == id
            }

            if (documentToDelete != null) {
                firestore.collection("objective_type")
                    .document(documentToDelete.id)
                    .delete()
                    .await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Objective type not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
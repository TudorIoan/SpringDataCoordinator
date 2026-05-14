package net.abaresults.progresspath.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import net.abaresults.progresspath.model.ObjItem
import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective
import net.abaresults.progresspath.model.ObjectiveType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectiveRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val orgRepo: OrgRepository,
    private val kidObjectiveRepo: KidObjectiveRepository
) {

    suspend fun addObjective(objectiveType: ObjectiveType, objectiveName: String, items: List<ObjItem>, level: ObjLevel = ObjLevel.BEGINNER): Result<Objective> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        val trimmedObjectiveName = objectiveName.trim()
        if (trimmedObjectiveName.isEmpty()) {
            return Result.failure(Exception("Objective name cannot be empty."))
        }

        // Basic validation for items (you might want more complex validation)
        if (items.any { it.name.trim().isEmpty() }) {
            return Result.failure(Exception("Objective item names cannot be empty."))
        }

        try {
            // Check name and type is unique
            val querySnapshot = firestore.collection("objectives")
                .whereEqualTo("normalizedName", objectiveName.trim().lowercase())
                .whereEqualTo("type", objectiveType.toString())
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                return Result.failure(Exception("An Objective with the type '${objectiveType.displayName}' and name '$objectiveName' already exists."))
            }

            val objectiveDocumentRef = firestore.collection("objectives").document()
            val processedItems = items.map { item ->
                item.copy(
                    name = item.name.trim(),
                    normalizedName = item.name.trim().lowercase()
                )
            }

            val newObjective = Objective(
                id = objectiveDocumentRef.id,
                ownerUid = currentUser.uid,
                name = trimmedObjectiveName,
                normalizedName = trimmedObjectiveName.lowercase(),
                type = objectiveType,
                level = level,
                itemsList = processedItems.toMutableList()
            )

            objectiveDocumentRef.set(newObjective).await()
            return Result.success(objectiveDocumentRef.get().await().toObject(Objective::class.java)!!)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun updateObjective(objective: Objective): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        // Validate the incoming objective data
        val trimmedObjectiveName = objective.name.trim()
        if (trimmedObjectiveName.isEmpty()) {
            return Result.failure(Exception("Objective name cannot be empty."))
        }
        if (objective.id.isEmpty()) {
            return Result.failure(Exception("Objective ID cannot be empty for an update."))
        }
        // Basic validation for items
        if (objective.itemsList.any { it.name.trim().isEmpty() }) {
            return Result.failure(Exception("Objective item names cannot be empty."))
        }

        try {
            val objectiveDocumentRef = firestore.collection("objectives").document(objective.id)

            // Ensure ObjItem names are trimmed and normalizedName is consistent
            val processedItems = objective.itemsList.map { item ->
                item.copy(
                    name = item.name.trim(),
                    normalizedName = item.name.trim().lowercase()
                )
            }

            val updatedObjectiveData = objective.copy(
                name = trimmedObjectiveName,
                normalizedName = trimmedObjectiveName.lowercase(),
                itemsList = processedItems.toMutableList()
            )

            objectiveDocumentRef.set(updatedObjectiveData).await()

            // Update all related KidObjectives with new items from the objective
            val updateResult = kidObjectiveRepo.updateAllKidObjectives(updatedObjectiveData)
            if (updateResult.isFailure) {
                // Log the error but don't fail the entire operation since the objective was successfully updated
                // You might want to add proper logging here
                println("Warning: Failed to update related KidObjectives: ${updateResult.exceptionOrNull()?.message}")
            }

            return Result.success(Unit)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun fetchObjectivesByIds(objectiveIds: List<String>): Result<List<Objective>> {
        if (objectiveIds.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val querySnapshot = firestore.collection("objectives")
                .whereIn(FieldPath.documentId(), objectiveIds)
                .get()
                .await()

            val objectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Objective::class.java)
            }
            Result.success(objectives)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findObjectiveByNameAndType(normalizedName: String, objectiveType: ObjectiveType): Result<Objective?> {
        return try {
            val querySnapshot = firestore.collection("objectives")
                .whereEqualTo("normalizedName", normalizedName)
                .whereEqualTo("type", objectiveType.toString())
                .limit(1)
                .get()
                .await()

            val objective = querySnapshot.documents.firstOrNull()?.toObject(Objective::class.java)
            Result.success(objective)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchObjectivesByType(objectiveType: ObjectiveType, ownerUid: String? = null): Result<List<Objective>> {
        return try {
            var query = firestore.collection("objectives")
                .whereEqualTo("type", objectiveType.toString())

            if (ownerUid != null) {
                query = query.whereEqualTo("ownerUid", ownerUid)
            }

            val querySnapshot = query.get().await()

            val objectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Objective::class.java)
            }
            Result.success(objectives)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllObjectives(userId: String? = null): Result<List<Objective>> {
        return try {
            val query = if (userId != null) {
                firestore.collection("objectives")
                    .whereEqualTo("ownerUid", userId)
            } else {
                firestore.collection("objectives")
            }

            val querySnapshot = query.get().await()

            val objectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Objective::class.java)
            }
            Result.success(objectives)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeObjective(objectiveId: String): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        if (objectiveId.isEmpty()) {
            return Result.failure(Exception("Objective ID cannot be empty"))
        }

        return try {
            // First remove all related kidObjectives
            val kidObjectivesResult = kidObjectiveRepo.removeKidObjectives(objectiveId)
            if (kidObjectivesResult.isFailure) {
                return Result.failure(Exception("Failed to remove related kid objectives: ${kidObjectivesResult.exceptionOrNull()?.message}"))
            }

            // Then remove the objective itself
            val objectiveDocumentRef = firestore.collection("objectives").document(objectiveId)
            objectiveDocumentRef.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package app.springdata.coordinator.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import app.springdata.coordinator.model.KidObjective
import app.springdata.coordinator.model.KidObjectiveItem
import app.springdata.coordinator.model.Objective
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KidObjectiveRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) {

    suspend fun addKidObjective(kidId: String, objective: Objective, isActive: Boolean = false, consecutiveYesses: Int? = null): Result<String> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        if (kidId.isBlank()) {
            return Result.failure(Exception("Kid ID cannot be empty."))
        }

        try {
            // Check if this kid-objective combination already exists
            val querySnapshot = firestore.collection("kid_objectives")
                .whereEqualTo("kidId", kidId)
                .whereEqualTo("objectiveId", objective.id)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                return Result.failure(Exception("This objective is already assigned to this kid."))
            }

            val kidObjectiveDocumentRef = firestore.collection("kid_objectives").document()
            val newKidObjective = KidObjective(
                id = kidObjectiveDocumentRef.id,
                kidId = kidId,
                itemsList = objective.itemsList.map { objItem -> KidObjectiveItem(objItem) },
                objectiveId = objective.id,
                active = isActive,
                consecutiveYesses = consecutiveYesses,
                addedByUid = currentUser.uid
            )

            kidObjectiveDocumentRef.set(newKidObjective).await()
            return Result.success(kidObjectiveDocumentRef.id)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getKidObjectives(kidId: String): Result<List<KidObjective>> {
        if (kidId.isBlank()) {
            return Result.failure(Exception("Kid ID cannot be empty."))
        }

        return try {
            val querySnapshot = firestore.collection("kid_objectives")
                .whereEqualTo("kidId", kidId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val kidObjectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(KidObjective::class.java)
            }

            // Populate lastModificationByUserName for each item
            val kidObjectivesWithUserNames = kidObjectives.map { kidObjective ->
                populateUserNamesForItems(kidObjective)
            }

            Result.success(kidObjectivesWithUserNames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveKidObjectives(kidId: String): Result<List<KidObjective>> {
        if (kidId.isBlank()) {
            return Result.failure(Exception("Kid ID cannot be empty."))
        }

        return try {
            val querySnapshot = firestore.collection("kid_objectives")
                .whereEqualTo("kidId", kidId)
                .whereEqualTo("active", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val kidObjectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(KidObjective::class.java)
            }

            // Populate lastModificationByUserName for each item
            val kidObjectivesWithUserNames = kidObjectives.map { kidObjective ->
                populateUserNamesForItems(kidObjective)
            }

            Result.success(kidObjectivesWithUserNames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getKidObjectivesForObjective(objectiveId: String): Result<List<KidObjective>> {
        if (objectiveId.isBlank()) {
            return Result.failure(Exception("Objective ID cannot be empty."))
        }

        return try {
            val querySnapshot = firestore.collection("kid_objectives")
                .whereEqualTo("objectiveId", objectiveId)
                .get()
                .await()

            val kidObjectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(KidObjective::class.java)
            }

            Result.success(kidObjectives)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateKidObjective(kidObjective: KidObjective): Result<Unit> {
        if (kidObjective.id.isBlank()) {
            return Result.failure(Exception("Kid Objective ID cannot be empty."))
        }

        return try {
            firestore.collection("kid_objectives")
                .document(kidObjective.id)
                .set(kidObjective)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeKidObjective(kidObjectiveId: String): Result<Unit> {
        if (kidObjectiveId.isBlank()) {
            return Result.failure(Exception("Kid Objective ID cannot be empty."))
        }

        return try {
            firestore.collection("kid_objectives")
                .document(kidObjectiveId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAllKidObjectives(objective: Objective): Result<Unit> {
        try {
            // Get all KidObjectives that correspond to this objective
            val querySnapshot = firestore.collection("kid_objectives")
                .whereEqualTo("objectiveId", objective.id)
                .get()
                .await()

            val kidObjectives = querySnapshot.documents.mapNotNull { document ->
                document.toObject(KidObjective::class.java)
            }

            // Update each KidObjective with newly added items
            for (kidObjective in kidObjectives) {
                val existingItemKeys = kidObjective.itemsList.map { it.objItem.normalizedName to it.objItem.type }.toSet()
                val newObjectiveItemKeys = objective.itemsList.map { it.normalizedName to it.type }.toSet()

                val updatedItemsList = kidObjective.itemsList.toMutableList()

                // Add new items from objective that don't exist in kidObjective (matched by name + type)
                for (objItem in objective.itemsList) {
                    val key = objItem.normalizedName to objItem.type
                    if (!existingItemKeys.contains(key)) {
                        val newKidObjectiveItem = KidObjectiveItem(
                            objItem = objItem,
                            active = false,
                            lastResponseTime = null,
                            lastModificationByUserId = null
                        )
                        updatedItemsList.add(newKidObjectiveItem)
                    }
                }

                // Remove items from kidObjective that are no longer in updated objective (matched by name + type)
                for (kidObjItem in kidObjective.itemsList) {
                    val key = kidObjItem.objItem.normalizedName to kidObjItem.objItem.type
                    if (!newObjectiveItemKeys.contains(key)) {
                        updatedItemsList.removeIf {
                            it.objItem.normalizedName to it.objItem.type == key
                        }
                    }
                }

                // Update the KidObjective with the new items list
                val updatedKidObjective = kidObjective.copy(itemsList = updatedItemsList)

                // Save the updated KidObjective to Firestore
                firestore.collection("kid_objectives")
                    .document(kidObjective.id)
                    .set(updatedKidObjective)
                    .await()
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun removeKidObjectives(objectiveId: String): Result<Unit> {
        return try {
            val querySnapshot = firestore.collection("kid_objectives")
                .whereEqualTo("objectiveId", objectiveId)
                .get()
                .await()

            if (querySnapshot.documents.isNotEmpty()) {
                val batch = firestore.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun populateUserNamesForItems(kidObjective: KidObjective): KidObjective {
        val updatedItemsList = kidObjective.itemsList.map { item ->
            item.lastModificationByUserName = item.lastModificationByUserId?.let { userId ->
                userRepository.therapistUsers.find { it.ownerUid == userId }?.name
            }
            item
        }
        return kidObjective.copy(itemsList = updatedItemsList)
    }
}

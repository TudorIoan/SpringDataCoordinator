package net.abaresults.progresspath.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import net.abaresults.progresspath.model.Clinic
import net.abaresults.progresspath.model.InvitedTherapist
import net.abaresults.progresspath.model.Kid
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KidRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val orgRepo: OrgRepository
) {

    suspend fun addKid(kidName: String): Result<String> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        val selectedClinicId = try {
            orgRepo.requireSelectedClinic().id
        } catch (e: IllegalStateException) {
            return Result.failure(Exception("No clinic selected. Cannot add kid."))
        }

        val normalizedKidName = kidName.trim().lowercase()
        if (normalizedKidName.isEmpty()) {
            return Result.failure(Exception("Kid name cannot be empty."))
        }

        try {
            val querySnapshot = firestore.collection("kids")
                .whereEqualTo("clinicId", selectedClinicId)
                .whereEqualTo("normalizedName", normalizedKidName)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                return Result.failure(Exception("A kid with the name '$kidName' already exists in '${orgRepo.requireSelectedClinic().name}'."))
            }

            val kidDocumentRef = firestore.collection("kids").document()
            val newKid = Kid(
                id = kidDocumentRef.id,
                ownerUid = currentUser.uid,
                clinicId = selectedClinicId,
                name = kidName.trim(),
                normalizedName = normalizedKidName
            )
            kidDocumentRef.set(newKid).await()
            return Result.success(kidDocumentRef.id)
        } catch (e: Exception) {
            return Result.failure(e) // Catch and return any other exception
        }
    }

    suspend fun updateKid(updatedKid: Kid): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        val trimmedName = updatedKid.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(Exception("Kid name cannot be empty for an update."))
        }
        if (updatedKid.id.isEmpty()) {
            return Result.failure(Exception("Kid ID cannot be empty for an update."))
        }
        if (updatedKid.clinicId.isEmpty()) {
            return Result.failure(Exception("Clinic ID cannot be empty for an update."))
        }

        val normalizedNewName = trimmedName.lowercase()

        try {
            val kidDocumentRef = firestore.collection("kids").document(updatedKid.id)
            val existingKidDoc = kidDocumentRef.get().await()

            if (!existingKidDoc.exists()) {
                return Result.failure(Exception("Kid with ID '${updatedKid.id}' not found."))
            }

            val existingKid = existingKidDoc.toObject(Kid::class.java)
                ?: return Result.failure(Exception("Failed to parse existing kid data."))

            if (existingKid.clinicId != updatedKid.clinicId) {
                return Result.failure(Exception("Changing a kid's clinic via this update method is not supported."))
            }

            if (existingKid.normalizedName != normalizedNewName) {
                val nameCheckQuery = firestore.collection("kids")
                    .whereEqualTo("clinicId", updatedKid.clinicId)
                    .whereEqualTo("normalizedName", normalizedNewName)
                    .limit(1)
                    .get()
                    .await()

                if (!nameCheckQuery.isEmpty) {
                    val conflictingKid = nameCheckQuery.documents.first().toObject(Kid::class.java)
                    if (conflictingKid?.id != updatedKid.id) {
                        return Result.failure(Exception("A kid with the name '$trimmedName' already exists in this clinic."))
                    }
                }
            }

            val finalKidData = updatedKid.copy(
                name = trimmedName,
                normalizedName = normalizedNewName,
                ownerUid = existingKid.ownerUid,
                createdAt = existingKid.createdAt
            )

            kidDocumentRef.set(finalKidData).await()
            return Result.success(Unit)

        } catch (e: Exception) {
            return Result.failure(e) // Catch and return any other exception
        }
    }

    suspend fun getKidById(kidId: String): Result<Kid?> {
        if (kidId.isBlank()) {
            return Result.failure(IllegalArgumentException("Kid ID cannot be empty."))
        }
        return try {
            val documentSnapshot = firestore.collection("kids").document(kidId).get().await()
            if (documentSnapshot.exists()) {
                val kid = documentSnapshot.toObject(Kid::class.java)
                if (kid != null) {
                    Result.success(kid)
                } else {
                    Result.failure(Exception("Kid document exists but could not be converted to Kid object for ID: $kidId"))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllKidsForClinic(clinicId: String): Result<List<Kid>> {
        return try {
            val querySnapshot = firestore.collection("kids")
                .whereEqualTo("clinicId", clinicId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val kids = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Kid::class.java)
            }
            Result.success(kids)
        } catch (e: IllegalStateException) {
            Result.failure(Exception("No clinic selected. ${e.message}")) // Include original message if helpful
        } catch (e: Exception) {
            Result.failure(e) // Catch and return any other exception
        }
    }

    suspend fun fetchAllKidsForClinicAndTherapist(clinicId: String, therapistId: String): Result<List<Kid>> {
        if (clinicId.isBlank()) {
            return Result.failure(Exception("Clinic ID cannot be empty."))
        }
        if (therapistId.isBlank()) {
            return Result.failure(Exception("Therapist ID cannot be empty."))
        }

        return try {
            val querySnapshot = firestore.collection("kids")
                .whereEqualTo("clinicId", clinicId)
                .whereArrayContains("therapistList", therapistId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val kids = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Kid::class.java)
            }
            Result.success(kids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateKidTherapistList(kid: Kid): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        // Validate clinic ID
        if (kid.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Kid ID cannot be empty."))
        }

        // Validate clinic name
        val trimmedKidName = kid.name.trim()
        if (trimmedKidName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Kid name cannot be empty."))
        }

        try {
            val kidRef = firestore.collection("kids").document(kid.id)
            val currentKidDoc = kidRef.get().await()

            if (!currentKidDoc.exists()) {
                return Result.failure(Exception("Kid with ID '${kid.id}' not found."))
            }

            val updates = mapOf(
                "therapistList" to kid.therapistList
            )

            kidRef.update(updates).await()
            return Result.success(Unit)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun addTherapistToKidOrInvite(
        email: String,
        kidId: String,
        coordinatorName: String
    ): Result<String> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        val therapistEmail = email.trim().lowercase()
        if (therapistEmail.isEmpty()) {
            return Result.failure(IllegalArgumentException("Therapist email cannot be empty."))
        }
        if (kidId.isBlank()) {
            return Result.failure(IllegalArgumentException("Kid ID cannot be blank."))
        }

        // It's good practice to get the current clinic from the kid document or ensure consistency
        val kidDocRef = firestore.collection("kids").document(kidId)
        val kidSnapshot = kidDocRef.get().await()
        if (!kidSnapshot.exists()) {
            return Result.failure(Exception("Kid with ID '$kidId' not found."))
        }
        val currentKid = kidSnapshot.toObject(Kid::class.java)
            ?: return Result.failure(Exception("Failed to parse kid data for ID '$kidId'."))

        try {
            val usersCollection = firestore.collection("users")
            val querySnapshot = usersCollection
                .whereEqualTo("email", therapistEmail)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) { // Therapist user already exists in the system
                val therapistUserDoc = querySnapshot.documents.first()
                val therapistUserId = therapistUserDoc.id

                // Check if therapist is already assigned to this specific kid
                if (currentKid.therapistList.contains(therapistUserId)) {
                    return Result.failure(Exception("Therapist with email '$email' is already assigned to kid '${currentKid.name}'."))
                } else {
                    // Add therapist to the kid's therapistList
                    // Use FieldValue.arrayUnion to atomically add the therapistId
                    kidDocRef.update("therapistList", FieldValue.arrayUnion(therapistUserId))
                        .await()
                    // Update the local kid object in orgRepo
                    orgRepo.requireSelectedKid().therapistList.add(therapistUserId)
                    return Result.success("Therapist with ID '$therapistUserId' added to kid '${currentKid.name}'.")
                }
            } else { // Therapist user does not exist in the system -> create invite for this clinic
                // Check if already invited for this clinic
                val inviteQuery = firestore.collection("therapist_invites")
                    .whereEqualTo("email", therapistEmail)
                    .whereEqualTo("kidId", currentKid.id)
                    .limit(1)
                    .get()
                    .await()

                if (inviteQuery.isEmpty) {
                    val therapistInvitesDocumentRef =
                        firestore.collection("therapist_invites").document()
                    val newInvitedTherapist = InvitedTherapist(
                        id = therapistInvitesDocumentRef.id,
                        ownerUid = currentUser.uid,
                        kidId = currentKid.id,
                        email = therapistEmail,
                        coordinatorName = coordinatorName
                    )
                    therapistInvitesDocumentRef.set(newInvitedTherapist).await()
                    return Result.success("Invite sent to '$therapistEmail' for kid ID '${currentKid.id}'. Invite ID: ${therapistInvitesDocumentRef.id}")
                } else {
                    return Result.failure(Exception("Therapist with email '$therapistEmail' is already invited to this kid (ID: '${currentKid.id}')."))
                }
            }
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to add or invite therapist for kid '$kidId'. ${e.localizedMessage}", e))
        }
    }

    suspend fun removeKid(kid: Kid): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        if (kid.id.isEmpty()) {
            return Result.failure(Exception("Kid ID cannot be empty."))
        }

        try {
            val kidDocumentRef = firestore.collection("kids").document(kid.id)
            val existingKidDoc = kidDocumentRef.get().await()

            if (!existingKidDoc.exists()) {
                return Result.failure(Exception("Kid with ID '${kid.id}' not found."))
            }

            // Remove all kidObjectives associated with this kid
            val kidObjectivesQuery = firestore.collection("kid_objectives")
                .whereEqualTo("kidId", kid.id)
                .get()
                .await()

            // Delete all associated kidObjectives
            for (document in kidObjectivesQuery.documents) {
                document.reference.delete().await()
            }

            // Remove the kid itself
            kidDocumentRef.delete().await()
            return Result.success(Unit)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}

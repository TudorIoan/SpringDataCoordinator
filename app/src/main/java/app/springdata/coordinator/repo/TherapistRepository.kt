package app.springdata.coordinator.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import app.springdata.coordinator.model.Clinic
import app.springdata.coordinator.model.InvitedTherapist
import app.springdata.coordinator.model.Kid
import app.springdata.coordinator.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.addAll
import kotlin.collections.mapNotNull

@Singleton
class TherapistRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val orgRepo: OrgRepository,
    private val userRepo: UserRepository,
    private val clinicRepo: ClinicRepository
) {
    suspend fun fetchTherapistsForClinic(clinicId: String): Result<List<UserProfile>> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be blank."))
        }

        try {
            // 1. Fetch all kids belonging to the given clinicId
            val kidsQuerySnapshot = firestore.collection("kids")
                .whereEqualTo("clinicId", clinicId)
                .get()
                .await()

            if (kidsQuerySnapshot.isEmpty) {
                // No kids in this clinic, so no therapists associated via kids
                return Result.success(emptyList())
            }

            val allTherapistIdsFromKids = mutableSetOf<String>()
            kidsQuerySnapshot.documents.forEach { kidDocument ->
                // Assuming your Kid model is correct and therapistList is a list of therapist UIDs
                val kid = kidDocument.toObject(Kid::class.java)
                kid?.therapistList?.let { therapistIds ->
                    allTherapistIdsFromKids.addAll(therapistIds.filter { it.isNotBlank() }) // Add non-blank IDs
                }
            }

            if (allTherapistIdsFromKids.isEmpty()) {
                // Kids exist, but none have therapists assigned in their therapistList
                return Result.success(emptyList())
            }

            // 2. Fetch UserProfile documents for these unique therapist IDs
            // Firestore 'whereIn' queries are limited to 30 elements in the array by default.
            // If you expect more, you'll need to batch the queries.
            val therapistUserProfiles = mutableListOf<UserProfile>()
            val therapistIdChunks = allTherapistIdsFromKids.toList().chunked(30) // Max 30 per 'in' query

            for (chunk in therapistIdChunks) {
                if (chunk.isEmpty()) continue

                val usersQuerySnapshot = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                usersQuerySnapshot.documents.mapNotNullTo(therapistUserProfiles) { document ->
                    document.toObject(UserProfile::class.java)
                }
            }

            return Result.success(therapistUserProfiles)

        } catch (e: Exception) {
            Log.e("TherapistRepo", "Failed to fetch therapists for clinic '$clinicId': ${e.message}", e)
            return Result.failure(Exception("Failed to fetch therapists for clinic '$clinicId': ${e.localizedMessage}", e))
        }
    }

    suspend fun fetchInvitedTherapistsForClinic(clinicId: String): Result<List<InvitedTherapist>> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be blank."))
        }

        try {
            // 1. Fetch all kids belonging to the given clinicId
            val kidsQuerySnapshot = firestore.collection("kids")
                .whereEqualTo("clinicId", clinicId)
                .get()
                .await()

            if (kidsQuerySnapshot.isEmpty) {
                return Result.success(emptyList())
            }

            val kidIdsForClinic = kidsQuerySnapshot.documents.mapNotNull {
                it.id
            }.filter { it.isNotBlank() }


            if (kidIdsForClinic.isEmpty()) {
                return Result.success(emptyList())
            }

            // 2. Fetch InvitedTherapist documents for these kid IDs
            val allInvitedTherapists = mutableListOf<InvitedTherapist>()
            val kidIdChunks = kidIdsForClinic.chunked(30) // Max 30 per 'in' query

            for (chunk in kidIdChunks) {
                if (chunk.isEmpty()) continue

                val invitesQuerySnapshot = firestore.collection("therapist_invites")
                    .whereIn("kidId", chunk)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                invitesQuerySnapshot.documents.mapNotNullTo(allInvitedTherapists) { document ->
                    document.toObject(InvitedTherapist::class.java)
                }
            }

            return Result.success(allInvitedTherapists)

        } catch (e: Exception) {
            Log.e("TherapistRepo", "Failed to fetch invited therapists for clinic '$clinicId': ${e.message}", e)
            return Result.failure(Exception("Failed to fetch invited therapists for clinic '$clinicId': ${e.localizedMessage}", e))
        }
    }


    suspend fun fetchTherapistsForKid(kid: Kid?): Result<List<UserProfile>> {
        // Validate the input Kid object
        if (kid == null) {
            return Result.failure(IllegalArgumentException("Kid object cannot be null."))
        }
        if (kid.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Kid ID within the Kid object cannot be blank."))
        }

        try {
            // 1. Check if the kid has any therapists assigned from the provided Kid object
            val therapistIds = kid.therapistList.filter { it.isNotBlank() }
            if (therapistIds.isEmpty()) {
                // No therapists assigned to this kid according to the passed Kid object
                return Result.success(emptyList())
            }

            // 2. Fetch UserProfile documents for the therapist IDs
            // Firestore 'whereIn' queries are limited (typically to 30 elements per query).
            // If a kid can have more therapists, batching is necessary.
            val therapistUserProfiles = mutableListOf<UserProfile>()
            // Ensure therapistIds is not empty before chunking, though filter { it.isNotBlank() } and isEmpty check above should handle it.
            val therapistIdChunks = therapistIds.chunked(30)

            for (chunk in therapistIdChunks) {
                // This check might be redundant if therapistIds.isEmpty() is handled, but good for safety.
                if (chunk.isEmpty()) continue

                val usersQuerySnapshot = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                usersQuerySnapshot.documents.mapNotNullTo(therapistUserProfiles) { document ->
                    document.toObject(UserProfile::class.java)
                }
            }

            return Result.success(therapistUserProfiles)

        } catch (e: Exception) {
            // Log the error for debugging purposes
            // Log.e("TherapistRepo", "Failed to fetch therapists for kid '${kid.id}': ${e.message}", e)
            return Result.failure(Exception("Failed to fetch therapists for kid '${kid.id}': ${e.localizedMessage}", e))
        }
    }

    suspend fun fetchInvitedTherapistsForKid(kid: Kid?): Result<List<InvitedTherapist>> {
        if (kid == null) {
            return Result.failure(IllegalArgumentException("Kid object cannot be null."))
        }
        if (kid.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Kid ID within the Kid object cannot be blank."))
        }

        try {
            // Fetch invited therapists based directly on the kidId
            val querySnapshot = firestore.collection("therapist_invites")
                .whereEqualTo("kidId", kid.id) // Query directly by the kidId
                .orderBy("createdAt", Query.Direction.DESCENDING) // Optional: order by creation time
                .get()
                .await()

            val invitedTherapists = querySnapshot.documents.mapNotNull { document ->
                document.toObject(InvitedTherapist::class.java)
            }

            return Result.success(invitedTherapists)

        } catch (e: Exception) {
            Log.e("TherapistRepo", "Failed to fetch invited therapists for kid '${kid.id}': ${e.message}", e)
            return Result.failure(Exception("Failed to fetch invited therapists for kid '${kid.id}': ${e.localizedMessage}", e))
        }
    }


    suspend fun isTherapistInvited(email: String): Result<String?> {
        val therapistEmail = email.trim().lowercase()
        if (therapistEmail.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty."))
        }

        try {
            val invitesQuerySnapshot = firestore.collection("therapist_invites")
                .whereEqualTo("email", therapistEmail)
                .limit(1)
                .get()
                .await()

            if (invitesQuerySnapshot.isEmpty) {
                return Result.success(null)
            }

            val inviteDocument = invitesQuerySnapshot.documents.first()
            val invitedTherapist = inviteDocument.toObject(InvitedTherapist::class.java)

            return Result.success(invitedTherapist?.coordinatorName)
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to check invitation status or fetch owner profile: ${e.localizedMessage}", e))
        }
    }

    suspend fun removeTherapistFromKid(therapistEmail: String, kid: Kid): Result<Unit> {
        try {
            // First, remove from therapist_invites (always check for invitations)
            val invitesQuerySnapshot = firestore.collection("therapist_invites")
                .whereEqualTo("email", therapistEmail)
                .whereEqualTo("kidId", kid.id)
                .get()
                .await()

            invitesQuerySnapshot.documents.forEach { document ->
                document.reference.delete().await()
            }

            // Second, remove from kid's therapistList (for actual therapist users)
            val therapistUserProfile = firestore.collection("users")
                .whereEqualTo("email", therapistEmail)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(UserProfile::class.java)

            if (therapistUserProfile != null) {
                val updatedTherapistList = kid.therapistList.toMutableList()
                updatedTherapistList.remove(therapistUserProfile.ownerUid)

                firestore.collection("kids")
                    .document(kid.id)
                    .update("therapistList", updatedTherapistList)
                    .await()
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TherapistRepo", "Failed to remove therapist '$therapistEmail' from kid '${kid.id}': ${e.message}", e)
            return Result.failure(Exception("Failed to remove therapist from kid: ${e.localizedMessage}", e))
        }
    }

    suspend fun removeTherapistFromClinic(therapistEmail: String, clinicId: String): Result<Unit> {
        try {
            // Get all kids for this clinic
            val kidsQuerySnapshot = firestore.collection("kids")
                .whereEqualTo("clinicId", clinicId)
                .get()
                .await()

            val kids = kidsQuerySnapshot.documents.mapNotNull { document ->
                document.toObject(Kid::class.java)
            }

            // Remove therapist from each kid
            for (kid in kids) {
                removeTherapistFromKid(therapistEmail, kid)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TherapistRepo", "Failed to remove therapist '$therapistEmail' from clinic '$clinicId': ${e.message}", e)
            return Result.failure(Exception("Failed to remove therapist from clinic: ${e.localizedMessage}", e))
        }
    }
}

package net.abaresults.progresspath.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import net.abaresults.progresspath.model.Clinic
import net.abaresults.progresspath.model.Kid
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClinicRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val kidRepository: KidRepository
) {

    suspend fun addClinic(clinicName: String): Result<String> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        val normalizedClinicName = clinicName.trim().lowercase()
        if (normalizedClinicName.isEmpty()) {
            return Result.failure(Exception("Clinic name cannot be empty."))
        }

        try {
            val querySnapshot = firestore.collection("clinics")
                .whereEqualTo("normalizedName", normalizedClinicName)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                return Result.failure(Exception("A clinic with the name '$clinicName' already exists."))
            }

            // Add the new clinic with an auto-generated ID
            val clinicDocumentRef = firestore.collection("clinics").document() // Auto-generates ID

            val clinicData = Clinic(
                id = clinicDocumentRef.id,
                ownerUid = currentUser.uid,
                name = clinicName.trim(),
                normalizedName = normalizedClinicName
            )

            clinicDocumentRef.set(clinicData).await()

            return Result.success(clinicDocumentRef.id)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun fetchAllClinicsCreatedByCurrentUser(): Result<List<Clinic>> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val querySnapshot = firestore.collection("clinics")
                .whereEqualTo("ownerUid", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val clinics = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Clinic::class.java)
            }
            Result.success(clinics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllClinicsWithCurrentTherapist(): Result<List<Clinic>> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated for therapist check"))

        return try {
            // First, find all kids that have the current therapist in their therapistList
            val kidsQuerySnapshot = firestore.collection("kids")
                .whereArrayContains("therapistList", currentUser.uid)
                .get()
                .await()

            if (kidsQuerySnapshot.isEmpty) {
                return Result.success(emptyList())
            }

            // Get unique clinic IDs from these kids
            val clinicIds = kidsQuerySnapshot.documents
                .mapNotNull { it.toObject(Kid::class.java)?.clinicId }
                .filter { it.isNotBlank() }
                .distinct()

            if (clinicIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Fetch the clinics by their IDs
            val clinics = mutableListOf<Clinic>()
            val clinicIdChunks = clinicIds.chunked(30) // Firestore 'in' queries are limited to 30 items

            for (chunk in clinicIdChunks) {
                val clinicsQuerySnapshot = firestore.collection("clinics")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                clinicsQuerySnapshot.documents.mapNotNullTo(clinics) { document ->
                    document.toObject(Clinic::class.java)
                }
            }

            // Sort by creation date (newest first)
            val sortedClinics = clinics.sortedByDescending { it.createdAt }
            
            Result.success(sortedClinics)
        } catch (e: Exception) {
            Log.e("ClinicRepo", "Error fetching clinics for therapist ${currentUser.uid}: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getClinicById(clinicId: String): Result<Clinic?> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be empty."))
        }
        return try {
            val documentSnapshot = firestore.collection("clinics").document(clinicId).get().await()
            if (documentSnapshot.exists()) {
                val clinic = documentSnapshot.toObject(Clinic::class.java)
                if (clinic != null) {
                    Result.success(clinic)
                } else {
                    Result.failure(Exception("Clinic document exists but could not be converted to Clinic object for ID: $clinicId"))
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeClinic(clinic: Clinic): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        if (clinic.id.isBlank()) {
            return Result.failure(Exception("Clinic ID cannot be empty."))
        }

        // Verify that the current user owns the clinic
        if (clinic.ownerUid != currentUser.uid) {
            return Result.failure(Exception("You don't have permission to remove this clinic."))
        }

        return try {
            // First, get all kids associated with this clinic
            val kidsQuery = firestore.collection("kids")
                .whereEqualTo("clinicId", clinic.id)
                .get()
                .await()

            // Remove each kid using the KidRepository's removeKid function
            // This will also remove their associated kidObjectives
            for (kidDocument in kidsQuery.documents) {
                val kid = kidDocument.toObject(Kid::class.java)
                if (kid != null) {
                    val removeKidResult = kidRepository.removeKid(kid)
                    if (removeKidResult.isFailure) {
                        return Result.failure(
                            Exception("Failed to remove kid '${kid.name}': ${removeKidResult.exceptionOrNull()?.message}")
                        )
                    }
                }
            }

            // Finally, remove the clinic itself
            firestore.collection("clinics")
                .document(clinic.id)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateClinic(clinic: Clinic): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("User not authenticated"))

        if (clinic.id.isBlank()) {
            return Result.failure(Exception("Clinic ID cannot be empty."))
        }

        if (clinic.name.trim().isEmpty()) {
            return Result.failure(Exception("Clinic name cannot be empty."))
        }

        // Verify that the current user owns the clinic
        if (clinic.ownerUid != currentUser.uid) {
            return Result.failure(Exception("You don't have permission to update this clinic."))
        }

        return try {
            firestore.collection("clinics")
                .document(clinic.id)
                .set(clinic)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
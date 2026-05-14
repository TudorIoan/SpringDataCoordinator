package net.abaresults.progresspath.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import net.abaresults.progresspath.model.InvitedTherapist
import net.abaresults.progresspath.model.UserProfile
import net.abaresults.progresspath.model.UserType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val clinicRepo: ClinicRepository,
    private val kidRepo: KidRepository
) {

    var userDetails: UserProfile? = null
    var therapistUsers: List<UserProfile> = emptyList()

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val tokenResult = firebaseAuth.currentUser?.getIdToken(true)?.await()
            val token = tokenResult?.token

            val fetchResult = fetchUserDetails(firebaseAuth.currentUser!!.uid)
            if (fetchResult.isSuccess) {
                this.userDetails = fetchResult.getOrNull()
                Result.success(Unit)
            } else {
                // Logout the user if fetching details fails, as the login isn't fully complete.
                firebaseAuth.signOut()
                this.userDetails = null
                Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch user details after login."))
            }
        } catch (e: Exception) {
            // Clear any potentially partially set state
            this.userDetails = null
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String, userType: UserType, isInvitedTherapist: Boolean = false): Result<Unit> {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim().lowercase(), password).await()
            val user = authResult.user

            user?.let { firebaseUser ->
                // Update Auth profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                // Now, also save user info to Firestore
                val userProfile = UserProfile(
                    ownerUid = firebaseUser.uid,
                    name = name,
                    email = email.trim().lowercase(),
                    userType = userType.roleName
                )

                firestore.collection("users").document(firebaseUser.uid)
                    .set(userProfile)
                    .await()

                val fetchResult = fetchUserDetails(firebaseUser.uid)
                if (fetchResult.isSuccess) {
                    this.userDetails = fetchResult.getOrNull()
                    Result.success(Unit)
                } else {
                    // Logout the user if fetching details fails, as the login isn't fully complete.
                    firebaseAuth.signOut()
                    this.userDetails = null
                    Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch user details after login."))
                }

                if (isInvitedTherapist) {
                    // Fetch the invited therapist details
                    val invitedTherapistResult = getTherapistInvite(email)
                    if (invitedTherapistResult.isSuccess) {
                        val invitedTherapist = invitedTherapistResult.getOrNull()
                        if (invitedTherapist != null) {
                            // Get the kid by kidId from the invited therapist
                            val kidResult = kidRepo.getKidById(invitedTherapist.kidId)
                            if (kidResult.isSuccess) {
                                val kid = kidResult.getOrNull()
                                if (kid != null) {
                                    // Add the new user's ID to the clinic's therapist list
                                    val updatedTherapistList = kid.therapistList.toMutableList()
                                    if (!updatedTherapistList.contains(firebaseUser.uid)) {
                                        updatedTherapistList.add(firebaseUser.uid)
                                    }
                                    val updatedKid = kid.copy(therapistList = updatedTherapistList)

                                    // Update the kid in the repository
                                    kidRepo.updateKidTherapistList(updatedKid)
                                    deleteTherapistInvite(email)
                                } else {
                                    // Handle case where clinic is not found
                                    // You might want to log this or return a specific error
                                    println("Kid not found for ID: ${invitedTherapist.kidId}")
                                }
                            } else {
                                // Handle error fetching clinic
                                println("Error fetching clinic: ${kidResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            // Handle case where invited therapist is not found (though should be unlikely if isInvitedTherapist is true)
                            println("Invited therapist not found for email: $email")
                        }
                    } else {
                        // Handle error fetching invited therapist
                        println("Error fetching invited therapist: ${invitedTherapistResult.exceptionOrNull()?.message}")
                    }
                }

                return Result.success(Unit)

            } ?: run {
                return Result.failure(Exception("User creation succeeded but user object is null, failed to set display name or save to Firestore."))
            }

        } catch (creationException: Exception) {
            return Result.failure(creationException)
        }
    }

    suspend fun autoLogIn() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val fetchResult = fetchUserDetails(currentUser.uid)
            if (fetchResult.isSuccess) {
                this.userDetails = fetchResult.getOrNull()
                Result.success(Unit)
            } else {
                // Logout the user if fetching details fails, as the login isn't fully complete.
                firebaseAuth.signOut()
                this.userDetails = null
                Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch user details after login."))
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun logout() {
        firebaseAuth.signOut()
        userDetails = null
    }

    suspend fun fetchUserDetails(userId: String): Result<UserProfile> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }
        return try {
            val documentSnapshot = firestore.collection("users").document(userId).get().await()

            if (documentSnapshot.exists()) {
                val profile = documentSnapshot.toObject(UserProfile::class.java)
                if (profile != null) {
                    Result.success(profile)
                } else {
                    Result.failure(Exception("User document exists but could not be converted to UserProfile for ID: $userId"))
                }
            } else {
                Result.failure(Exception("User not found with ID: $userId"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch user details for ID $userId: ${e.localizedMessage}", e))
        }
    }

    fun requireUserDetails(): UserProfile {
        return userDetails ?: throw IllegalStateException("User details not available")
    }

    suspend fun updateUser(updatedProfile: UserProfile): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("No user logged in."))

        // Ensure the updatedProfile's ownerUid matches the current user's UID for security
        if (updatedProfile.ownerUid != firebaseUser.uid) {
            return Result.failure(SecurityException("Attempting to update user profile with mismatched UID."))
        }

        return try {
            // 1. Update Firebase Authentication display name if it has changed
            if (updatedProfile.name != firebaseUser.displayName) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(updatedProfile.name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            // Note: Firebase Authentication email updates are more complex and require re-authentication.
            // If email updates are needed, consider `firebaseUser.updateEmail(newEmail).await()`
            // and handle potential `FirebaseAuthRecentLoginRequiredException`.

            // 2. Update Firestore document
            firestore.collection("users").document(firebaseUser.uid)
                .set(updatedProfile) // .set() will overwrite the document with the new profile
                .await()

            // 3. Update local cache
            this.userDetails = updatedProfile

            Result.success(Unit)
        } catch (e: Exception) {
            // Optionally log the exception e
            Result.failure(e)
        }
    }

    suspend fun getTherapistInvite(email: String): Result<InvitedTherapist?> {
        return try {
            val querySnapshot = firestore.collection("therapist_invites")
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1) // Assuming email is unique for invites, limit to 1
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                // Return the first document found
                Result.success(querySnapshot.documents.first().toObject(InvitedTherapist::class.java))
            } else {
                // No invite found for this email
                Result.success(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteTherapistInvite(email: String): Result<Unit> {
        return try {
            val querySnapshot = firestore.collection("therapist_invites")
                .whereEqualTo("email", email.trim().lowercase())
                .get()
                .await()

            // Optional: For many deletes, consider a batched write for atomicity if needed,
            // though individual deletes are often fine.
            val batch = firestore.batch()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
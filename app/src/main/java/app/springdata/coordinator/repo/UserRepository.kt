package app.springdata.coordinator.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import app.springdata.coordinator.model.UserProfile
import app.springdata.coordinator.model.UserType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

    suspend fun register(name: String, email: String, password: String): Result<Unit> {
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
                    userType = UserType.COORDINATOR.roleName
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

}

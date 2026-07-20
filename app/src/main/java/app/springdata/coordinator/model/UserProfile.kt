package app.springdata.coordinator.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val ownerUid: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = UserType.COORDINATOR.roleName,
    @ServerTimestamp
    val createdAt: Date? = null,
)

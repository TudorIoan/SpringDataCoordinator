package net.abaresults.progresspath.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Clinic(
    val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val ownerUid: String = "",// Coordinator UserProfile uid
    @ServerTimestamp
    val createdAt: Date? = null,
)


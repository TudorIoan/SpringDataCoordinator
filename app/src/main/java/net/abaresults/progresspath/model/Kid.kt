package net.abaresults.progresspath.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Kid(
    val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val ownerUid: String = "", // Coordinator UserProfile uid
    val clinicId: String = "",
    val therapistList: MutableList<String> = mutableListOf(),
    @ServerTimestamp
    val createdAt: Date? = null,
)

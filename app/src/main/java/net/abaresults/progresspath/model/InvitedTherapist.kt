package net.abaresults.progresspath.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class InvitedTherapist(
    val id: String = "",
    val email: String = "",
    val coordinatorName: String = "",
    val ownerUid: String = "",
    val kidId: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
)
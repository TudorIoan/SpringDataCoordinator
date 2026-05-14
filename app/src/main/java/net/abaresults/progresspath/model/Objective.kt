package net.abaresults.progresspath.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Objective(
    val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val ownerUid: String = "", // Coordinator UserProfile uid
    val itemsList: MutableList<ObjItem> = mutableListOf(),
    val type: ObjectiveType = ObjectiveType.MAND_BEGINNER,
    val level: ObjLevel = ObjLevel.BEGINNER,
    @ServerTimestamp
    val createdAt: Date? = null,
)
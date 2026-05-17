package net.abaresults.progresspath.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class KidObjective(
    val id: String = "",
    val kidId: String = "",
    val objectiveId: String = "",
    val active: Boolean = false,
    val consecutiveYesses: Int? = null,
    val addedByUid: String = "", // Coordinator who added this objective to the kid
    val itemsList: List<KidObjectiveItem> = listOf(),
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

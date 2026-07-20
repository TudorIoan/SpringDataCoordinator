package app.springdata.coordinator.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class KidObjectiveItem(
    val objItem: ObjItem = ObjItem(),
    var active: Boolean = false,
    var firstResponseTime: Date? = null,
    var lastResponseTime: Date? = null,
    var lastModificationByUserId: String? = null,
    var mastered: Boolean = false,
    // YES_NO item
    var yesNoList: MutableList<YesNoItem> = mutableListOf(),
    // FREQUENCY item
    var frequencyList: MutableList<FrequencyItem> = mutableListOf(),
    // CHECKMARK item
    var checkmarkList: MutableList<Date> = mutableListOf(),
    // PERCENTAGE item
    var percentageList: MutableList<ProgressItem> = mutableListOf()
) {
    @get:Exclude
    var lastModificationByUserName: String? = null
}

data class FrequencyItem(
    val frequency: Int = 0,
    val date: Date? = null
)

data class ProgressItem(
    val progress: Int = 0, // Value from 0 to 100
    val date: Date? = null
)

data class YesNoItem(
    val yes: Boolean = false,
    val date: Date? = null
)
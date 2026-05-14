package net.abaresults.progresspath.model

data class ObjItem(
    val name: String = "",
    val normalizedName: String = "",
    val type: ObjItemType = ObjItemType.YES_NO,
    val consecutiveYesses: Int? = null // null = coordinator decides, 1-20 = consecutive Yes count for mastery
)

enum class ObjItemType(val displayName: String) {
    YES_NO("Yes/No"),
    FREQUENCY("Frequency"),
    CHECKMARK("Checkmark"),
    PERCENTAGE("Percentage");

    companion object {
        fun fromDisplayName(displayName: String): ObjItemType {
            return values().find { it.displayName == displayName } ?: YES_NO
        }

        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}
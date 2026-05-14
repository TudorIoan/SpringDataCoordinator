package net.abaresults.progresspath.model

enum class UserType(val roleName: String) {
    THERAPIST("therapist"),
    COORDINATOR("coordinator");

    companion object {
        fun fromString(userType: String): UserType {
            return UserType.entries.find { it.roleName == userType }!!
        }

        fun toString(userType: UserType): String {
            return userType.roleName
        }

    }
}


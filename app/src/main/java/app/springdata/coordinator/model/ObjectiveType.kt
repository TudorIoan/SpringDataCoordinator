package app.springdata.coordinator.model

enum class ObjectiveType(val displayName: String, val level: ObjLevel) {
    // BEGINNER Level
    MAND_BEGINNER("Mand", ObjLevel.BEGINNER),
    TACT_BEGINNER("Tact", ObjLevel.BEGINNER),
    SOCIAL_BEGINNER("Social", ObjLevel.BEGINNER),
    ECHOIC_BEGINNER("Echoic", ObjLevel.BEGINNER),
    LISTENER_BEGINNER("Listener", ObjLevel.BEGINNER),
    INTRAVERBAL("Intraverbal", ObjLevel.BEGINNER),
    PLAY_BEGINNER("Play", ObjLevel.BEGINNER),
    MOTOR_IMITATION("Motor Imitation", ObjLevel.BEGINNER),
    COLORING("Academic: Coloring & Drawing", ObjLevel.BEGINNER),
    COUNTING("Academic: Counting", ObjLevel.BEGINNER),
    VIZUO_SPATIAL("Vizuo-Spatial", ObjLevel.BEGINNER),
    EARLY_LEARNING("Early Learning", ObjLevel.BEGINNER),

    // INTERMEDIATE Level
    GENERALISED_SKILLS("Generalised Skills", ObjLevel.INTERMEDIATE),
    VISUAL_SKILLS("Visual Skills", ObjLevel.INTERMEDIATE),
    PLAY_INTERMEDIATE("Play", ObjLevel.INTERMEDIATE),
    SOCIAL_INTERMEDIATE("Social", ObjLevel.INTERMEDIATE),
    DRAWING_LETTERS("Academic: Drawing & Copying Letters", ObjLevel.INTERMEDIATE),
    MAND_INTERMEDIATE("Mand", ObjLevel.INTERMEDIATE),
    LISTENER_INTERMEDIATE("Listener", ObjLevel.INTERMEDIATE),
    ECHOIC_INTERMEDIATE("Echoic", ObjLevel.INTERMEDIATE),
    TACT_INTERMEDIATE("Tact", ObjLevel.INTERMEDIATE),
    INTRAVERBAL_CONTROL("Intraverbal Control", ObjLevel.INTERMEDIATE),
    AUTOCLITIC("Autoclitic", ObjLevel.INTERMEDIATE),
    READING_DICTATION("Academic: Reading & Dictation", ObjLevel.INTERMEDIATE),
    NUMBER_QUANTITY("Academic: Number & Quantity", ObjLevel.INTERMEDIATE),

    // ADVANCED Level
    MULTIPLE_VERBAL_CONTROL("Multiple Verbal Control & Problem Solving", ObjLevel.ADVANCED),
    TEMPORAL_RELATIONS("Temporal Relations (Vocabulary)", ObjLevel.ADVANCED),
    PAST_EVENT_RECALL("Past Event Recall: Visual, Experience, Oral", ObjLevel.ADVANCED),
    SOCIAL_CONVERSATION("Social & Conversation Skills", ObjLevel.ADVANCED),
    PRIVATE_EVENTS("Private Events (Emotions)", ObjLevel.ADVANCED),
    ABSTRACT_REASONING("Abstract Reasoning", ObjLevel.ADVANCED);

    companion object {
        fun getAllDisplayNames(): List<String> {
            return ObjectiveType.entries.map { it.displayName }
        }

        fun getDisplayNamesByLevel(level: ObjLevel): List<String> {
            return ObjectiveType.entries
                .filter { it.level == level }
                .map { it.displayName }
        }

        fun getObjectiveTypesByLevel(level: ObjLevel): List<ObjectiveType> {
            return ObjectiveType.entries.filter { it.level == level }
        }

        fun fromDisplayName(displayName: String): ObjectiveType {
            return ObjectiveType.entries.find { it.displayName == displayName }!!
        }
    }
}


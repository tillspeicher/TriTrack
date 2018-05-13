package de.tritrack.recording.recording

enum class OpType(val prefix: String) {
    ID (""),
    AVG ("Avg "),
    MAX ("Max "),
    NORM_AVG ("Avg norm. "),
    OFFSET ("") // used to make values like time or distance start at 0 when they are subscribed later
}
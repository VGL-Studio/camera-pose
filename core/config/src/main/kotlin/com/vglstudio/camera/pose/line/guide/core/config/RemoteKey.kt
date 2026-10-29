package com.vglstudio.camera.pose.line.guide.core.config

/**
 * A Remote Config key with its type, its value before any remote value arrives, and a description for the
 * team that sets it on Firebase (ADR-0023). Every key of the app is declared once as a [RemoteKey] by the
 * module that owns it; docs/remote-config-keys.md is generated from these declarations.
 *
 * Not a data class: its `copy` would bypass the checks of the factories.
 */
class RemoteKey<T : Any> private constructor(
    val name: String,
    val type: Type,
    val default: T,
    val description: String,
) {
    enum class Type { BOOLEAN, LONG, DOUBLE, STRING }

    init {
        require(NAME_PATTERN.matches(name)) { "Remote Config key '$name' must be lower snake case (ADR-0023)" }
        require(description.isNotBlank()) { "Remote Config key '$name' needs a description" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description of '$name' is longer than $MAX_DESCRIPTION_LENGTH characters, the Firebase limit"
        }
    }

    override fun equals(other: Any?): Boolean = other is RemoteKey<*> &&
        name == other.name &&
        type == other.type &&
        default == other.default &&
        description == other.description

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + default.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }

    override fun toString(): String = name

    companion object {
        private val NAME_PATTERN = Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$")

        /** Firebase rejects parameter descriptions longer than this. */
        private const val MAX_DESCRIPTION_LENGTH = 256

        fun boolean(name: String, default: Boolean, description: String): RemoteKey<Boolean> =
            RemoteKey(name, Type.BOOLEAN, default, description)

        fun long(name: String, default: Long, description: String): RemoteKey<Long> =
            RemoteKey(name, Type.LONG, default, description)

        fun double(name: String, default: Double, description: String): RemoteKey<Double> =
            RemoteKey(name, Type.DOUBLE, default, description)

        fun string(name: String, default: String, description: String): RemoteKey<String> =
            RemoteKey(name, Type.STRING, default, description)
    }
}

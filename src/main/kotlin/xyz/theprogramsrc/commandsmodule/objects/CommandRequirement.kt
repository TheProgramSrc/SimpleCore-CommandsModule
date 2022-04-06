package xyz.theprogramsrc.commandsmodule.objects

/**
 * Representation of a requirement for a command
 * @param message The message to send to the user if the requirement is not met
 * @param check The function to check if the requirement is met
 */
class CommandRequirement<T>(val message: String, val check: (T) -> Boolean = { true })
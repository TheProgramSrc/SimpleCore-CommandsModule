package xyz.theprogramsrc.commandsmodule.spigot

import xyz.theprogramsrc.commandsmodule.spigot.arguments.ArgumentProcessor

/**
 * Representation of the Indexed Arguments
 * @param args The arguments
 *
 * How to retrieve a value:
 * ```kt
 * val name = args.get<Type>(index) // The type is the object type that you expect to receive, the index is the index of the argument.
 * ```
 */
class IndexedArguments(val args: List<String> = emptyList()){

    /**
     * Gets the value of an argument
     * @param index The index of the argument
     * @return The value of the argument
     */
    inline fun <reified T> get(index: Int): T? = if(isValid<T>(index)){
        ArgumentProcessor.get(T::class.java).get(args[index]) as T
    } else {
        null
    }

    /**
     * Checks if the value of an argument is valid
     * @param index The index of the argument
     * @return True if the value is valid, false otherwise
     */
    inline fun <reified T> isValid(index: Int): Boolean = ArgumentProcessor.get(T::class.java).validate(args[index])

    /**
     * Checks if the list is not empty
     * @return True if the list is not empty, false otherwise
     */
    fun isNotEmpty(): Boolean = args.isNotEmpty()

    /**
     * Checks if the list is empty
     * @return True if the list is empty, false otherwise
     */
    fun isEmpty(): Boolean = args.isEmpty()
}

/**
 * Representation of the Arguments
 * @param args The arguments provided by the command
 *
 * How to retrieve a value:
 * ```kt
 * val name = args.get<Type>("name") // The type is the object type that you expect to receive.
 * ```
 */
class NamedArguments(val args: Map<String, String> = emptyMap()){

    /**
     * Gets the value of an argument
     * @param name The name of the argument
     * @return The value of the argument
     */
    inline fun <reified T> get(name: String): T? {
        val raw = args[name] ?: return null
        val processor = ArgumentProcessor.get(T::class.java)
        if(!processor.validate(raw)) return null
        return processor.get(raw) as T
    }

    /**
     * Checks if the value of an argument is valid
     * @param name The name of the argument
     * @return True if the value is valid, false otherwise
     */
    inline fun <reified T> isValid(name: String): Boolean {
        val raw = args[name] ?: return false
        return ArgumentProcessor.get(T::class.java).validate(raw)
    }

    /**
     * Checks if the argument exists
     * @param name The name of the argument
     * @return True if the argument exists, false otherwise
     */
    infix fun exists(name: String): Boolean = args.containsKey(name)

    /**
     * Checks if the list is not empty
     * @return True if the list is not empty, false otherwise
     */
    fun isNotEmpty(): Boolean = args.isNotEmpty()

    /**
     * Checks if the list is empty
     * @return True if the list is empty, false otherwise
     */
    fun isEmpty(): Boolean = args.isEmpty()
}

package xyz.theprogramsrc.commandsmodule.objects.arguments

import java.time.LocalDateTime
import java.util.*

/**
 * Representation of an Argument Processor
 */
interface ArgumentProcessor<T> {

    companion object {
        private val processors = mutableMapOf<Class<*>, ArgumentProcessor<*>>(
            String::class.java to StringArgumentProcessor(),
            Char::class.java to CharArgumentProcessor(),
            Int::class.java to IntArgumentProcessor(),
            Double::class.java to DoubleArgumentProcessor(),
            Float::class.java to FloatArgumentProcessor(),
            Long::class.java to LongArgumentProcessor(),
            ShortArgumentProcessor::class.java to ShortArgumentProcessor(),
            ByteArgumentProcessor::class.java to ByteArgumentProcessor(),
            Boolean::class.java to BooleanArgumentProcessor(),
            UUID::class.java to UUIDArgumentProcessor(),
            LocalDateTime::class.java to DateTimeArgumentProcessor(),
        )

        /**
         * Registers a new Argument Processor, so it can be used in the command argument parsing
         * @param type The type of the argument. Example: String class, Int class, etc.
         * @param processor The class of the argument processor
         * @return The ArgumentProcessor object
         */
        fun register(type: Class<*>, processor: ArgumentProcessor<*>) = apply {
            processors[type] = processor
        }

        /**
         * Registers the given map of classes and processors to the processors map
         * @param map The map of classes and processors
         * @return The ArgumentProcessor object
         */
        fun register(map: Map<Class<*>, ArgumentProcessor<*>>) = apply {
            processors.putAll(map)
        }

        /**
         * Gets the Argument Processor for the given type
         * @param type The type of the argument. Example: String class, Int class, etc.
         * @return The Argument Processor for the given type
         */
        fun get(type: Class<*>): ArgumentProcessor<*> = processors[type] ?: throw IllegalArgumentException("Cannot find a processor for type ${type.name}. Please contact our Team at: https://theprogramsrc.xyz")
    }

    /**
     * Validates the given raw argument and returns if it can be parsed using this argument processor
     * @param raw The raw argument
     * @return If the raw argument can be parsed using this argument processor
     */
    fun validate(raw: String): Boolean

    /**
     * Parses the given raw argument and returns the parsed value
     * @param raw The raw argument
     * @return The parsed value
     */
    fun get(raw: String): T
}

/**
 * Representation of the Argument Manager. This class is used to get a given argument in a command executor
 * @param indexedArguments The list of arguments
 *
 * How to retrieve a value:
 * ```kt
 * val name = args.get<Type>(index) // The type is the object type that you expect to receive, the index is the index of the argument.
 * val lastname = args.get<Type>(name) // The type is the object type that you expect to receive, the name is the name of the argument.
 * ```
 */
class Arguments(val indexedArguments: List<String> = emptyList(), val namedArguments: Map<String, String> = emptyMap()){

    /**
     * Gets the value of an argument by index
     * @param index The index of the argument
     * @return The value of the argument
     */
    inline fun <reified T> get(index: Int): T? = if(isValid<T>(index)){
        ArgumentProcessor.get(T::class.java).get(indexedArguments[index]) as T
    } else {
        null
    }

    /**
     * Gets the value of an argument by name
     * @param name The name of the argument
     * @return The value of the argument
     */
    inline fun <reified T> get(name: String): T? {
        val raw = namedArguments[name] ?: return null
        val processor = ArgumentProcessor.get(T::class.java)
        if(!processor.validate(raw)) return null
        return processor.get(raw) as T
    }

    /**
     * Checks if the value of an argument is valid by index
     * @param index The index of the argument
     * @return True if the value is valid, false otherwise
     */
    inline fun <reified T> isValid(index: Int): Boolean = ArgumentProcessor.get(T::class.java).validate(indexedArguments[index])

    /**
     * Checks if the value of an argument is valid by name
     * @param name The name of the argument
     * @return True if the value is valid, false otherwise
     */
    inline fun <reified T> isValid(name: String): Boolean {
        val raw = namedArguments[name] ?: return false
        return ArgumentProcessor.get(T::class.java).validate(raw)
    }

    /**
     * Checks if there is an argument under the given name
     * @param name The name of the argument
     * @return True if there is an argument under the given name, false otherwise
     */
    fun has(name: String): Boolean = namedArguments.containsKey(name)

    /**
     * Checks if there is an argument under the given index
     * @param index The index of the argument
     * @return True if there is an argument under the given index, false otherwise
     */
    fun has(index: Int): Boolean = indexedArguments.size > index

    /**
     * Checks if there is at least one argument
     * @return True if the list is not empty, false otherwise
     */
    fun isNotEmpty(): Boolean = indexedArguments.isNotEmpty()

    /**
     * Checks if there are no arguments
     * @return True if the list is empty, false otherwise
     */
    fun isEmpty(): Boolean = indexedArguments.isEmpty()
}
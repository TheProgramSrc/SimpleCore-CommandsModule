package xyz.theprogramsrc.commandsmodule.velocity

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import xyz.theprogramsrc.commandsmodule.Main
import xyz.theprogramsrc.commandsmodule.objects.CommandRequirement
import xyz.theprogramsrc.commandsmodule.objects.CommandType
import xyz.theprogramsrc.commandsmodule.objects.arguments.Arguments
import xyz.theprogramsrc.tasksmodule.velocity.VelocityTasks

/**
 * Representation of a velocity command
 * @param name The name of the command
 * @param onExecute The function to execute when the command is executed
 */
class Command(val name: String, val onExecute: (CommandExecutor) -> Unit = {}) {

    var description: String? = null
        private set
    var usage: String? = null
        private set
    var permission: String? = null
        private set
    var commandType: CommandType = CommandType.PLAYER_AND_CONSOLE
        private set
    var tabCompleter: (CommandSource, Array<String>) -> Collection<String>
       private set
    val aliases: MutableList<String> = mutableListOf()
    val subCommands: MutableList<SubCommand> = mutableListOf()
    val requirements: MutableList<CommandRequirement<CommandSource>> = mutableListOf()

    companion object {
        private var registered = false
    }

    init {
        tabCompleter = { source, args ->
            if(args.isEmpty() || (commandType == CommandType.PLAYER && source !is Player) || (commandType == CommandType.CONSOLE && source !is ConsoleCommandSource)) {
                emptyList<String>()
            }

            val subCommands = subCommands.filter { it.name.lowercase().startsWith(args[0]) }
            if(args.isEmpty()) {
                subCommands.map { it.name }
            }

            subCommands.flatMap {
                val argument = it.arguments.getOrNull(args.size - 2)
                val completions = it.tabCompletions(source)
                if (argument != null && completions.keys.any { key -> key.lowercase() == argument.lowercase() })
                    completions.filter { entry -> entry.key.lowercase() == argument.lowercase() }.values.flatten()
                if (argument != null)
                    listOf("<$argument>")

                emptyList<String>()
            }.filter {
                it.lowercase().startsWith(args.last().lowercase())
            }

            emptyList()
        }
        register()
    }

    /**
     * Sets the description of the command
     * @param description The description of the command
     * @return this command
     */
    fun setDescription(description: String): Command = this.apply {
        this.description = description
    }

    /**
     * Sets the usage of the command
     * @param usage The usage of the command
     * @return this command
     */
    fun setUsage(usage: String): Command = this.apply {
        this.usage = usage
    }

    /**
     * Sets the permission of the command
     * @param permission The permission of the command
     * @return this command
     */
    fun withPermission(permission: String): Command = this.apply {
        this.permission = permission
    }

    /**
     * Adds the given aliases to the command
     * @param aliases the aliases to add
     * @return this command
     */
    fun withAlias(vararg aliases: String): Command = this.apply {
        this.aliases.addAll(aliases)
    }

    /**
     * Makes this command to be executable only by players
     * @return this command
     */
    fun playersOnly(): Command = this.apply {
        this.commandType = CommandType.PLAYER
    }

    /**
     * Makes this command to be executable only through console
     * @return this command
     */
    fun consoleOnly(): Command = this.apply {
        this.commandType = CommandType.CONSOLE
    }

    /**
     * Adds a new requirement to the command
     * @param message THe message to send to the command sender if the requirement is not met
     * @param check: The function to check if the requirement is met
     */
    fun require(message: String, check: (CommandSource) -> Boolean): Command = this.apply {
        requirements.add(CommandRequirement(message, check))
    }

    /**
     * Registers an argument to this command
     * @param signature THe name and signature of the command
     * @executor The function to execute when the argument is executed
     * @return this command
     */
    fun subCommand(signature: String, tabCompletions: (CommandSource) -> Map<String, Collection<String>> = { emptyMap() }, excecutor: (CommandExecutor) -> Unit): Command = this.apply {
        val name = signature.split(" ")[0]
        val arguments = signature.split(" ").drop(1).map {
            it.dropWhile { char -> char == '{' }.dropLastWhile { char -> char == '}' }
        }
        subCommands.add(SubCommand(name, arguments, tabCompletions, excecutor))
    }

    /**
     * Registers this command in the server
     * @return true if the command was registered, false otherwise
     */
    private fun register(): Boolean {
        check(!registered) { "Command $name is already registered" }
        return try {
            registered = true
            val command = object : SimpleCommand {

                override fun execute(invocation: SimpleCommand.Invocation) {
                    val source = invocation.source()
                    if(commandType == CommandType.PLAYER && source !is Player) {
                        source.sendMessage(Component.text(Main.PLAYER_COMMAND.translate()))
                        return
                    } else if (commandType == CommandType.CONSOLE && source !is ConsoleCommandSource) {
                        source.sendMessage(Component.text(Main.CONSOLE_COMMAND.translate()))
                        return
                    }

                    val cmd = this@Command
                    val failedRequirements = cmd.requirements.filter { !it.check(source) }
                    // First we check the requirements
                    if(failedRequirements.isNotEmpty()) {
                        VelocityTasks.instance.runAsync {
                            source.sendMessage(Component.text(
                                Main.MISSING_REQUIREMENTS
                                    .translate()
                                    .plus("\n")
                                    .plus(failedRequirements.joinToString("\n"){ "&7- &c${it.message}" })
                                    .replace("&", "§")
                            ))
                        }

                        return
                    }

                    if(source is Player && cmd.permission != null && cmd.permission != "none" && !source.hasPermission(cmd.permission!!)) {
                        source.sendMessage(Component.text(Main.NO_PERMISSION.translate()))
                        return
                    }

                    val args = invocation.arguments()
                    if(args.isNotEmpty() && cmd.subCommands.isNotEmpty()) { // Now that we have sub commands, let's find one!
                        val subCommandName = args.first() // Here we got the subcommand name
                        val rawArguments = args.drop(1) // Arguments provided by velocity
                        val subCommand = cmd.subCommands.find { it.name.lowercase() == subCommandName.lowercase() && it.arguments.size == rawArguments.size } // Let's look for a sub command. And because we can specify multiple sub commands with the same name, we need to check the amount of arguments too
                        if(subCommand == null) {
                            cmd.onExecute(CommandExecutor(source, Arguments(indexedArguments = args.toList())))
                            return
                        }

                        val inputArguments = mutableMapOf<String, String>() // The arguments that the API will provide to the executor
                        if(subCommand.arguments.isNotEmpty()){ // Only generate named arguments if there is at least one
                            for(index in 0 until subCommand.arguments.size) { // Here we fill the map with the arguments
                                val name = subCommand.arguments[index]
                                if(rawArguments.getOrNull(index) == null){ // Should not be null because all the arguments are required
                                    VelocityTasks.instance.runAsync { // If we need to send a message, let's do it async!
                                        source.sendMessage(Component.text(
                                            Main.MISSING_ARGUMENT.translate(placeholders = mapOf(
                                                "argument" to name // Apply the placeholder to let know the user which argument is missing
                                            ))
                                        ))
                                    }
                                    return
                                }
                                inputArguments[name] = rawArguments[index] // Here we add the argument to the map
                            }
                        }

                        val arguments = Arguments(indexedArguments = args.toList(), namedArguments = inputArguments) // The sub commands always uses indexed and named arguments
                        subCommand.executor(CommandExecutor(source, arguments)) // Now we execute it!
                    } else {
                        cmd.onExecute(CommandExecutor(source, Arguments(indexedArguments = args.toList())))
                    }
                }
            }



            true
        }catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

/**
 * Representation of a SubCommand
 * @param name The name of the argument
 * @param arguments The arguments of the subCommand
 * @param tabCompletions The tab completions of the arguments mapped by name and their respective completions
 * @param executor The function to execute when the argument is executed
 *
 * Signature format:
 * {argument1} {argument2}...
 *
 * Examples:
 * - ban {player} -> The name is ban, the argument is player.
 * - ban {uuid} -> The name is ban, the argument is uuid.
 */
data class SubCommand(val name: String, val arguments: List<String> = emptyList(), val tabCompletions: (CommandSource) -> Map<String, Collection<String>>, val executor: (CommandExecutor) -> Unit = {})
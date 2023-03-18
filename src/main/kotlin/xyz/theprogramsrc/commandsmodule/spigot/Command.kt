package xyz.theprogramsrc.commandsmodule.spigot

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.entity.Player
import xyz.theprogramsrc.commandsmodule.Main
import xyz.theprogramsrc.commandsmodule.objects.CommandRequirement
import xyz.theprogramsrc.commandsmodule.objects.CommandType
import xyz.theprogramsrc.commandsmodule.objects.arguments.Arguments
import xyz.theprogramsrc.tasksmodule.spigot.SpigotTasks

/**
 * Representation of a spigot command
 * @param name The name of the command
 * @param onExecute The function to execute when the command is executed.
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
    var tabCompleter: (CommandSender, Array<String>) -> Collection<String>
        private set
    val aliases: MutableList<String> = mutableListOf()
    val subCommands: MutableList<SubCommand> = mutableListOf()
    val requirements: MutableList<CommandRequirement<CommandSender>> = mutableListOf()
    
    companion object {
        private var registered = false
    }

    init {
        tabCompleter = { sender, args ->
            if(commandType == CommandType.PLAYER && sender !is Player) {
                emptyList()
            } else if(commandType == CommandType.CONSOLE && sender !is ConsoleCommandSender) {
                emptyList()
            } else {
                if(args.isNotEmpty()){ // If we have at least 1 argument
                    val subCommands = subCommands.filter { it.name.lowercase().startsWith(args[0].lowercase()) } // Get the sub commands to show
                    if(args.size > 1){ // If there are more than 1 arguments
                        subCommands.flatMap {
                            val argument = it.arguments.getOrNull(args.size-2) // Get the argument. We remove 2 because the first argument is the sub command and the second is the argument
                            val completions = it.tabCompletions(sender)
                            if(argument != null && completions.isNotEmpty() && completions.keys.any { key -> key.lowercase() == argument.lowercase() }){ // Get the available completions for the current argument if any
                                completions.filter { entry -> entry.key.lowercase() == argument.lowercase() }.values.flatten() // Get the completions
                            } else if(argument != null) {
                                listOf("<$argument>") // If there are no completions show the argument with <>
                            } else {
                                emptyList() // If there are no completions nor arguments show nothing
                            }
                        }.filter {
                            it.lowercase().startsWith(args.last().lowercase())
                        } // Show the arguments of the sub command that matches the argument depth
                    } else { // If there is only 1 argument
                        subCommands.map { it.name } // Show the sub commands
                    }
                } else {
                    emptyList()
                }
            }
        }

        register()
    }

    /**
     * Sets the command description
     * @param description The description of the command
     * @return this command
     */
    fun setDescription(description: String): Command = this.apply {
        this.description = description
    }

    /**
     * Sets the command usage
     * @param usage The usage of the command
     * @return this command
     */
    fun setUsage(usage: String): Command = this.apply {
        this.usage = usage
    }

    /**
     * Sets the command permission to the given permission
     * @param permission The permission to set
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
     * @param message The message to send to the command sender if the requirement is not met
     * @param check The function to check if the requirement is met
     */
    fun require(message: String, check: (CommandSender) -> Boolean): Command = this.apply {
        requirements.add(CommandRequirement(message, check))
    }

    /**
     * Replaces the default tab completer for this command
     * @param tabCompleter The tab completer to set
     * @return this command
     */
    fun withTabCompleter(tabCompleter: (CommandSender, Array<String>) -> Collection<String>): Command = this.apply {
        this.tabCompleter = tabCompleter
    }

    /**
     * Registers an argument to this command
     * @param signature The name and signature of the command
     * @param executor The function to execute when the argument is executed
     * @return this command
     */
    fun subCommand(signature: String, tabCompletions: (CommandSender) -> Map<String, Collection<String>> = { emptyMap() }, executor: (CommandExecutor) -> Unit): Command = this.apply {
        val name = signature.split(" ")[0]
        val arguments = signature.split(" ").drop(1).map {
            it.dropWhile { char -> char == '{' }.dropLastWhile { char -> char == '}' }
        }
        subCommands.add(SubCommand(name, arguments, tabCompletions, executor))
    }

    /**
     * Registers this command in the server
     * @return true if the command was registered, false otherwise
     */
    private fun register(): Boolean {
        check(!registered) { "Command $name is already registered" }
        return try {
            val command = object : BukkitCommand(this.name, this.description ?: "", this.usage ?: "/$name", aliases) {
                override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
                    if(commandType == CommandType.PLAYER && sender !is Player) {
                        sender.sendMessage(Main.PLAYER_COMMAND.translate())
                        return false
                    } else if(commandType == CommandType.CONSOLE && sender !is ConsoleCommandSender) {
                        sender.sendMessage(Main.CONSOLE_COMMAND.translate())
                        return false
                    }

                    val cmd = this@Command
                    val failedRequirements = cmd.requirements.filter { !it.check(sender) }
                    // First we check the requirements
                    if(failedRequirements.isNotEmpty()){
                        SpigotTasks.instance.runTaskAsynchronously { // Send async to prevent lag
                            sender.sendMessage(
                                Main.MISSING_REQUIREMENTS
                                    .translate()
                                    .plus("\n")
                                    .plus(failedRequirements.joinToString("\n") { "&7- &c${it.message}" })
                                    .replace("&", "§")
                            )
                        }
                        return false
                    }

                    if(sender is Player && cmd.permission != null && cmd.permission != "none" && !sender.hasPermission(cmd.permission!!)) {
                        sender.sendMessage(Main.NO_PERMISSION.translate())
                        return false
                    }

                    if(args.isNotEmpty() && cmd.subCommands.isNotEmpty()){  // Now that we have sub commands, let's find one!
                        val subCommandName = args.first() // Here we get the subcommand name
                        val rawArguments = args.drop(1) // Arguments provided by bukkit
                        val subCommand = cmd.subCommands.find { it.name.lowercase() == subCommandName.lowercase() && it.arguments.size == rawArguments.size } // Let's look for a sub command. And because we can specify multiple sub commands with the same name, we need to check the amount of arguments too
                        if(subCommand == null) { // No sub commands? No worries, let's run the main action
                            cmd.onExecute(CommandExecutor(sender, Arguments(indexedArguments = args.toList())))
                            return false
                        }

                        val inputArguments = mutableMapOf<String, String>() // The arguments that the API will provide to the executor
                        if(subCommand.arguments.isNotEmpty()){ // Only generate named arguments if there is at least one
                            for(index in 0 until subCommand.arguments.size) { // Here we fill the map with the arguments
                                val name = subCommand.arguments[index]
                                if(rawArguments.getOrNull(index) == null){ // Should not be null because all the arguments are required
                                    SpigotTasks.instance.runTaskAsynchronously { // If we need to send a message, let's do it async!
                                        sender.sendMessage(Main.MISSING_ARGUMENT.translate(placeholders = mapOf(
                                            "argument" to name // Apply the placeholder to let know the user which argument is missing
                                        )))
                                    }
                                    return false
                                }
                                inputArguments[name] = rawArguments[index] // Here we add the argument to the map
                            }
                        }
                        val arguments = Arguments(indexedArguments = args.toList(), namedArguments = inputArguments) // The sub commands always uses indexed and named arguments
                        subCommand.executor(CommandExecutor(sender, arguments)) // Now we execute it!
                    } else {
                        cmd.onExecute(CommandExecutor(sender, Arguments(indexedArguments = args.toList())))
                    }
                    return true
                }

                override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): MutableList<String> =
                    this@Command.tabCompleter(sender, args).toMutableList()
            }
            val commandMapField = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            val commandMap = commandMapField.get(Bukkit.getServer()) as CommandMap
            commandMap.register("command", command)
            registered = true
            true
        } catch (e: Exception) {
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
data class SubCommand(val name: String, val arguments: List<String> = emptyList(), val tabCompletions: (CommandSender) -> Map<String, Collection<String>>, val executor: (CommandExecutor) -> Unit = {})
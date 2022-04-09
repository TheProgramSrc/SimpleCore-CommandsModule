package xyz.theprogramsrc.commandsmodule.bungee

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.TabExecutor
import xyz.theprogramsrc.commandsmodule.Main
import xyz.theprogramsrc.commandsmodule.objects.CommandRequirement
import xyz.theprogramsrc.commandsmodule.objects.CommandType
import xyz.theprogramsrc.commandsmodule.objects.arguments.Arguments
import xyz.theprogramsrc.simplecoreapi.bungee.BungeeLoader
import xyz.theprogramsrc.tasksmodule.bungee.BungeeTasks

/**
 * Representation of a command
 * @param name The name of the command
 * @param onExecute The function to execute when the command is executed.
 */
class Command(val name: String, val onExecute: (CommandExecutor) -> Unit = {}) {

    var permission: String? = null
        private set
    var commandType: CommandType = CommandType.PLAYER_AND_CONSOLE
        private set
    var tabCompleter: (CommandSender, Array<String>) -> Collection<String>
        private set
    val aliases: MutableList<String> = mutableListOf()
    val subCommands: MutableList<SubCommand> = mutableListOf()
    val requirements: MutableList<CommandRequirement<CommandSender>> = mutableListOf()
    private var registered = false

    init {
        tabCompleter = { sender, args ->
            if(commandType == CommandType.PLAYER && sender !is ProxiedPlayer) {
                emptyList()
            } else if(commandType == CommandType.CONSOLE && sender is ProxiedPlayer) {
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
                            it.lowercase().startsWith(args[args.size - 1].lowercase())
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
        subCommands.add(SubCommand(
            name = signature.split(" ")[0],
            arguments = signature.split(" ").drop(1).map {
                it.dropWhile { char -> char == '{' }.dropLastWhile { char -> char == '}' }
            },
            tabCompletions = tabCompletions,
            executor = executor
        ))
    }

    /**
     * Registers this command in the server
     * @return true if the command was registered, false otherwise
     */
    private fun register(): Boolean {
        check(!registered) { "Command $name is already registered" }
        try {
            val command = BungeeCommandHandler(name = this.name, permission = this.permission ?: "", aliases = this.aliases.toTypedArray(), tabCompleter = this.tabCompleter) { sender, args ->
                if(commandType == CommandType.PLAYER && sender !is ProxiedPlayer) {
                    sender.sendMessage(TextComponent(Main.PLAYER_COMMAND.translate()))
                    return@BungeeCommandHandler
                } else if(commandType == CommandType.CONSOLE && sender is ProxiedPlayer) {
                    sender.sendMessage(TextComponent(Main.CONSOLE_COMMAND.translate()))
                    return@BungeeCommandHandler
                }

                val cmd = this@Command
                val failedRequirements = cmd.requirements.filter { !it.check(sender) }
                // First we check the requirements
                if(failedRequirements.isNotEmpty()){
                    BungeeTasks.instance.runAsync { // Send async to prevent lag
                        sender.sendMessage(TextComponent(
                            Main.MISSING_REQUIREMENTS
                                .translate()
                                .plus("\n")
                                .plus(failedRequirements.joinToString("\n") { "&7- &c${it.message}" })
                                .replace("&", "§")
                        ))
                    }
                    return@BungeeCommandHandler
                }

                if(sender is ProxiedPlayer && cmd.permission != null && cmd.permission != "none" && !sender.hasPermission(cmd.permission!!)) {
                    sender.sendMessage(TextComponent(Main.NO_PERMISSION.translate()))
                    return@BungeeCommandHandler
                }

                if(args.isNotEmpty() && cmd.subCommands.isNotEmpty()){  // Now that we have sub commands, let's find one!
                    val subCommandName = args.first() // Here we get the subcommand name
                    val rawArguments = args.drop(1) // Arguments provided by bukkit
                    val subCommand = cmd.subCommands.find { it.name.lowercase() == subCommandName.lowercase() && it.arguments.size == rawArguments.size } // Let's look for a sub command. And because we can specify multiple sub commands with the same name, we need to check the amount of arguments too
                    if(subCommand == null) { // No sub commands? No worries, let's run the main action
                        cmd.onExecute(CommandExecutor(sender, Arguments(indexedArguments = args.toList())))
                        return@BungeeCommandHandler
                    }

                    val inputArguments = mutableMapOf<String, String>() // The arguments that the API will provide to the executor
                    if(subCommand.arguments.isNotEmpty()){ // Only generate named arguments if there is at least one
                        for(index in 0 until subCommand.arguments.size) { // Here we fill the map with the arguments
                            val name = subCommand.arguments[index]
                            if(rawArguments.getOrNull(index) == null){ // Should not be null because all the arguments are required
                                BungeeTasks.instance.runAsync { // If we need to send a message, let's do it async!
                                    sender.sendMessage(TextComponent(Main.MISSING_ARGUMENT.translate(placeholders = mapOf(
                                        "argument" to name // Apply the placeholder to let know the user which argument is missing
                                    ))))
                                }
                                return@BungeeCommandHandler
                            }
                            inputArguments[name] = rawArguments[index] // Here we add the argument to the map
                        }
                    }
                    val arguments = Arguments(indexedArguments = args.toList(), namedArguments = inputArguments) // The sub commands always uses indexed and named arguments
                    subCommand.executor(CommandExecutor(sender, arguments)) // Now we execute it!
                } else {
                    cmd.onExecute(CommandExecutor(sender, Arguments(indexedArguments = args.toList())))
                }
            }
            BungeeLoader.instance.proxy.pluginManager.registerCommand(BungeeLoader.instance, command)
            registered = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
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

private class BungeeCommandHandler(
    name: String,
    permission: String = "",
    aliases: Array<String> = emptyArray(),
    val tabCompleter: (CommandSender, Array<String>) -> Collection<String> = { _, _ -> emptyList() },
    val commandExecutor: (CommandSender, Array<String>) -> Unit = { _, _ -> },
): net.md_5.bungee.api.plugin.Command(name, permission, *aliases), TabExecutor {

    override fun execute(sender: CommandSender, args: Array<String>) = commandExecutor(sender, args)

    override fun onTabComplete(sender: CommandSender, args: Array<String>): MutableIterable<String> = tabCompleter(sender, args).toMutableList()

}
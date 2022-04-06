package xyz.theprogramsrc.commandsmodule

import xyz.theprogramsrc.filesmodule.config.JsonConfig
import xyz.theprogramsrc.filesmodule.utils.folder
import xyz.theprogramsrc.simplecoreapi.global.module.Module
import xyz.theprogramsrc.translationsmodule.Translation
import java.io.File
import java.time.format.DateTimeFormatter

class Main: Module() {

    companion object {
        val NO_PERMISSION = Translation(
            id = "NoPermission",
            group = "Commands",
            defaultValue = "You don't have permission to use this command",
            mainColor = "&c",
        )

        val PLAYER_COMMAND = Translation(
            id = "PlayerCommand",
            group = "Commands",
            defaultValue = "This command can **only** be used by **players**",
            mainColor = "&c",
            colors = arrayOf("&e&b", "&e")
        )

        val CONSOLE_COMMAND = Translation(
            id = "ConsoleCommand",
            group = "Commands",
            defaultValue = "This command can **only** be used by **console**",
            mainColor = "&c",
            colors = arrayOf("&e&b", "&e")
        )

        val MISSING_REQUIREMENTS = Translation(
            id = "MissingRequirements",
            group = "Commands",
            defaultValue = "You didn't pass some requirements to execute the command:",
            mainColor = "&c",
        )

        val MISSING_ARGUMENT = Translation(
            id = "MissingArgument",
            group = "Commands",
            defaultValue = "The argument **{argument}** is missing!",
            mainColor = "&c",
            colors = arrayOf("&e&b")
        )

        private val config = JsonConfig(
            File(
                File("plugins/SimpleCoreAPI/modules/CommandsModule").folder(),
                "config.json"
            )
        )

        fun getDefaultDateFormatter(): DateTimeFormatter {
            config.add("dateFormat", "dd-MM-yyyy HH:mm:ss")
            return DateTimeFormatter.ofPattern(config.getString("dateFormat") ?: "dd-MM-yyyy HH:mm:ss")
        }

    }

    override fun onEnable() {

    }

}
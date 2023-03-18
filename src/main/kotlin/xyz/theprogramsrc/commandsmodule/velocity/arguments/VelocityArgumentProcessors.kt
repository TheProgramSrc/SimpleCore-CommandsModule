package xyz.theprogramsrc.commandsmodule.velocity.arguments

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import xyz.theprogramsrc.commandsmodule.objects.arguments.ArgumentProcessor
import xyz.theprogramsrc.simplecoreapi.velocity.VelocityLoader

object VelocityArgumentProcessors {

    private var loaded = false

    fun load() {
        check(!loaded) { "Velocity Argument processors already loaded" }
        ArgumentProcessor.register(mapOf(
            Player::class.java to PlayerArgumentProcessor()
        ))
    }
}

class PlayerArgumentProcessor: ArgumentProcessor<Player> {

    override fun validate(raw: String): Boolean = VelocityLoader.instance.server.allPlayers.any { it.username == raw || it.uniqueId.toString() == raw }

    override fun get(raw: String): Player = VelocityLoader.instance.server.allPlayers.first { it.username == raw || it.uniqueId.toString() == raw }

}

class RegisteredServerArgumentProcessor: ArgumentProcessor<RegisteredServer> {

    override fun validate(raw: String): Boolean =
        VelocityLoader.instance.server.allServers.any { it.serverInfo.name == raw }

    override fun get(raw: String): RegisteredServer =
        VelocityLoader.instance.server.allServers.first { it.serverInfo.name == raw }

}

package com.chattriggers.bot.messages

import com.chattriggers.bot.CTBot
import com.chattriggers.bot.field
import com.chattriggers.bot.image
import com.chattriggers.bot.sendMessage
import com.chattriggers.bot.types.Module
import com.chattriggers.bot.types.Release
import com.jessecorbett.diskord.api.channel.ChannelClient
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

suspend fun ChannelClient.onCreateModule(module: Module) {
    sendMessage {
        title = "Module created: ${module.name}"
        url = "https://www.chattriggers.com/modules/v/${module.name}"
        field("Author", module.owner.name, true)

        if (module.tags.isNotEmpty())
            field("Tags", module.tags.joinToString(), true)

        if (!module.description.isBlank())
            field("Description", module.description, false)

        if (module.image.isNotBlank())
            image(module.image)

        color = CTBot.MESSAGE_COLOR
        timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
}

suspend fun ChannelClient.onCreateRelease(module: Module, release: Release) {
    sendMessage {
        title = "Release created for module: ${module.name}"
        url = "https://www.chattriggers.com/modules/v/${module.name}"

        if (module.owner.name.isNotBlank())
            field("Author", module.owner.name, true)

        if (release.releaseVersion.isNotBlank())
            field("Release Version", release.releaseVersion, true)

        if (release.modVersion.isNotBlank())
            field("Mod Version", release.modVersion, true)

        if (release.changelog.isNotBlank())
            field("Changelog", release.changelog, false)

        color = CTBot.MESSAGE_COLOR
        timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
}

suspend fun ChannelClient.onDeleteModule(module: Module) {
    sendMessage {
        title = "Module deleted: ${module.name}"
        color = CTBot.MESSAGE_COLOR
        timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
}

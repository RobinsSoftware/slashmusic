/*
Copyright 2022 Robins Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package ca.robinssoftware.slashmusic;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.robinssoftware.slashmusic.MusicThread.SongData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class MusicCommand implements CommandExecutor {

    HashSet<UUID> skips = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            help(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
        case "help":
            help(player);
            break;
        case "join":
            join(player);
            break;
        case "play":
            play(player, args);
            break;
        case "skip":
            skip(player, args);
            break;
        case "sync":
            sync(player);
            break;
        case "info":
            info(player);
            break;
        case "queue":
            queue(player, args);
            break;
        default:
            player.sendMessage("§7>> §cUnrecognized subcommand. §7<<");
            break;
        }

        return true;
    }

    void help(Player player) {
        player.sendMessage("§7>> §aCommands for slashmusic §7<<" + "\n§dhelp§7: §bView available commands."
                + "\n§djoin§7: §bReceive a link to join the music server."
                + "\n§dplay <id>§:: §bQueue a video from YouTube to be played." + "\n§dskip§7: §bVote to skip a track."
                + "\n§dsync§7: §bSync client to the current track and timestamp."
                + "\n§dinfo§7: §bView information about the current track."
                + "\n§dqueue§7: §bView the current track queue.");
    }

    void join(Player player) {
        BaseComponent[] component = new ComponentBuilder(">> ").color(ChatColor.GRAY)
                .append("Click to join the music channel.").color(ChatColor.LIGHT_PURPLE)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL,
                        "https://robinssoftware.github.io/slashmusic-web?address="
                                + MusicPlugin.instance.server.address.getHostString() + ":"
                                + MusicPlugin.instance.server.address.getPort() + "&id=" + player.getUniqueId()))
                .append(" <<").color(ChatColor.GRAY).create();

        player.spigot().sendMessage(component);
        player.sendMessage(
                "§7>> §cNOTE: If the video does not play right away, use /music sync to sync the timestamp with the server. §7<<");

        MusicPlugin.instance.server.links.add(player.getUniqueId());
    }

    void play(Player player, String[] args) {
        if (!MusicPlugin.instance.server.isConnected(player.getUniqueId())) {
            player.sendMessage("§7>> §cYou must be connected to do this. §7<<");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§7>> §cPlease specify a video. §7<<");
            return;
        }
        
        MusicInformation video = new MusicInformation(args[1]);
        
        if (video.video == null) {
            player.sendMessage("§7>> §cVideo does not exist or is not publicly available. §7<<");
            return;
        }
        
        int position = MusicPlugin.instance.thread.add(args[1], player);

        if (position == -1) {
            player.sendMessage("§7>> §cQueue is currently full. §7<<");
            return;
        }

        player.sendMessage(
                "§7>> §aAdding track to queue §7<<" + "\n§dTitle§7: §b" + video.getVideo().getSnippet().getTitle()
                        + "\n§dChannel§7: §b" + video.getVideo().getSnippet().getChannelTitle() + "\n§dDurationh§7: §b"
                        + video.getMinutesAndSeconds()[0] + "m " + video.getMinutesAndSeconds()[1] + "s"
                        + "\n§dPosition§7: §b" + position);
    }

    void skip(Player player, String[] args) {
        if (!MusicPlugin.instance.server.isConnected(player.getUniqueId())) {
            player.sendMessage("§7>> §cYou must be connected to do this. §7<<");
            return;
        }

        if (MusicPlugin.instance.thread.current == null) {
            player.sendMessage("§7>> §cNo track is currently playing. §7<<");
            return;
        }

        if ((player.hasPermission("slashmusic.admin") || player.isOp()) && args.length > 1
                && args[1].equalsIgnoreCase("force")) {
            player.sendMessage("§7>> §dForce skipping track. §7<<");
            MusicPlugin.instance.thread.forceSkip();
            return;
        }

        if (!MusicPlugin.instance.thread.skip(player)) {
            player.sendMessage("§7>> §cYou have already voted to skip this track. §7<<");
            return;
        }

        player.sendMessage("§7>> §dVoting to skip current track. §7<<");
    }

    void sync(Player player) {
        if (!MusicPlugin.instance.server.isConnected(player.getUniqueId())) {
            return;
        }

        player.sendMessage("§7>> §dSyncing client to server. §7<<");
        MusicPlugin.instance.server.sync(player.getUniqueId());
    }

    void info(Player player) {
        SongData data = MusicPlugin.instance.thread.current;

        if (data == null) {
            player.sendMessage("§7>> §cNo track is currently playing. §7<<");
            return;
        }

        player.sendMessage("§7>> §aCurrent track information §7<<" + "\n§dTitle§7: §d"
                + data.track.getVideo().getSnippet().getTitle() + "\n§dChannel§7: §d"
                + data.track.getVideo().getSnippet().getChannelTitle() + "\n§dLength§7: §b"
                + data.track.getMinutesAndSeconds()[0] + "m " + data.track.getMinutesAndSeconds()[1] + "s"
                + "\n§dQueued by§7: §d" + Bukkit.getOfflinePlayer(data.queuer).getName());
    }

    void queue(Player player, String[] args) {
        Object[] queue = MusicPlugin.instance.thread.tracks.toArray();

        if (queue.length == 0) {
            player.sendMessage("§7>> §dTrack queue is currently empty. §7<<");
            return;
        }

        if ((player.hasPermission("slashmusic.admin") || player.isOp()) && args.length > 1
                && args[1].equalsIgnoreCase("clear")) {
            player.sendMessage("§7>> §dClearing queue. §7<<");
            MusicPlugin.instance.thread.clearQueue();
            return;
        }

        player.sendMessage("§7>> §aTrack queue §7<<");

        for (int i = 0; i < queue.length; i++) {
            player.sendMessage("§7" + (i + 1) + " §d" + Bukkit.getOfflinePlayer(((SongData) queue[i]).queuer).getName()
                    + " §b" + ((SongData) queue[i]).track.getVideo().getSnippet().getTitle());
        }
    }

}

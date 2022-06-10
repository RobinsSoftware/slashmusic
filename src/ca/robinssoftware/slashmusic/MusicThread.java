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
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.bukkit.entity.Player;

public class MusicThread extends Thread {

    boolean active = true, skip;
    Queue<SongData> tracks = new LinkedList<>();
    HashSet<UUID> skips = new HashSet<UUID>();

    long start, end;
    SongData current;

    public void clearQueue() {
        tracks.clear();
    }
    
    public void forceSkip() {
        skip = true;
    }
    
    public boolean skip(Player player) {
        if (skips.contains(player.getUniqueId()))
            return false;

        skips.add(player.getUniqueId());

        if (skips.size() != 0 && skips.size() >= (MusicPlugin.instance.server.connections.size() / 2))
            skip = true;

        return true;
    }

    public int add(String id, Player player) {
        if (tracks.size() >= MusicPlugin.instance.getConfig().getInt("queue-max"))
            return -1;
        
        tracks.add(new SongData(id, player.getUniqueId(), new MusicInformation(id)));
        return tracks.size();
    }

    public String getPacket() {
        if (current == null)
            return "stop";
        else
            return "play;" + current.id + ";" + Math.round((System.currentTimeMillis() - start) / 1000F);
    }

    @Override
    public void run() {
        while (active) {
            // move to next
            if (System.currentTimeMillis() > end || skip || current == null) {
                if (skip) {
                    MusicPlugin.instance.server.broadcastToPlayers("§7>> §dSkipping current track. §7<<");
                    skips.clear();
                    skip = false;
                }

                current = tracks.poll();
                start = System.currentTimeMillis();

                if (current != null) {
                    MusicPlugin.instance.server.broadcastToPlayers(
                            "§7>> §dNow playing§b " + current.track.video.getSnippet().getTitle() + " §7<<");
                    end = System.currentTimeMillis() + ((long) (current.track.getSeconds() + 1) * 1000);
                }

                MusicPlugin.instance.server.broadcast(getPacket());
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }
        }
    }

    public static class SongData {
        final String id;
        final UUID queuer;
        final MusicInformation track;

        SongData(String id, UUID queuer, MusicInformation song) {
            this.id = id;
            this.queuer = queuer;
            this.track = song;
        }
    }

}

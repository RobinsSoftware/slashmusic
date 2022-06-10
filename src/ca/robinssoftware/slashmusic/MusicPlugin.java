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

import java.net.InetSocketAddress;

import org.bukkit.plugin.java.JavaPlugin;

public class MusicPlugin extends JavaPlugin {

    static MusicPlugin instance;

    MusicServer server;
    MusicThread thread;

    @Override
    public void onEnable() {
        getCommand("music").setExecutor(new MusicCommand());
        getCommand("music").setTabCompleter(new MusicTabCompleter());

        saveDefaultConfig();

        server = new MusicServer(new InetSocketAddress(getConfig().getString("host"), getConfig().getInt("port")));
        server.start();

        thread = new MusicThread();
        thread.setName("slashmusic-thread");
        thread.start();

        instance = this;
    }

    @Override
    public void onDisable() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        thread.active = false;

        thread = null;
        server = null;
        instance = null;
    }

}

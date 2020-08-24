package it.multicoredev.cookiesrain;

import it.multicoredev.cookiesrain.commands.CookiesRainCompleter;
import it.multicoredev.cookiesrain.commands.CookiesRainExecutor;
import it.multicoredev.cookiesrain.listeners.OnPlayerPickup;
import it.multicoredev.cookiesrain.storage.LeaderboardManager;
import it.multicoredev.mbcore.spigot.Chat;
import it.multicoredev.mclib.yaml.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Copyright © 2020 by Daniele Patella & Lorenzo Magni
 * This file is part of CookiesRain.
 * CookiesRain is under "The 3-Clause BSD License", you can find a copy <a href="https://opensource.org/licenses/BSD-3-Clause">here</a>.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
public class CookiesRain extends JavaPlugin {
    private Configuration config;
    private LeaderboardManager manager;
    private Game game;

    @Override
    public void onEnable() {
        try {
            config = new Configuration(new File(getDataFolder(), "config.yml"), getResource("config.yml"));

            if (!getDataFolder().exists() || !getDataFolder().isDirectory()) {
                if (!getDataFolder().mkdir()) throw new IOException("Cannot create plugin directory");
            }

            config.autoload();
        } catch (IOException e) {
            e.printStackTrace();
            onDisable();
            return;
        }


        game = new Game(this, config);
        if (!game.initLeaderboard()) {
            onDisable();
            return;
        }
        game.resumeGame();

        getCommand("cookiesrain").setExecutor(new CookiesRainExecutor(this, config, game));
        getCommand("cookiesrain").setTabCompleter(new CookiesRainCompleter());

        getServer().getPluginManager().registerEvents(new OnPlayerPickup(config, game), this);

        Chat.info("&eCookiesRain &2enabled&e!");
    }

    @Override
    public void onDisable() {
        if (game.isRunning()) game.stopGame(true);
        Chat.info("&eCookiesRain &4disabled&e!");
    }
}

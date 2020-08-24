package it.multicoredev.cookiesrain.commands;

import it.multicoredev.cookiesrain.Game;
import it.multicoredev.cookiesrain.storage.User;
import it.multicoredev.mbcore.spigot.Chat;
import it.multicoredev.mclib.yaml.Configuration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

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
public class CookiesRainExecutor implements CommandExecutor {
    private final Plugin plugin;
    private final Configuration config;
    private final Game game;

    public CookiesRainExecutor(Plugin plugin, Configuration config, Game game) {
        this.plugin = plugin;
        this.config = config;
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Chat.send(config.getString("messages.incorrect-usage"), sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("leaderboard")) {
            if (game.isRunning() && !sender.hasPermission("cookiesrain.admin")) {
                Chat.send(config.getString("messages.leaderboard-disabled"), sender);
                return true;
            }

            if (game.getLeaderboard().isEmpty()) {
                Chat.send(config.getString("messages.leaderboard-empty"), sender);
                return true;
            }

            List<User> users = game.getLeaderboard().getUsers();
            int size = Math.min(users.size(), 20);

            for (int i = 0; i < size; i++) {
                User user = users.get(i);
                Chat.broadcast(config.getString("messages.leaderboard")
                        .replace("{position}", String.valueOf(i + 1))
                        .replace("{player}", user.getNick())
                        .replace("{points}", String.valueOf(user.getPoints()))
                );
            }

            return true;
        }

        if (!sender.hasPermission("cookiesrain.admin")) {
            Chat.send(config.getString("messages.insufficient-perms"), sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (game.isRunning()) {
                Chat.send(config.getString("messages.stop-before-reload"), sender);
                return true;
            }

            plugin.onDisable();
            plugin.onEnable();
            Chat.send(config.getString("messages.plugin-reloaded"), sender);
        } else if (args[0].equalsIgnoreCase("start")) {
            if (game.isRunning()) {
                Chat.send(config.getString("messages.game-running"), sender);
                return true;
            }

            game.sendTitle(config.getString("messages.start-title"), config.getString("messages.start-subtitle"));

            for (String line : config.getStringList("messages.end-messages")) {
                Chat.broadcast(line);
            }

            game.startGame();
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (!game.isRunning()) {
                Chat.send(config.getString("messages.game-not-running"), sender);
                return true;
            }

            game.stopGame(true);
            Chat.send(config.getString("messages.game-stopped"), sender);
        } else {
            Chat.send(config.getString("messages.incorrect-usage"), sender);
            return true;
        }

        return false;
    }
}

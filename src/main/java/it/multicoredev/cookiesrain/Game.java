package it.multicoredev.cookiesrain;

import de.tr7zw.nbtapi.NBTItem;
import it.multicoredev.cookiesrain.storage.Leaderboard;
import it.multicoredev.cookiesrain.storage.LeaderboardManager;
import it.multicoredev.cookiesrain.storage.User;
import it.multicoredev.mbcore.spigot.Chat;
import it.multicoredev.mclib.yaml.Configuration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

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
public class Game {
    private final Plugin plugin;
    private final Configuration config;
    private LeaderboardManager manager;
    private CopyOnWriteArrayList<Cookie> cookies = new CopyOnWriteArrayList<>();
    private BukkitTask rainTask;
    private BukkitTask despawnTask;
    private BukkitTask endTask;
    private boolean running = false;

    Game(Plugin plugin, Configuration config) {
        this.plugin = plugin;
        this.config = config;
    }

    boolean initLeaderboard() {
        manager = new LeaderboardManager(new File(plugin.getDataFolder(), "leaderboard.yml"));
        return manager.load();
    }

    public void startGame() {
        despawnTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new DespawnTask(), 0, 10);
        rainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new RainTask(), 0, 20);
        running = true;
    }

    public void stopGame(boolean force) {
        running = false;

        if (!rainTask.isCancelled()) rainTask.cancel();
        if (!despawnTask.isCancelled()) despawnTask.cancel();
        if (!endTask.isCancelled()) endTask.cancel();
        rainTask = null;
        despawnTask = null;
        endTask = null;

        manager.getLeaderboard().setTimer(-1);
        saveLeaderboard();

        if (!force) {
            endTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, new EndTask());
        }
    }

    public void resumeGame() {
        if (getLeaderboard().getTimer() != -1) {
            sendTitle(Chat.getTranslated(config.getString("messages.resume-title")), Chat.getTranslated(config.getString("messages.resume-subtitle")));
            for (String line : config.getStringList("messages.resume-messages")) {
                Chat.broadcast(line);
            }
            startGame();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public CopyOnWriteArrayList<Cookie> getCookies() {
        return cookies;
    }

    public Leaderboard getLeaderboard() {
        return manager.getLeaderboard();
    }

    public void saveLeaderboard() {
        new Thread(manager::save);
    }

    public void sendTitle(String title, String subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(Chat.getTranslated(title), Chat.getTranslated(subtitle), 10, 60, 30);
        }
    }

    private class RainTask implements Runnable {

        @Override
        public void run() {
            if (new Random().nextInt(100) > config.getInt("cookie-spawn-chance")) return;

            Player player = getRandomPlayer();
            Location playerLoc = player.getLocation();
            if (playerLoc.getWorld() == null) return;
            if (!playerLoc.getWorld().getName().equalsIgnoreCase(config.getString("rain-world"))) return;

            int spawnDistance = config.getInt("spawn-distance");
            double x = playerLoc.getX() + getRandom(-spawnDistance, spawnDistance);
            double z = playerLoc.getZ() + getRandom(-spawnDistance, spawnDistance);
            double y = playerLoc.getWorld().getHighestBlockAt((int) x, (int) z).getY() + config.getInt("height-offset");
            Location cookieLoc = new Location(playerLoc.getWorld(), x, y, z);

            ItemStack cookie = new ItemStack(Material.COOKIE, 1);
            NBTItem nbt = new NBTItem(cookie);
            nbt.setBoolean("cookie", true);
            cookie = nbt.getItem();

            ItemStack finalCookie = cookie;
            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Item item = playerLoc.getWorld().dropItemNaturally(cookieLoc, finalCookie);

                cookies.add(new Cookie(item, 0));
                return true;
            });


            manager.getLeaderboard().incrementTimer();
            saveLeaderboard();
        }

        private Player getRandomPlayer() {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            return players.get(new Random().nextInt(players.size()));
        }

        private int getRandom(int min, int max) {
            return new Random().nextInt(max - min) + min;
        }
    }

    private class DespawnTask implements Runnable {
        private int cookieLifespan = config.getInt("cookie-lifespan") * 20;

        @Override
        public void run() {
            for (Cookie cookie : cookies) {
                if (cookie.getCookie().isOnGround() || isInWater(cookie)) {
                    if (cookie.getTime() >= cookieLifespan) {
                        cookies.remove(cookie);

                        Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                            cookie.getCookie().remove();

                            return true;
                        });
                    } else {
                        cookie.incrementTime(10);
                        cookies.set(cookies.indexOf(cookie), cookie);
                    }
                }
            }

            if (getLeaderboard().getTimer() >= config.getLong("game-duration")) {
                stopGame(false);
            }

            long missingTime = config.getInt("game-duration") - getLeaderboard().getTimer();
            String time = getMissingTime(missingTime);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Chat.getTranslated("&6" + time)));
            }
        }

        private boolean isInWater(Cookie cookie) {
            Item item = cookie.getCookie();
            Material material = item.getLocation().getBlock().getType();

            return material == Material.WATER;
        }

        private String getMissingTime(long seconds) {
            long minutes = TimeUnit.MINUTES.convert(seconds, TimeUnit.SECONDS);
            seconds = seconds - (minutes * 60);

            return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        }
    }

    private class EndTask implements Runnable {

        @Override
        public void run() {
            List<User> top = manager.getLeaderboard().getUsers();
            if (top.size() < 5) return;

            sendTitle(Chat.getTranslated(config.getString("messages.end-title")), Chat.getTranslated(config.getString("messages.end-subtitle")));
            for (String line : config.getStringList("messages.end-messages")) {
                if (line.toLowerCase().contains("{leaderboard}")) {
                    String num = line.substring(line.indexOf("}(") + 2, line.indexOf(")"));
                    int n = 5;
                    try {
                        n = Integer.parseInt(num);
                    } catch (NumberFormatException ignored) {
                    }

                    List<User> users = getLeaderboard().getUsers();
                    n = Math.min(n, users.size());

                    for (int i = 0; i < n; i++) {
                        User user = users.get(i);
                        Chat.broadcast(config.getString("messages.leaderboard")
                                .replace("{position}", String.valueOf(i + 1))
                                .replace("{player}", user.getNick())
                                .replace("{points}", String.valueOf(user.getPoints()))
                        );
                    }
                } else {
                    Chat.broadcast(line);
                }
            }

            manager.getLeaderboard().setTimer(0);
            saveLeaderboard();
        }
    }
}

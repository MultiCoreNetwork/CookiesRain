package it.multicoredev.cookiesrain.listeners;

import de.tr7zw.nbtapi.NBTItem;
import it.multicoredev.cookiesrain.Cookie;
import it.multicoredev.cookiesrain.Game;
import it.multicoredev.cookiesrain.storage.User;
import it.multicoredev.mbcore.spigot.Chat;
import it.multicoredev.mclib.yaml.Configuration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

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
public class OnPlayerPickup implements Listener {
    private final Configuration config;
    private final Game game;

    public OnPlayerPickup(Configuration config, Game game) {
        this.config = config;
        this.game = game;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupCookie(EntityPickupItemEvent event) {
        if (!game.isRunning()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Item item = event.getItem();
        NBTItem nbt = new NBTItem(item.getItemStack());
        if (!nbt.hasKey("cookie")) return;

        Cookie cookie = getCookie(item);

        if (cookie == null) return;
        event.setCancelled(true);

        game.getCookies().remove(cookie);
        item.remove();

        Player player = (Player) event.getEntity();
        User user = game.getLeaderboard().getUser(player.getUniqueId());
        if (user == null) user = new User(player.getUniqueId(), player.getName());

        user.addPoints(config.getInt("cookie-points"));
        game.getLeaderboard().setUser(user);
        game.saveLeaderboard();

        Chat.send(config.getString("messages.cookie-caught")
                .replace("{earned_points}", String.valueOf(config.getInt("cookie-points"))
                        .replace("{total_points}", String.valueOf(user.getPoints()))), player);
    }

    private Cookie getCookie(Item item) {
        for (Cookie cookie : game.getCookies()) {
            if (cookie.getCookie().getUniqueId().equals(item.getUniqueId())) return cookie;
        }

        return null;
    }
}

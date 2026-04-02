package com.earth4ck;

import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Earth4ck extends JavaPlugin {

    @Override
    public void onEnable() {

        // aplica no startup completo
        Bukkit.getScheduler().runTaskLater(this, () -> {

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 20");

            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRules.RANDOM_TICK_SPEED, 3);
            }

            Bukkit.getLogger().info("[3arth4ck] Valores aplicados no startup!");

        }, 100L); // espera 5 segundos
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

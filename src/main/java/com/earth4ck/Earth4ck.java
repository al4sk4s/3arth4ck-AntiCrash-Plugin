package com.earth4ck;

import io.netty.channel.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public final class Earth4ck extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 20");

            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRules.RANDOM_TICK_SPEED, 3);
            }

            Bukkit.getLogger().info("[3arth4ck] Valores aplicados no startup!");
        }, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            inject(p);
            sendFakeOp(p);
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        uninject(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            inject(p);
            sendFakeOp(p);
        }, 10L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            inject(p);
            sendFakeOp(p);
        }, 10L);
    }

    // =========================
    // 🔥 INJEÇÃO (CORE)
    // =========================

    private void inject(Player player) {
        try {
            CraftPlayer cp = (CraftPlayer) player;
            ServerPlayer sp = cp.getHandle();

            Connection connection = sp.connection.connection;
            Channel channel = connection.channel;

            if (channel.pipeline().get("gamemode_bypass") != null) return;

            /*for (String name : channel.pipeline().names()) {
                Bukkit.getLogger().info("PIPELINE: " + name);
            }*/

            channel.pipeline().addBefore("packet_handler", "gamemode_bypass", new ChannelDuplexHandler() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {

                    String name = packet.getClass().getSimpleName();

                    // 🔥 NOVO PACKET 1.21+
                    if (name.equals("ServerboundChangeGameModePacket")) {
                        handleGamemodePacket(sp, packet);
                        return; // cancela Paper
                    }

                    super.channelRead(ctx, packet);
                }
            });

            getLogger().info("[3arth4ck] Injector aplicado em " + player.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uninject(Player player) {
        try {
            CraftPlayer cp = (CraftPlayer) player;
            ServerPlayer sp = cp.getHandle();

            Connection connection = sp.connection.connection;
            Channel channel = connection.channel;

            if (channel.pipeline().get("gamemode_bypass") != null) {
                channel.pipeline().remove("gamemode_bypass");
            }

        } catch (Exception ignored) {}
    }

    // =========================
    // 🎯 PACKET HANDLER
    // =========================

    private void handleGamemodePacket(ServerPlayer player, Object packet) {
        try {
            Object gameType = null;

            // 🔍 procura automaticamente o campo correto
            for (var field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                Object value = field.get(packet);

                if (value != null && value.getClass().getSimpleName().equals("GameType")) {
                    gameType = value;
                    break;
                }
            }

            if (gameType == null) {
                Bukkit.getLogger().warning("Não achou GameType no packet!");
                return;
            }

            GameMode gm = convert(gameType);

            Bukkit.getScheduler().runTask(this, () -> {
                player.getBukkitEntity().setGameMode(gm);
                //player.getBukkitEntity().sendMessage("§aGamemode alterado para " + gm.name());
                Player bukkitPlayer = player.getBukkitEntity();

                getServer().broadcastMessage(
                        "§7[" + bukkitPlayer.getDisplayName() + ": Set own game mode to "+ gm.name() + "]"
                );
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GameMode convert(Object gameType) {
        return switch (gameType.toString()) {
            case "SURVIVAL" -> GameMode.SURVIVAL;
            case "CREATIVE" -> GameMode.CREATIVE;
            case "ADVENTURE" -> GameMode.ADVENTURE;
            case "SPECTATOR" -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }

    // =========================
    // 🎭 FAKE OP
    // =========================

    private void sendFakeOp(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);

            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundEntityEventPacket");

            Object packet = packetClass
                    .getConstructor(Class.forName("net.minecraft.world.entity.Entity"), byte.class)
                    .newInstance(handle, (byte) 28);

            Object connection = handle.getClass().getField("connection").get(handle);

            Method sendPacket = findMethod(connection.getClass(), "send", 1);
            sendPacket.invoke(connection, packet);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private Method findMethod(Class<?> clazz, String name, int... paramCount) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(name)) {
                if (paramCount.length == 0 || m.getParameterCount() == paramCount[0]) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                if (paramCount.length == 0 || m.getParameterCount() == paramCount[0]) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
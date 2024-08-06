package avalanche7.net.forgeannouncements.utils;

import avalanche7.net.forgeannouncements.configs.RestartConfigHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@Mod.EventBusSubscriber(modid = "forgeannouncements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Restart {

    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private static final DecimalFormat TIME_FORMATTER = new DecimalFormat("00");
    public static boolean isRestarting = false;
    private static MinecraftServer server;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void debugLog(String message) {
        if (RestartConfigHandler.CONFIG.debugEnabled.get()) {
            LOGGER.info(message);
        }
    }
    private static void debugLog(String message, Exception e) {
        if (RestartConfigHandler.CONFIG.debugEnabled.get()) {
            LOGGER.error(message, e);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        debugLog("Server is starting, scheduling restarts.");
        String restartType = RestartConfigHandler.CONFIG.restartType.get();
        debugLog("Configured restart type: " + restartType);
        switch (restartType) {
            case "Fixed":
                double restartInterval = RestartConfigHandler.CONFIG.restartInterval.get();
                debugLog("Fixed restart scheduled every " + restartInterval + " hours.");
                scheduleFixedRestart();
                break;
            case "Realtime":
                List<String> realTimeIntervals = (List<String>) RestartConfigHandler.CONFIG.realTimeInterval.get();
                debugLog("Real-time restarts will be scheduled with intervals: " + realTimeIntervals);
                scheduleRealTimeRestart();
                break;
            case "None":
                debugLog("No automatic restarts scheduled.");
                break;
            default:
                debugLog("Unknown restart type specified: " + restartType);
                break;
        }
    }

    public static void shutdown() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            debugLog("Shutdown initiated at: " + new Date());
            try {
                debugLog("Stopping server...");
                server.saveEverything(true, true, true);
                server.halt(false);
                debugLog("Server stopped successfully.");
            } catch (Exception e) {
                debugLog("Error during server shutdown", e);
            }
        } else {
            debugLog("Server instance is null, cannot shutdown.");
        }
    }

    public static void warningMessages(double rInterval) {
        List<? extends Integer> timerBroadcast = RestartConfigHandler.CONFIG.timerBroadcast.get();
        long startTimestamp = System.currentTimeMillis();
        long totalIntervalSeconds = (long) (rInterval);

        debugLog("Broadcasting warning messages with interval: " + rInterval + " seconds.");

        for (int broadcastTime : timerBroadcast) {
            long broadcastIntervalSeconds = totalIntervalSeconds - broadcastTime;
            if (broadcastIntervalSeconds > 0) {
                debugLog("Scheduling warning message for: " + broadcastIntervalSeconds + " seconds from now.");
                Timer warnTimer = new Timer();
                warnTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        long timeElapsed = (System.currentTimeMillis() - startTimestamp) / 1000;
                        long timeLeft = totalIntervalSeconds - timeElapsed;

                        int hours = (int) (timeLeft / 3600);
                        int minutes = (int) ((timeLeft % 3600) / 60);
                        int seconds = (int) (timeLeft % 60);

                        String formattedTime = String.format("%dh %02dm %02ds", hours, minutes, seconds);

                        if (RestartConfigHandler.CONFIG.timerUseChat.get()) {
                            String customMessage = RestartConfigHandler.CONFIG.BroadcastMessage.get()
                                    .replace("{hours}", String.valueOf(hours))
                                    .replace("{minutes}", TIME_FORMATTER.format(minutes))
                                    .replace("{seconds}", TIME_FORMATTER.format(seconds));
                            TextComponent messageComponent = new TextComponent(customMessage);
                            server.getPlayerList().broadcastMessage(messageComponent, ChatType.SYSTEM, Util.NIL_UUID);
                        }

                        if (RestartConfigHandler.CONFIG.titleEnabled.get()) {
                            String titleMessage = RestartConfigHandler.CONFIG.titleMessage.get()
                                    .replace("{hours}", String.valueOf(hours))
                                    .replace("{minutes}", TIME_FORMATTER.format(minutes))
                                    .replace("{seconds}", TIME_FORMATTER.format(seconds));

                            TextComponent titleComponent = new TextComponent(titleMessage);
                            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
                            }
                        }

                        if (RestartConfigHandler.CONFIG.playSoundEnabled.get()) {
                            String soundString = RestartConfigHandler.CONFIG.playSoundString.get();
                            SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundString));
                            if (soundEvent != null) {
                                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                    player.playNotifySound(soundEvent, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                                }
                            }
                        }

                        if (RestartConfigHandler.CONFIG.bossbarEnabled.get()) {
                            String bossBarMessage = RestartConfigHandler.CONFIG.bossBarMessage.get()
                                    .replace("{time}", formattedTime);
                            Component message = new TextComponent(bossBarMessage);
                            ServerBossEvent bossEvent = new ServerBossEvent(message, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

                            ClientboundBossEventPacket addPacket = ClientboundBossEventPacket.createAddPacket(bossEvent);
                            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                player.connection.send(addPacket);
                            }
                            bossEvent.setProgress((float) timeLeft / totalIntervalSeconds);
                            long bossbarTime = 1;
                            executorService.schedule(() -> {
                                ClientboundBossEventPacket removePacket = ClientboundBossEventPacket.createRemovePacket(bossEvent.getId());
                                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                    player.connection.send(removePacket);
                                }
                            }, bossbarTime, TimeUnit.SECONDS);
                        }
                    }
                }, broadcastIntervalSeconds * 1000);
            }
        }
    }

    public static void scheduleFixedRestart() {
        double intervalHours = RestartConfigHandler.CONFIG.restartInterval.get();
        if (intervalHours <= 0) {
            debugLog("Invalid restart interval specified: " + intervalHours);
            return;
        }

        long intervalMillis = (long) (intervalHours * 3600 * 1000);
        if (executorService.isShutdown()) {
            debugLog("ExecutorService has been shut down. Not scheduling new tasks.");
            return;
        }

        debugLog("Scheduling fixed restart every " + intervalHours + " hours.");
        executorService.scheduleAtFixedRate(() -> {
            try {
                shutdown();
            } catch (Exception e) {
                debugLog("Error during fixed restart execution", e);
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public static void scheduleRealTimeRestart() {
        List<String> realTimeIntervals = (List<String>) RestartConfigHandler.CONFIG.realTimeInterval.get();
        if (realTimeIntervals == null || realTimeIntervals.isEmpty()) {
            debugLog("No valid restart times found in the configuration.");
            return;
        }

        debugLog("Scheduling real-time restarts.");
        Calendar nowCal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

        long minDelayMillis = Long.MAX_VALUE;

        for (String restartTimeStr : realTimeIntervals) {
            try {
                Date restartTime = format.parse(restartTimeStr);
                Calendar restartCal = Calendar.getInstance();
                restartCal.setTime(restartTime);
                restartCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
                restartCal.set(Calendar.DAY_OF_YEAR, nowCal.get(Calendar.DAY_OF_YEAR));

                if (nowCal.after(restartCal)) {
                    restartCal.add(Calendar.DAY_OF_MONTH, 1);
                }

                long delayMillis = restartCal.getTimeInMillis() - nowCal.getTimeInMillis();
                if (delayMillis < minDelayMillis) {
                    minDelayMillis = delayMillis;
                }
            } catch (ParseException e) {
                debugLog("Error parsing restart time: " + restartTimeStr, e);
            }
        }

        if (minDelayMillis == Long.MAX_VALUE) {
            debugLog("No valid restart times found after processing.");
            return;
        }

        debugLog("Scheduled shutdown at: " + format.format(new Date(System.currentTimeMillis() + minDelayMillis)));
        isRestarting = true;

        executorService.schedule(() -> {
            debugLog("Timer task triggered.");
            shutdown();
        }, minDelayMillis, TimeUnit.MILLISECONDS);

        warningMessages(minDelayMillis / 1000.0);
    }

}

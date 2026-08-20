package pl.kiosel.playerlist.model;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.rosacore.RosaLogger;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public final class Ticker implements Runnable {

    private static final CopyOnWriteArrayList<Runnable> TASKS = new CopyOnWriteArrayList<>();
    private static final AtomicLong AVERAGE_NANOS = new AtomicLong();
    private static final AtomicInteger TICK = new AtomicInteger();

    private static volatile int refreshTicks = 1;
    private static volatile AdvancedPlayerList plugin;
    private static volatile BukkitTask tickerTask;
    private static volatile ExecutorService asyncExecutor;

    private Ticker() {
    }

    public static synchronized void initialize(AdvancedPlayerList plugin) {
        if (tickerTask != null) {
            return;
        }

        Ticker.plugin = plugin;
        asyncExecutor = Executors.newSingleThreadExecutor(new WorkerThreadFactory());
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Ticker(), 1L, 1L);
    }

    public static synchronized void shutdown() {
        BukkitTask task = tickerTask;
        tickerTask = null;
        if (task != null) {
            task.cancel();
        }

        ExecutorService executor = asyncExecutor;
        asyncExecutor = null;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2L, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        TASKS.clear();
        TICK.set(0);
        AVERAGE_NANOS.set(0L);
        plugin = null;
    }

    public static void submit(Runnable task) {
        ExecutorService executor = asyncExecutor;
        if (task == null || executor == null || executor.isShutdown()) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    RosaLogger.getInstance().log(Level.WARNING, "Background task failed", throwable);
                }
            });
        } catch (RejectedExecutionException ignored) {
        }
    }

    public static void delay(Runnable task, long ticks) {
        AdvancedPlayerList owner = plugin;
        if (task == null || owner == null || !owner.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(owner, task, Math.max(0L, ticks));
    }

    public static void delayMillis(Runnable task, long millis) {
        long ticks = Math.max(0L, (millis + 49L) / 50L);
        delay(task, ticks);
    }

    public static void clear() {
        TASKS.clear();
    }

    public static void setPeriodTicks(int period) {
        refreshTicks = Math.max(1, period);
    }

    public static long getAverageNanoSecondsPerTick() {
        return AVERAGE_NANOS.get();
    }

    public static boolean optimize() {
        return TICK.get() % refreshTicks == 0;
    }

    @Override
    public void run() {
        long startedAt = System.nanoTime();
        for (Runnable task : TASKS) {
            try {
                task.run();
            } catch (Throwable throwable) {
                RosaLogger.getInstance().log(Level.WARNING, "Scheduled task failed", throwable);
            }
        }

        long elapsed = System.nanoTime() - startedAt;
        long previous = AVERAGE_NANOS.get();
        AVERAGE_NANOS.set(previous == 0L ? elapsed : (previous * 7L + elapsed) / 8L);
        TICK.updateAndGet(value -> value >= 9_999_999 ? 0 : value + 1);
    }

    public static void register(Runnable task) {
        if (task != null) {
            TASKS.addIfAbsent(task);
        }
    }

    public static void unregister(Runnable task) {
        TASKS.remove(task);
    }

    public static int tickTime() {
        return TICK.get();
    }

    private static final class WorkerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "AdvancedPlayerList-Worker");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, throwable) ->
                    RosaLogger.getInstance().log(Level.WARNING, "Background task failed", throwable));
            return thread;
        }
    }
}

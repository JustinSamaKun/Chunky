package org.popcraft.chunky;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Chunky extends JavaPlugin {
    private ConfigStorage configStorage;
    private ConcurrentHashMap<World, GenTask> genTasks;
    private World world;
    private int x, z, radius;
    private boolean queue;
    private boolean silent;
    private int quiet;
    private Metrics metrics;

    private final static String FORMAT_START = "[Chunky] Task started for %s at %d, %d with radius %d.";
    private final static String FORMAT_STARTED_ALREADY = "[Chunky] Task already started for %s!";
    private final static String FORMAT_PAUSE = "[Chunky] Task paused for %s.";
    private final static String FORMAT_CONTINUE = "[Chunky] Task continuing for %s.";
    private final static String FORMAT_WORLD = "[Chunky] World changed to %s.";
    private final static String FORMAT_CENTER = "[Chunky] Center changed to %d, %d.";
    private final static String FORMAT_RADIUS = "[Chunky] Radius changed to %d.";
    private final static String FORMAT_SILENT = "[Chunky] Silent mode %s.";
    private final static String FORMAT_QUIET = "[Chunky] Quiet interval set to %d seconds.";

    @Override
    public void onEnable() {
        this.configStorage = new ConfigStorage(this);
        this.genTasks = new ConcurrentHashMap<>();
        this.world = this.getServer().getWorlds().get(0);
        this.x = 0;
        this.z = 0;
        this.radius = 500;
        this.silent = false;
        this.quiet = 0;
        this.metrics = new Metrics(this, 8211);
    }

    @Override
    public void onDisable() {
        pause(this.getServer().getConsoleSender());
        this.getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "start":
                return start(sender);
            case "pause":
                return pause(sender);
            case "continue":
                return cont(sender);
            case "cancel":
                return cancel(sender);
            case "world":
                return world(sender, args);
            case "center":
                return center(sender, args);
            case "radius":
                return radius(sender, args);
            case "silent":
                return silent(sender);
            case "quiet":
                return quiet(sender, args);
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "pause", "continue", "world", "center", "radius", "silent", "quiet");
        }
        if (args.length == 2 && "world".equalsIgnoreCase(args[0])) {
            return Bukkit.getWorlds().stream().map(World::getName).map(String::toLowerCase).filter(w -> w.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean start(CommandSender sender) {
        if (genTasks.containsKey(world)) {
            sender.sendMessage(String.format(FORMAT_STARTED_ALREADY, world.getName()));
            return true;
        }
        GenTask genTask = new GenTask(this, world, radius, x, z);
        genTasks.put(world, genTask);
        this.getServer().getScheduler().runTaskAsynchronously(this, genTask);
        sender.sendMessage(String.format(FORMAT_START, world.getName(), x, z, radius));
        return true;
    }

    private boolean pause(CommandSender sender) {
        for (GenTask genTask : genTasks.values()) {
            genTask.cancel();
            sender.sendMessage(String.format(FORMAT_PAUSE, genTask.getWorld().getName()));
        }
        return true;
    }

    private boolean cont(CommandSender sender) {
        configStorage.loadTasks().forEach(genTask -> {
            if (!genTasks.containsKey(genTask.getWorld())) {
                genTasks.put(genTask.getWorld(), genTask);
                this.getServer().getScheduler().runTaskAsynchronously(this, genTask);
                sender.sendMessage(String.format(FORMAT_CONTINUE, world.getName()));
            } else {
                sender.sendMessage(String.format(FORMAT_STARTED_ALREADY, world.getName()));
            }
        });
        return true;
    }

    private boolean cancel(CommandSender sender) {
        pause(sender);
        this.getConfigStorage().reset();
        this.getServer().getScheduler().cancelTasks(this);
        return true;
    }

    private boolean world(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }
        Optional<World> newWorld = Input.tryWorld(args[1]);
        if (!newWorld.isPresent()) {
            return false;
        }
        this.world = newWorld.get();
        sender.sendMessage(String.format(FORMAT_WORLD, world.getName()));
        return true;
    }

    private boolean center(CommandSender sender, String[] args) {
        Optional<Integer> newX = Optional.empty();
        if (args.length > 1) {
            newX = Input.tryInteger(args[1]);
        }
        Optional<Integer> newZ = Optional.empty();
        if (args.length > 2) {
            newZ = Input.tryInteger(args[2]);
        }
        if (!newX.isPresent() || !newZ.isPresent()) {
            return false;
        }
        this.x = newX.get();
        this.z = newZ.get();
        sender.sendMessage(String.format(FORMAT_CENTER, x, z));
        return true;
    }

    private boolean radius(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }
        Optional<Integer> newRadius = Input.tryInteger(args[1]);
        if (!newRadius.isPresent()) {
            return false;
        }
        this.radius = newRadius.get();
        sender.sendMessage(String.format(FORMAT_RADIUS, radius));
        return true;
    }

    private boolean silent(CommandSender sender) {
        this.silent = !silent;
        sender.sendMessage(String.format(FORMAT_SILENT, silent ? "enabled" : "disabled"));
        return true;
    }

    private boolean quiet(CommandSender sender, String[] args) {
        Optional<Integer> newQuiet = Optional.empty();
        if (args.length > 1) {
            newQuiet = Input.tryInteger(args[1]);
        }
        if (!newQuiet.isPresent()) {
            return false;
        }
        this.quiet = newQuiet.get();
        sender.sendMessage(String.format(FORMAT_QUIET, quiet));
        return true;
    }

    public ConfigStorage getConfigStorage() {
        return configStorage;
    }

    public ConcurrentHashMap<World, GenTask> getGenTasks() {
        return genTasks;
    }

    public boolean isSilent() {
        return silent;
    }

    public int getQuiet() {
        return quiet;
    }
}
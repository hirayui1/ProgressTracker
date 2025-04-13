package org.example.progresstracker;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final AtomicInteger daysCount = new AtomicInteger(8); // Initial count

    public static void main(String[] args) {
        runBot();
    }

    private static void runBot() {
        try {
            JDA jda = JDABuilder.createDefault(Config.getDiscordToken()) // bot token is retrieved from env variable by config class
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT) // enables message content for the bot, allowing it to send and view messages. this is a JDA related necessity,
                    .build();                                       // we also had to enable permissions on discord through application portal.

            jda.awaitReady();   // no clue what this is but it was recommended

            sendRepeatedMessage(jda); // the daily message method, maybe a more suitable method name later
            jda.addEventListener(new EventListener()); // listens to user interaction events from the server

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendRepeatedMessage(JDA jda) {

        List<TextChannel> channels = jda.getTextChannelsByName("daily-achievement", true);

        TextChannel dailyAchievementChannel = channels.get(0);

        if (dailyAchievementChannel == null) {//If the channel does not exist, it throws the error
            throw new IllegalArgumentException("Channel not found!");
        }

        Runnable task = () -> dailyAchievementChannel.sendMessage("Day: "+daysCount.getAndIncrement()).queue();

        long initialDelay = getInitialDelay(8, 0); // Schedule at 8:00 AM
        long period = 24 * 60 * 60; // 24 hours in seconds

        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS); // Execute task after specific interval of time
    }

    private static long getInitialDelay(int hour, int minute) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(minute).withSecond(0);

        if (now.isAfter(nextRun)) {
            // If the time has already passed today, schedule for tomorrow
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).getSeconds();//returning the time difference between now vs the give time the daily message needs to be send
    }
}
package org.example.progresstracker;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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

            jda.upsertCommand(
                    Commands.slash("say", "Makes the bot say what you tell it to")
                            .addOption(STRING, "content", "What the bot should say", true) // Accepting a user input
            ).queue(); // adds the say command, the actual event once this is called is handled by EventListener onSlashCommandInteraction

            // listens to user interaction events from the server
            jda.addEventListener(new EventListener());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendRepeatedMessage(JDA jda) {
        // gets daily-achievement channel, but it only returns a list.
        // could do .getFirst() and store it in a TextChannel variable to make the method less verbose
        List<TextChannel> channels = jda.getTextChannelsByName("daily-achievement", true);

        if (channels.isEmpty()) {//Null safety check
            throw new IllegalArgumentException("Channel not found!");
        }

        TextChannel dailyAchievementChannel = channels.getFirst();

        if (dailyAchievementChannel == null) {//If the channel does not exist, it throws the error
            throw new IllegalArgumentException("Channel not found!");
        }

        Runnable task = () -> dailyAchievementChannel.sendMessage("Day: "+calculateDateDiff()).queue();

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

    private static long calculateDateDiff() {//using this function instead of using atomic integer and increment
        LocalDate startDate = LocalDate.parse("2025-03-15"); // start date of this bot
        return ChronoUnit.DAYS.between(startDate, LocalDateTime.now()); // calculates the days since startDate by retrieving today's date
    }
}
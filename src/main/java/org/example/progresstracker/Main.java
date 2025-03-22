package org.example.progresstracker;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main {
    public static void main(String[] args) {
        runBot();
    }

    private static void runBot() {
        try {
            JDA jda = JDABuilder.createDefault(Config.getDiscordToken()) // bot token is retrieved from env variable by config class
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT) // enables message content for the bot, allowing it to send and view messages. this is a JDA related necessity,
                    .build();                                       // we also had to enable permissions on discord through application portal.

            // no clue what this is but it was recommended
            jda.awaitReady();
            // the daily message method, maybe a more suitable method name later?
            sendRepeatedMessage(jda);

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


        List<Message> messages = MessageHistory.getHistoryFromBeginning(channels.getFirst()).complete().getRetrievedHistory(); // get a reversed list of message history in a channel

        // in order to avoid trapping the main thread
        new Thread(() -> {
            // runs as long as the bot is on
            while (true) {
                // accessing every message from a particular channel
                for (Message mess : messages) {
                    // fail-fast condition for if the message had already been sent today
                    if (mess.getContentDisplay().equals("Day " + calculateDateDiff())) {
                        try {
                            // make this process sleep for until 1h in order to avoid unlimited if checks
                            Thread.sleep(1000*60*60);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // check if the time is 8am, does not check the minute because we don't have a 24/7 server and I can't always be punctual irl.
                if (LocalDateTime.now().getHour() == 8) {
                    channels.getFirst().sendMessage("Day " + calculateDateDiff()).queue();
                    try {
                        // eventually fix busy-waiting
                        Thread.sleep(1000*60*60*24);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    private static long calculateDateDiff() {
        LocalDate startDate = LocalDate.parse("2025-03-15"); // start date of this bot

        return ChronoUnit.DAYS.between(startDate, LocalDateTime.now()); // calculates the days since startDate by retrieving today's date
    }
}
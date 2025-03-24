package org.example.progresstracker;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Utility {

    public static void runBot() {
        try {
            JDA jda = JDABuilder.createDefault(Config.getDiscordToken()) // bot token is retrieved from env variable by config class
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT) // enables message content for the bot, allowing it to send and view messages. this is a JDA related necessity,
                    .build();                                       // we also had to enable permissions on discord through application portal.

            jda.awaitReady();   // no clue what this is but it was recommended
            sendRepeatedMessage(jda); // the daily message method, maybe a more suitable method name later?

            jda.addEventListener(new EventListener()); // listens to user interaction events from the server
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static long calculateDateDiff() {
        LocalDate startDate = LocalDate.parse("2025-03-15"); // start date of this bot

        return ChronoUnit.DAYS.between(startDate, LocalDateTime.now()); // calculates the days since startDate by retrieving today's date
    }

    private static void sendRepeatedMessage(JDA jda) {
        List<TextChannel> channels = jda.getTextChannelsByName("daily-achievement", true); // gets daily-achievement channel, but it only returns a list.
        // could do .getFirst() and store it in a TextChannel variable to make the method less verbose

        List<Message> messages = MessageHistory.getHistoryFromBeginning(channels.getFirst()).complete().getRetrievedHistory(); // get a reversed list of message history in a channel


        new Thread(() -> { // in order to avoid trapping the main thread
            while (true) { // runs as long as the bot is on
                for (Message mess : messages) { // accessing every message from a particular channel
                    if (mess.getContentDisplay().equals("Day " + calculateDateDiff())) {  // fail-fast condition for if the message had already been sent today
                        try {
                            Thread.sleep(1000*60*60); // make this process sleep for until 1h in order to avoid unlimited if checks
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                if (LocalDateTime.now().getHour() == 8) { // check if the time is 8am, does not check the minute because we don't have a 24/7 server and I can't always be punctual irl.
                    channels.getFirst().sendMessage("Day " + calculateDateDiff()).queue();
                    try {
                        Thread.sleep(1000*60*60*24); // eventually fix busy-waiting
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
}

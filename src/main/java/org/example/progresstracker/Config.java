package org.example.progresstracker;

public class Config {
    private static final String discordToken = System.getenv("DBOT_TOKEN");

    public static String getDiscordToken() {
        return discordToken;
    }
}

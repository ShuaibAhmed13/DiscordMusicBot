package Bot;

import Player.MusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.internal.audio.VoiceCode;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

public class Bot extends ListenerAdapter {

    private final AudioPlayerManager audioPlayerManager;
    private final Map<Long, MusicManager> musicManagers;

    private Bot() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.musicManagers = new HashMap<>();
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    private synchronized MusicManager getMusicManager(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        MusicManager musicManager = this.musicManagers.get(guildId);
        if (musicManager == null) {
            musicManager = new MusicManager(this.audioPlayerManager);
            musicManagers.put(guildId, musicManager);
        }
        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
        return musicManager;
    }

    private void loadAndPlay(TextChannel textChannel, String url, VoiceChannel voiceChannel, SlashCommandInteractionEvent event) {
        MusicManager musicManager = getMusicManager(textChannel.getGuild());

        audioPlayerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                if (musicManager.audioPlayer.getPlayingTrack() == null) {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Playing " + audioTrack.getInfo().title)
                            .build()).queue();
                } else {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Queued  " + musicManager.audioPlayer.getPlayingTrack().getInfo().title)
                            .build()).queue();
                }
                play(textChannel.getGuild(), musicManager, audioTrack, voiceChannel);

            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {

            }

            @Override
            public void noMatches() {
                textChannel.sendMessage("Could not find " + url).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                textChannel.sendMessage("Could not play " + url).queue();
            }
        });
    }

    private void play(Guild guild, MusicManager musicManager, AudioTrack audioTrack, VoiceChannel voiceChannel) {
        connectToVoiceChannel(guild.getAudioManager(), voiceChannel);
        musicManager.scheduler.queue(audioTrack);
    }

    private void pause(MusicManager musicManager) {
        musicManager.scheduler.pause();
    }

    private void next(MusicManager musicManager) {
        musicManager.scheduler.nextTrack();
    }

    private void prev(MusicManager musicManager) {
        musicManager.scheduler.prevTrack();
    }

    private void stop(MusicManager musicManager, AudioManager audioManager) {
        musicManager.scheduler.stop();
        audioManager.closeAudioConnection();
    }

    private void volumeUp(MusicManager musicManager) {
        musicManager.scheduler.volumeUp();
    }

    private void volumeDown(MusicManager musicManager) {
        musicManager.scheduler.volumeDown();
    }

    private static void connectToVoiceChannel(AudioManager audioManager, VoiceChannel voiceChannel) {
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
        }
    }

    public static void main(String[] args) throws Exception {
        String token = args[0];
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("with your girls heart"))
                .addEventListeners(new Bot())
                .build();
        jda.updateCommands().addCommands(
                Commands.slash("play", "song you want to play")
                        .addOption(OptionType.STRING, "url", "link of song you want to play", true),
                Commands.slash("pause", "pause/play the song"),
                Commands.slash("next", "play the next song in queue"),
                Commands.slash("prev", "play the previous song in queue"),
                Commands.slash("queue-list", "get all of the songs in queue"),
                Commands.slash("controls", "display buttons to control the player")
        ).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        MusicManager musicManager = getMusicManager(event.getGuild());
        switch (event.getComponentId()) {
            case "pause":
                pause(musicManager);
                event.editButton(Button.primary("resume", "Resume")).queue();
                break;
            case "resume":
                pause(musicManager);
                event.editButton(Button.primary("pause", "Pause")).queue();
                break;
            case "next":
                next(musicManager);
                event.editMessageEmbeds(new EmbedBuilder().
                        setTitle(musicManager.audioPlayer.getPlayingTrack().getInfo().title)
                        .addField("Artist", musicManager.audioPlayer.getPlayingTrack().getInfo().author, true)
                        .addField("Duration", musicManager.audioPlayer.getPlayingTrack().getInfo().length / 1000 + " seconds", true)
                        .build()).queue();
//                event.reply("playing next").setEphemeral(true).queue();
                break;
            case "prev":
                prev(musicManager);
                event.editMessageEmbeds(new EmbedBuilder().
                        setTitle(musicManager.audioPlayer.getPlayingTrack().getInfo().title)
                        .addField("Artist", musicManager.audioPlayer.getPlayingTrack().getInfo().author, true)
                        .addField("Duration", musicManager.audioPlayer.getPlayingTrack().getInfo().length / 1000 + " seconds", true)
                        .build()).queue();
//                event.reply("playing previous").setEphemeral(true).queue();
                break;
            case "volume-up":
                volumeUp(musicManager);
                event.editButton(Button.primary("volume-up", "Up").withEmoji(Emoji.fromUnicode("U+1F50A"))).queue();
                break;
            case "volume-down":
                volumeDown(musicManager);
                event.editButton(Button.primary("volume-down", "Down").withEmoji(Emoji.fromUnicode("U+1F508"))).queue();
                break;
            case "exit":
                stop(musicManager, event.getGuild().getAudioManager());
                event.getMessage().delete().queue();
                break;
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        MusicManager musicManager = getMusicManager(event.getGuild());
        event.deferReply().queue();
        switch (event.getName()) {
            case "play":
                VoiceChannel vc = null;
                for (VoiceChannel voiceChannel : event.getGuild().getVoiceChannels()) {
                    if (voiceChannel.getMembers().contains(event.getMember())) {
                        vc = voiceChannel;
                        break;
                    }
                }
                if (vc == null) {
                    event.getHook().sendMessage("Must be in a voice channel!").queue();
                    break;
                }
                ;
                System.out.println(event.getOption("url").getAsString());
//                event.getHook().sendMessage("Playing").queue();
                loadAndPlay(event.getChannel().asTextChannel(), event.getOption("url").getAsString(),
                        vc, event);

                break;
            case "pause":
                pause(getMusicManager(event.getGuild()));
                event.getHook().sendMessage("paused").queue();
                break;
            case "next":
                next(getMusicManager(event.getGuild()));
                event.getHook().sendMessage("playing next").queue();
                break;
            case "prev":
                prev(getMusicManager(event.getGuild()));
                event.getHook().sendMessage("playing prev").queue();
                break;
            case "controls":
                if (musicManager.audioPlayer.getPlayingTrack() == null) {
                    event.getHook().sendMessage("Song must be playing to display controls").queue();
                    break;
                }
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                .setTitle(musicManager.audioPlayer.getPlayingTrack().getInfo().title,
                                        musicManager.audioPlayer.getPlayingTrack().getInfo().uri)
                                .addField("Artist",
                                        musicManager.audioPlayer.getPlayingTrack().getInfo().author, true)
                                .addField("Length", musicManager.audioPlayer.getPlayingTrack().getInfo().length / 1000 + " seconds", true)
                                .build()).addActionRow(

                                Button.primary("prev", "Prev"),
                                Button.primary("pause", "Pause"),
                                Button.primary("next", "Next")
                        )
                        .addActionRow(
                                Button.primary("volume-down", "Down").withEmoji(Emoji.fromUnicode("U+1F508")),
                                Button.primary("volume-up", "Up").withEmoji(Emoji.fromUnicode("U+1F50A")),
                                Button.danger("exit", "Exit")
                        ).queue();
//                event.getHook().sendMessage("Song Controls:").addActionRow(
//
//
//                ).queue();
        }
    }
}

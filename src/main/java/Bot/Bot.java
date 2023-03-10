package Bot;

import Player.Emitter;
import Player.MusicManager;
import Player.Scheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
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
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.css.RGBColor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Bot extends ListenerAdapter implements Emitter {

    private final AudioPlayerManager audioPlayerManager;
    private final Map<Long, MusicManager> musicManagers;
    private final Map<AudioPlayer, MessageChannel> audioPlayers;

    private final Map<MessageChannel, Message> controllerMessages;
    private MessageCreateBuilder currentControls;
    private Bot() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.musicManagers = new HashMap<>();
        this.audioPlayers = new HashMap<>();
        this.controllerMessages = new HashMap<>();
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    private synchronized MessageChannel getMessageChannel(AudioPlayer audioPlayer) {
        return this.audioPlayers.get(audioPlayer);
    }
    private synchronized MusicManager getMusicManager(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        MusicManager musicManager = this.musicManagers.get(guildId);
        if (musicManager == null) {
            musicManager = new MusicManager(this.audioPlayerManager);
            musicManagers.put(guildId, musicManager);
            musicManager.scheduler.addEmitListener(this);

        }
        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
        return musicManager;
    }

    private void loadAndPlay(TextChannel textChannel, String url, VoiceChannel voiceChannel, SlashCommandInteractionEvent event) {
        MusicManager musicManager = getMusicManager(textChannel.getGuild());

        audioPlayerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
//                MessageChannel messageChannel = getMessageChannel(musicManager.audioPlayer);
//                if(messageChannel != null && controllerMessages.get(messageChannel) != null)
//                    controllerMessages.get(getMessageChannel(musicManager.audioPlayer)).delete().queue();
//                controllerMessages.get(musicManager).delete();
                Long duration = audioTrack.getDuration() / 1000;
                String durationValue = duration >= 3600 ? Util.getDurationHMS(duration) : Util.getDurationMS(duration);
                if (musicManager.audioPlayer.getPlayingTrack() == null) {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Playing " + audioTrack.getInfo().title, audioTrack.getInfo().uri)
                                    .addField("Requested by", event.getMember().getAsMention(), true)
                                    .addField("Artist", audioTrack.getInfo().author, true)
                                    .addField("Duration", durationValue, true)
                            .build()).queue();
                } else {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Queued  " + audioTrack.getInfo().title, audioTrack.getInfo().uri)
                            .addField("Requested by", event.getMember().getAsMention(), true)
                            .addField("Artist", audioTrack.getInfo().author, true)
                            .addField("Duration", durationValue, true)
                            .build()).queue();
                    AudioTrack playingTrack = musicManager.audioPlayer.getPlayingTrack();
                    event.getMessageChannel().sendMessage(buildControls(playingTrack.getInfo())).queue((message -> {
//                        if(controllerMessages.get(event.getMessageChannel()) != null) {
//                            controllerMessages.get(event.getMessageChannel()).delete().queue();
//                            controllerMessages.put(event.getMessageChannel(), message);
//                        }
                        controllerMessages.put(event.getMessageChannel(), message);
//                        if(audioPlayers.get(musicManager.audioPlayer) != event.getMessageChannel()) {
//                            audioPlayers.put(musicManager.audioPlayer, event.getMessageChannel());
//                            controllerMessages.put(event.getMessageChannel(), message);
//                        }
                    }));
                }

                play(textChannel.getGuild(), musicManager, audioTrack, voiceChannel);

            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {

            }

            @Override
            public void noMatches() {
//                textChannel.sendMessage("Could not find " + url).queue();
                event.getHook().sendMessage("Could not find " + url).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
//                textChannel.sendMessage("Could not play " + url).queue();
                event.getHook().sendMessage("Could not play " + url).queue();
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

    private boolean next(MusicManager musicManager) {
        return musicManager.scheduler.playNext();
    }

    private boolean prev(MusicManager musicManager) {
        return musicManager.scheduler.prevTrack();
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
                Commands.slash("recently-played", "show recently played songs")
//                Commands.slash("clear", "Clear your queue and recently played")
//                Commands.slash("controls", "display buttons to control the player")
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
                if(!next(musicManager)) {
                    event.deferEdit().queue();
                    break;
                }
//                event.editMessageEmbeds(new EmbedBuilder().
//                        setTitle(musicManager.audioPlayer.getPlayingTrack().getInfo().title)
//                        .addField("Artist", musicManager.audioPlayer.getPlayingTrack().getInfo().author, true)
//                        .addField("Duration", musicManager.audioPlayer.getPlayingTrack().getInfo().length / 1000 + " seconds", true)
//                        .build()).queue();
//                event.reply("playing next").setEphemeral(true).queue();
                break;
            case "prev":
//                if(!musicManager.scheduler.getPrevStack().isEmpty()) {
//                    event.deferEdit().queue();
//                    break;
//                }
                if(!prev(musicManager)) {
                    event.deferEdit().queue();
                    break;
                }
//                event.editMessageEmbeds(new EmbedBuilder().
//                        setTitle(musicManager.audioPlayer.getPlayingTrack().getInfo().title)
//                        .addField("Artist", musicManager.audioPlayer.getPlayingTrack().getInfo().author, true)
//                        .addField("Duration", musicManager.audioPlayer.getPlayingTrack().getInfo().length / 1000 + " seconds", true)
//                        .build()).queue();
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
                event.getMessage().delete().submit();
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

                if(this.audioPlayers.get(musicManager.audioPlayer) != null) {
                    if(this.controllerMessages.get(this.audioPlayers.get(musicManager.audioPlayer)) != null) {
                        this.controllerMessages.get(this.audioPlayers.get(musicManager.audioPlayer)).delete().submit();
                    }
                    this.controllerMessages.remove(this.audioPlayers.get(musicManager.audioPlayer));

                    this.audioPlayers.remove(musicManager.audioPlayer);
                }
                this.audioPlayers.put(musicManager.audioPlayer, event.getMessageChannel());
                loadAndPlay(event.getChannel().asTextChannel(), event.getOption("url").getAsString(),
                        vc, event);
                System.out.println(event.getOption("url").getAsString());
//                event.getHook().sendMessage("Playing").queue();
//                if(this.audioPlayers.get(musicManager.audioPlayer) != null) {
//                    this.controllerMessages.get(this.audioPlayers.get(musicManager.audioPlayer)).delete().queue();
//                    this.controllerMessages.remove(this.audioPlayers.get(musicManager.audioPlayer));
//                    event.getHook().sendMessage(buildControls(musicManager.audioPlayer.getPlayingTrack().getInfo())).queue((message -> {
//                        this.controllerMessages.put(event.getMessageChannel(), message);
//                    }));
//                }



                break;
            case "pause":
                pause(musicManager);
                event.getHook().sendMessage("paused").queue();
                break;
            case "next":
                if(next(musicManager)) {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Playing " + musicManager.audioPlayer.getPlayingTrack().getInfo().title).build()).queue();
//                    event.getHook().sendMessage("Nothing to play!");
                } else {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Nothing to play!").setColor(new Color(150, 0, 30)).build()).queue();
                }
//                event.getHook().sendMessage("playing next").queue();
                break;
            case "prev":
                if(prev(musicManager)) {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Playing " + musicManager.audioPlayer.getPlayingTrack().getInfo().title).build()).queue();
//                    event.getHook().sendMessage("playing prev").queue();
                } else {
                    event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Nothing to play!").setColor(new Color(150, 0, 30)).build()).queue();
                }
                break;
            case "controls":
                break;
            case "queue-list":
                showQueue(event);
                break;
            case "recently-played":
                showRecentlyPlayed(event);
                break;
            case "clear":
                clearQueue(event);
                break;
//                event.getHook().sendMessage(getMusicManager(event.getGuild()).scheduler.getQueue().toString()).queue();
//                if (musicManager.audioPlayer.getPlayingTrack() == null) {
//                    event.getHook().sendMessage("Song must be playing to display controls").queue();
//                    break;
//                }
//                if(this.currentControls != null) break;

//                AudioTrackInfo info = musicManager.audioPlayer.getPlayingTrack().getInfo();
//                this.currentControls = MessageCreateBuilder.from(MessageCreateData.fromEmbeds(
//                        new EmbedBuilder()
//                                .setTitle(info.title, info.uri)
//                                .addField("Artist", info.author, true)
//                                .addField("Duration", info.length/1000 + " seconds", true)
//                                .build())).addActionRow(
//                                Button.primary("prev", "Prev"),
//                                Button.primary("pause", "Pause"),
//                                Button.primary("next", "Next")
//                        )
//                        .addActionRow(
//                                Button.primary("volume-down", "Down").withEmoji(Emoji.fromUnicode("U+1F508")),
//                                Button.primary("volume-up", "Up").withEmoji(Emoji.fromUnicode("U+1F50A")),
//                                Button.danger("exit", "Exit")
//                        );


//                this.messageChannel = event.getMessageChannel();
//                event.getHook().sendMessage(this.currentControls.build()).queue( (message -> {
//                    this.messageId = Long.valueOf(message.getId());
//                }));
//                event.getHook().sendMessage(MessageCreateData.fromEmbeds(
//                )).queue();
//                event.getHook().sendMessageEmbeds(new EmbedBuilder()
//                                .setTitle(musicManager.audioPlayer.getPlayingTrack().getInfo().title,
//                                        musicManager.audioPlayer.getPlayingTrack().getInfo().uri)
//                                .addField("Artist",
//                                        musicManager.audioPlayer.getPlayingTrack().getInfo().author, true)
//                                .addField("Length", musicManager.audioPlayer.getPlayingTrack().getInfo().length / 1000 + " seconds", true)
//                                .build()).addActionRow(
//                                Button.primary("prev", "Prev"),
//                                Button.primary("pause", "Pause"),
//                                Button.primary("next", "Next")
//                        )
//                        .addActionRow(
//                                Button.primary("volume-down", "Down").withEmoji(Emoji.fromUnicode("U+1F508")),
//                                Button.primary("volume-up", "Up").withEmoji(Emoji.fromUnicode("U+1F50A")),
//                                Button.danger("exit", "Exit")
//                        ).queue();

        }
    }

    private MessageCreateData buildControls(AudioTrackInfo info) {
        Long duration = info.length/1000;
        String durationValue = duration >= 3600 ? Util.getDurationHMS(duration) : Util.getDurationMS(duration);
        return MessageCreateBuilder.from(MessageCreateData.fromEmbeds(
                        new EmbedBuilder()
                                .setTitle(info.title, info.uri)
                                .addField("Artist", info.author, true)
                                .addField("Duration", durationValue, true)
                                .build())).addActionRow(
                        Button.primary("prev", "Prev"),
                        Button.primary("pause", "Pause"),
                        Button.primary("next", "Next")
                )
                .addActionRow(
                        Button.primary("volume-down", "Down").withEmoji(Emoji.fromUnicode("U+1F508")),
                        Button.primary("volume-up", "Up").withEmoji(Emoji.fromUnicode("U+1F50A")),
                        Button.danger("exit", "Exit")
                ).build();
    }

    public void showQueue(SlashCommandInteractionEvent event) {
        Scheduler scheduler = getMusicManager(event.getGuild()).scheduler;
        if(scheduler.getList().isEmpty() || scheduler.getIndex() == scheduler.getList().size()) {
            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Queue empty!").build()).queue();
            return;
        }
        List<AudioTrack> subList = scheduler.getList().subList(scheduler.getIndex() + 1, scheduler.getList().size());
        Iterator<AudioTrack> iterator = subList.iterator();
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Queue");
        int val = 1;
        while (iterator.hasNext()) {
            AudioTrack audioTrack = iterator.next();
            Long duration = audioTrack.getDuration() / 1000;
            String durationValue = duration >= 3600 ? Util.getDurationHMS(duration) : Util.getDurationMS(duration);
            embedBuilder
                    .addField("#", val++  + "",true)
                    .addField("Title", audioTrack.getInfo().title, true)
                    .addField("Duration", durationValue, true);
//                    .addBlankField(false);
        }
        if(subList.isEmpty()) embedBuilder = new EmbedBuilder().setTitle("Queue empty!");
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public void showRecentlyPlayed(SlashCommandInteractionEvent event) {
        Scheduler scheduler = getMusicManager(event.getGuild()).scheduler;
        System.out.println("LIST: " + scheduler.getList());
        System.out.println("\n\n\n");
        System.out.println("INDEX: " + scheduler.getIndex());
        List<AudioTrack> subList = scheduler.getList().subList(0, scheduler.getIndex());
        Collections.reverse(subList);
        Iterator<AudioTrack> iterator = subList.iterator();
        int val = subList.size();
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Recently Played");

        while(iterator.hasNext()) {
            AudioTrack audioTrack = iterator.next();
            Long duration = audioTrack.getDuration() / 1000;
            String durationValue = duration >= 3600 ? Util.getDurationHMS(duration) : Util.getDurationMS(duration);
            embedBuilder
                    .addField("#", val-- + "", true)
                    .addField("Title", audioTrack.getInfo().title, true)
                    .addField("Duration", durationValue, true);
        }
        if(subList.isEmpty()) embedBuilder.setTitle("Nothing recently played!");
        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public void clearQueue(SlashCommandInteractionEvent event) {
        MusicManager musicManager = getMusicManager(event.getGuild());
        musicManager.scheduler.clearQueue();
        event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Cleared!").build()).queue();
    }
    @Override
    public void trackStarted(AudioTrackInfo info, AudioPlayer audioPlayer) {
        MessageChannel messageChannel = this.audioPlayers.get(audioPlayer);
        if(messageChannel == null) return;
        messageChannel.sendMessage(buildControls(info)).queue((message -> {
            this.controllerMessages.put(audioPlayers.get(audioPlayer), message);
//            System.out.println(message.getId());
        }));
//        this.messageChannel.sendMessage(buildControls(info)).queue();
    }

    @Override
    public void trackEnded(AudioPlayer audioPlayer) {
        if(audioPlayers.get(audioPlayer) != null && controllerMessages.get(audioPlayers.get(audioPlayer)) != null)
            this.controllerMessages.get(audioPlayers.get(audioPlayer)).delete().queue();
    }

    @Override
    public void queueEnded(AudioPlayer audioPlayer) {
        this.audioPlayers.get(audioPlayer).sendMessageEmbeds(new EmbedBuilder().setTitle("Queue has ended").build()).queue();
//        this.messageChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Queue has ended").build()).queue();
    }
}

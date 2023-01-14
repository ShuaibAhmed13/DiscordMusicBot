package Player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.audio.AudioSendHandler;

public class MusicManager {

    public final AudioPlayer audioPlayer;
    public final Scheduler scheduler;

    public MusicManager(AudioPlayerManager manager) {
        audioPlayer = manager.createPlayer();
        scheduler = new Scheduler(audioPlayer);
        audioPlayer.addListener(scheduler);
    }

    public AudioSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(audioPlayer);
    }
}

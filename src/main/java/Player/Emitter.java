package Player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public interface Emitter {
    void trackStarted(AudioTrackInfo info, AudioPlayer audioPlayer);
    void trackEnded();
    void queueEnded(AudioPlayer audioPlayer);

}

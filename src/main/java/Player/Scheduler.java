package Player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class Scheduler implements AudioEventListener {

    private final AudioPlayer player;
    private final Queue<AudioTrack> queue;
    private final Stack<AudioTrack> prevQueue;
    public Scheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedList<>();
        this.prevQueue = new Stack<>();
    }

    @Override
    public void onEvent(AudioEvent audioEvent) {
        if(audioEvent instanceof TrackEndEvent){
//            prevQueue.push(audioEvent.player.getPlayingTrack());

            nextTrack();
        }
        if(audioEvent instanceof TrackStartEvent) {
            prevQueue.push(audioEvent.player.getPlayingTrack());
            System.out.println("Track ended");
            System.out.println("prev queue: " + prevQueue);
            System.out.println("queue: " + queue);
        }
    }
    public void pause() {
        if(this.player.isPaused()) {
            this.player.setPaused(false);
        } else {
            this.player.setPaused(true);
        }
    }

    public void nextTrack() {
//        prevQueue.offer(this.player.getPlayingTrack());
        this.player.startTrack(this.queue.poll(), false);
    }

    public void prevTrack() {
//        queue.offer(this.player.getPlayingTrack());
        this.player.startTrack(this.prevQueue.pop().makeClone(), true);
    }

    public void queue(AudioTrack track) {
        if(!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }
}

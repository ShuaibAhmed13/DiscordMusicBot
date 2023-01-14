package Player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.sound.midi.Track;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class Scheduler implements AudioEventListener {

    private final AudioPlayer player;
    private final Queue<AudioTrack> queue;
    private final Stack<AudioTrack> prevQueue;
    public Scheduler(AudioPlayer player) {
        this.player = player;
        this.player.setVolume(20);
        this.queue = new LinkedList<>();
        this.prevQueue = new Stack<>();
    }

    @Override
    public void onEvent(AudioEvent audioEvent) {
        if(audioEvent instanceof TrackEndEvent){
            //prevQueue.push(((TrackEndEvent) audioEvent).track);

            System.out.println("track ended");
            System.out.println("prev queue: " + prevQueue);
            System.out.println("queue: " + queue);
            nextTrack();
        }
        if(audioEvent instanceof TrackStartEvent) {
//            prevQueue.push(audioEvent.player.getPlayingTrack());
            System.out.println(audioEvent.player.getPlayingTrack().getInfo().title);
            System.out.println(audioEvent.player.getPlayingTrack().getDuration());
            System.out.println("Track started");
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
//        prevQueue.push(this.player.getPlayingTrack());
        if(this.queue.peek() != null) {
            this.player.startTrack(this.queue.poll().makeClone(), false);
            this.prevQueue.push(this.player.getPlayingTrack());
            System.out.println("next track if");
        }
    }

    public void prevTrack() {

        if(prevQueue.size() == 0) {
            System.out.println("Nothing to play");
            return;
        }

        Stack<AudioTrack> stack = new Stack<>();
        while(!queue.isEmpty()) {
            stack.push(queue.poll());
        }
//        if(!player.isPaused()) {
//            System.out.println("IS NOT PAUSED");
//            stack.push(player.getPlayingTrack());
//        }
        stack.push(prevQueue.pop());
        while (!stack.isEmpty()) {
            queue.offer(stack.pop());
        }
        nextTrack();

//        queue.offer(this.player.getPlayingTrack());

//        AudioTrack track = this.prevQueue.pop().makeClone();
//        if(!player.startTrack(track, true)) {
//            queue.offer(player.getPlayingTrack());
//        }
//        System.out.println(prevQueue.size() + " " + queue.size());
//        AudioTrack track = this.prevQueue.pop().makeClone();
//        this.queue.offer(track);
        //System.out.println(prevQueue.size() + " " + queue.size());


        //nextTrack();
        //this.player.startTrack(track, false);
    }

    public void volumeUp() {
        player.setVolume(player.getVolume() + 5);
    }
    public void stop() {
        player.stopTrack();
    }
    public void queue(AudioTrack track) {
        if(!player.startTrack(track, true)) {
            queue.offer(track);
        } else prevQueue.push(track);
    }

    public void volumeDown() {
        player.setVolume(player.getVolume() - 5);
    }
}

package Player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.*;

public class Scheduler implements AudioEventListener {
    private List<Emitter> emitterList = new ArrayList<>();
    private final AudioPlayer player;

//    private final Queue<AudioTrack> queue;
//    private final Stack<AudioTrack> prevStack;

    private final List<AudioTrack> list;
    private Integer index = 0;

    public Scheduler(AudioPlayer player) {
        this.player = player;
        this.player.setVolume(20);
        this.list = new ArrayList<>();
//        this.queue = new LinkedList<>();
//        this.prevStack = new Stack<>();
    }

    //    public AudioPlayer getPlayer() {
//        return this.player;
//    }
    public void addEmitListener(Emitter emitter) {
        this.emitterList.add(emitter);
    }

    @Override
    public void onEvent(AudioEvent audioEvent) {
        if (audioEvent instanceof TrackEndEvent) {
            //prevQueue.push(((TrackEndEvent) audioEvent).track);

//            index++;
//            this.prevStack.push(((TrackEndEvent) audioEvent).track);
//            System.out.println("track ended");
//            System.out.println("prev queue: " + this.prevStack);
//            System.out.println("queue: " + queue);
//            System.out.println();
//            System.out.println("TOP: " + ((TrackEndEvent) audioEvent).endReason);
//            System.out.println("\n\n\n");
//            if(((TrackEndEvent) audioEvent).endReason.equals("REPLACED")) {
//                System.out.println("REplacing...");
//                return;
//            }

            for (Emitter emitter : emitterList) {
//                System.out.println("Sending emits");
                emitter.trackEnded();
                if(index >= list.size()) emitter.queueEnded(this.player);
//                if (queue.isEmpty()) emitter.queueEnded(this.player);
            }
            System.out.println("name: " + ((TrackEndEvent) audioEvent).endReason.name());
            if(((TrackEndEvent) audioEvent).endReason.name().equals("STOPPED")) {
                System.out.println("STOPPING...");
                return;
            }
            index++;

            System.out.println("Track index: " + index);
            System.out.println(((TrackEndEvent) audioEvent).endReason);
            nextTrackAuto();
        }
        if (audioEvent instanceof TrackStartEvent) {
            if(this.player.getPlayingTrack() == null) return;
//            this.queue.offer(((TrackStartEvent) audioEvent).track.makeClone());
            for (Emitter emitter : emitterList) {
                emitter.trackStarted(this.player.getPlayingTrack().getInfo(), this.player);
            }
//            prevQueue.push(audioEvent.player.getPlayingTrack());
//            System.out.println(audioEvent.player.getPlayingTrack().getInfo().title);
//            System.out.println(audioEvent.player.getPlayingTrack().getDuration());
            System.out.println("Track started");
            System.out.println(list);
            System.out.println("Current index: " + index);
//            System.out.println("prev queue: " + prevStack);
//            System.out.println("queue: " + queue);
            System.out.println();

        }
    }

    public void pause() {
        if (this.player.isPaused()) {
            this.player.setPaused(false);
        } else {
            this.player.setPaused(true);
        }
    }

    public boolean playNext() {
        index++;
        this.player.stopTrack();
        if(index < this.list.size()) {
            this.player.startTrack(this.list.get(index).makeClone(), false);
            return true;
        }
        return false;
    }
    public boolean nextTrackAuto() {
//        prevQueue.push(this.player.getPlayingTrack());
//        if(this.player.getPlayingTrack() != null) {
////            this.prevStack.push(this.player.getPlayingTrack());
//            this.player.stopTrack();
//        }

        if(index < this.list.size()) {
            this.player.startTrack(this.list.get(index).makeClone(), false);
            return true;
        }
//        if (this.queue.peek() != null) {
//            this.player.startTrack(this.queue.poll().makeClone(), false);
//            return true;
////            System.out.println("next track if");
//        }
        return false;
    }

    public boolean prevTrack() {
        if(index == 0) {
            if(list.size() == 0) return false;
            this.player.stopTrack();
            if(this.player.getPlayingTrack() != null)
                this.player.startTrack(player.getPlayingTrack().makeClone(), false);;
        }
        System.out.println(this.list);
        System.out.println("index: " + index);

        this.player.stopTrack();
        if(index - 1 >= 0) index--;
        this.player.startTrack(this.list.get(index).makeClone(), false);
        System.out.println("index: " + index);
        return true;
//        System.out.println("In prev track");
//        if (prevStack.size() == 0) {
//            AudioTrack track = this.player.getPlayingTrack();
//            if(track != null) {
//                track = track.makeClone();
//                this.player.stopTrack();
//                this.player.startTrack(track, false);
//            }
//            System.out.println(queue);
////            if(this.player.startTrack(this.player.getPlayingTrack().makeClone(), false));
////            if(this.player.getPlayingTrack() != null) {
////                this.player.startTrack(this.player.getPlayingTrack().makeClone(), false);
////            }
//            System.out.println("Nothing to play");
//            return false;
//        }
//        System.out.println("This far");
//        Queue<AudioTrack> tempQueue = new LinkedList<>();
//        tempQueue.offer(prevStack.pop());
//        System.out.println(1 + " " + tempQueue);
//        if(this.player.getPlayingTrack() != null) {
//            tempQueue.offer(this.player.getPlayingTrack());
//        }
//        System.out.println(2 + " " + tempQueue);
//        while(!queue.isEmpty()) {
//            tempQueue.offer(queue.poll());
//        }
//        System.out.println(3 + " " + tempQueue);
//        System.out.println("Queue " + queue);
//        while(!tempQueue.isEmpty()) {
//            queue.offer(tempQueue.poll());
//        }
//        this.player.stopTrack();
//        this.player.startTrack(queue.poll().makeClone(), false);
//        return true;

//        Stack<AudioTrack> stack = new Stack<>();
//        while (!queue.isEmpty()) {
//            stack.push(queue.poll());
//        }
////        if(!player.isPaused()) {
////            System.out.println("IS NOT PAUSED");
////            stack.push(player.getPlayingTrack());
////        }
//        stack.push(prevQueue.pop());
//        while (!stack.isEmpty()) {
//            queue.offer(stack.pop());
//        }
//        nextTrack();
//        return true;
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
        list.add(track);
        if (!player.startTrack(track, true)) {
//            queue.offer(track);
//            list.add(track);
        }

//        else prevStack.push(track);
    }

    public void volumeDown() {
        player.setVolume(player.getVolume() - 5);
    }

    public List<AudioTrack> getList() {
        return this.list;
    }

//    public Queue<AudioTrack> getQueue() {
//        return this.queue;
//    }
//
//    public Stack<AudioTrack> getPrevStack() {
//        return this.prevStack;
//    }
}

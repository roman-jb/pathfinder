package v2;

import javax.swing.Timer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Demo playback state and scheduling, confined to the Swing event dispatch thread. */
final class DemoPlayback {
    private final IntSupplier stepDelay;
    private final IntSupplier cycleDelay;
    private final IntConsumer onChange;
    private final Runnable onNextCycle;
    private Timer timer;
    private int nodeCount;
    private int visibleNodes;
    private boolean active;
    private boolean paused;

    DemoPlayback(IntSupplier stepDelay, IntSupplier cycleDelay,
                 IntConsumer onChange, Runnable onNextCycle) {
        this.stepDelay = stepDelay;
        this.cycleDelay = cycleDelay;
        this.onChange = onChange;
        this.onNextCycle = onNextCycle;
    }

    void start(int nodeCount) {
        cancelTimer();
        this.nodeCount = nodeCount;
        visibleNodes = Math.min(1, nodeCount);
        active = true;
        onChange.accept(0);
        schedule();
    }

    void stop() {
        cancelTimer();
        active = false;
        paused = false;
        nodeCount = 0;
        visibleNodes = 0;
    }

    void togglePaused() {
        if (!active) return;
        paused = !paused;
        cancelTimer();
        onChange.accept(0);
        schedule();
    }

    void next() {
        if (!canNext()) return;
        visibleNodes++;
        onChange.accept(1);
    }

    void previous() {
        if (!canPrevious()) return;
        visibleNodes--;
        onChange.accept(-1);
    }

    boolean isActive() {
        return active;
    }

    boolean isPaused() {
        return paused;
    }

    int visibleNodes() {
        return visibleNodes;
    }

    boolean canNext() {
        return active && paused && visibleNodes < nodeCount;
    }

    boolean canPrevious() {
        return active && paused && visibleNodes > 1;
    }

    private void schedule() {
        if (!active || paused) return;
        int delay = visibleNodes < nodeCount ? stepDelay.getAsInt() : cycleDelay.getAsInt();
        timer = new Timer(delay, event -> {
            if (event.getSource() != timer || !active || paused) return;
            timer = null;
            if (visibleNodes < nodeCount) {
                visibleNodes++;
                onChange.accept(1);
                schedule();
            } else {
                onNextCycle.run();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}

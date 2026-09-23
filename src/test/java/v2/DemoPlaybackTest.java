package v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DemoPlaybackTest {
    private DemoPlayback playback;

    @AfterEach
    void stopPlayback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (playback != null) playback.stop();
        });
    }

    @Test
    void manualStepsRequirePauseAndStayWithinPath() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 60_000, () -> 60_000, direction -> {}, () -> {});
            assertFalse(playback.isActive());
            playback.togglePaused();
            assertFalse(playback.isPaused());

            playback.start(3);
            playback.next();
            playback.previous();
            assertEquals(1, playback.visibleNodes());
            assertFalse(playback.canNext());
            assertFalse(playback.canPrevious());

            playback.togglePaused();
            playback.previous();
            assertEquals(1, playback.visibleNodes());
            assertFalse(playback.canPrevious());
            assertTrue(playback.canNext());
            playback.next();
            assertEquals(2, playback.visibleNodes());
            playback.next();
            playback.next();
            assertEquals(3, playback.visibleNodes());
            assertFalse(playback.canNext());
            playback.previous();
            assertEquals(2, playback.visibleNodes());
            assertTrue(playback.isPaused());

            playback.togglePaused();
            playback.next();
            playback.previous();
            assertEquals(2, playback.visibleNodes());
        });
    }

    @Test
    void pauseCancelsPendingStepAndResumeContinuesFromRewoundNode() throws Exception {
        AtomicInteger delay = new AtomicInteger(0);
        CountDownLatch advanced = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(delay::get, () -> 60_000, direction -> {
                if (direction == 1 && !playback.isPaused()) {
                    playback.togglePaused();
                    advanced.countDown();
                }
            }, () -> fail("Unexpected next cycle"));
            playback.start(4);
            playback.togglePaused();
            playback.next();
            playback.next();
            playback.previous();
            assertEquals(2, playback.visibleNodes());
        });
        assertFalse(advanced.await(100, TimeUnit.MILLISECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, playback.visibleNodes());
            playback.togglePaused();
        });
        assertTrue(advanced.await(3, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(3, playback.visibleNodes());
            assertTrue(playback.isPaused());
        });
    }

    @Test
    void completedPathDoesNotCycleUntilResumed() throws Exception {
        CountDownLatch cycled = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 60_000, () -> 0, direction -> {}, cycled::countDown);
            playback.start(2);
            playback.togglePaused();
            playback.next();
            assertEquals(2, playback.visibleNodes());
            assertFalse(playback.canNext());
            assertTrue(playback.canPrevious());
        });
        assertFalse(cycled.await(100, TimeUnit.MILLISECONDS));
        SwingUtilities.invokeAndWait(() -> playback.togglePaused());
        assertTrue(cycled.await(3, TimeUnit.SECONDS));
    }

    @Test
    void pauseCancelsPendingCycleForSingleNodePath() throws Exception {
        CountDownLatch cycled = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 0, () -> 0, direction -> {}, cycled::countDown);
            playback.start(1);
            playback.togglePaused();
            playback.next();
            playback.previous();
            assertEquals(1, playback.visibleNodes());
            assertFalse(playback.canNext());
            assertFalse(playback.canPrevious());
        });
        assertFalse(cycled.await(100, TimeUnit.MILLISECONDS));
        SwingUtilities.invokeAndWait(() -> playback.togglePaused());
        assertTrue(cycled.await(3, TimeUnit.SECONDS));
    }

    @Test
    void replacingPathPreservesPauseAndStoppingResetsControls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 60_000, () -> 60_000, direction -> {}, () -> {});
            playback.start(3);
            playback.togglePaused();
            playback.next();
            playback.start(2);
            assertTrue(playback.isPaused());
            assertEquals(1, playback.visibleNodes());

            playback.stop();
            playback.next();
            playback.previous();
            playback.togglePaused();
            assertFalse(playback.isActive());
            assertFalse(playback.isPaused());
            assertFalse(playback.canNext());
            assertFalse(playback.canPrevious());
            assertEquals(0, playback.visibleNodes());

            playback.start(0);
            playback.togglePaused();
            playback.next();
            playback.previous();
            assertEquals(0, playback.visibleNodes());
        });
    }

    @Test
    void automaticPlaybackVisitsEveryNodeBeforeStartingNextCycle() throws Exception {
        List<Integer> visibleCounts = new ArrayList<>();
        List<Integer> directions = new ArrayList<>();
        CountDownLatch cycled = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 0, () -> 0, direction -> {
                visibleCounts.add(playback.visibleNodes());
                directions.add(direction);
            }, cycled::countDown);
            playback.start(4);
        });

        assertTrue(cycled.await(3, TimeUnit.SECONDS), "Playback should finish and start a new cycle");
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(List.of(1, 2, 3, 4), visibleCounts);
            assertEquals(List.of(0, 1, 1, 1), directions);
            assertFalse(playback.isPaused());
        });
    }

    @Test
    void onlyAcceptedManualStepsNotifyDisplayWithCorrectDirection() throws Exception {
        List<Integer> directions = new ArrayList<>();
        List<Integer> visibleCounts = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 60_000, () -> 60_000, direction -> {
                directions.add(direction);
                visibleCounts.add(playback.visibleNodes());
            }, () -> {});
            playback.start(3);
            playback.next();
            playback.previous();
            assertEquals(List.of(0), directions, "Playing must ignore manual steps");

            playback.togglePaused();
            directions.clear();
            visibleCounts.clear();
            playback.previous();
            playback.next();
            playback.next();
            playback.next();
            playback.previous();
            playback.previous();
            playback.previous();

            assertEquals(List.of(1, 1, -1, -1), directions);
            assertEquals(List.of(2, 3, 2, 1), visibleCounts);
            assertTrue(playback.isPaused());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void stoppingCancelsPendingStepsAndCycles(int nodeCount) throws Exception {
        CountDownLatch unexpectedCallback = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 0, () -> 0, direction -> {
                if (direction != 0) unexpectedCallback.countDown();
            }, unexpectedCallback::countDown);
            playback.start(nodeCount);
            playback.stop();
        });

        assertFalse(unexpectedCallback.await(100, TimeUnit.MILLISECONDS),
                "Stopped playback must not advance or start another cycle");
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, playback.visibleNodes());
            assertFalse(playback.isActive());
        });
    }

    @Test
    void resumingAfterRewindingCompletedPathAdvancesBeforeCycling() throws Exception {
        List<Integer> automaticCounts = new ArrayList<>();
        CountDownLatch cycled = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            playback = new DemoPlayback(() -> 0, () -> 0, direction -> {
                if (direction == 1 && !playback.isPaused()) {
                    automaticCounts.add(playback.visibleNodes());
                }
            }, cycled::countDown);
            playback.start(4);
            playback.togglePaused();
            playback.next();
            playback.next();
            playback.next();
            playback.previous();
            playback.previous();
            assertEquals(2, playback.visibleNodes());
            playback.togglePaused();
        });

        assertTrue(cycled.await(3, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> assertEquals(List.of(3, 4), automaticCounts));
    }
}

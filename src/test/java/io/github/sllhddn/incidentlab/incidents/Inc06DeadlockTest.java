package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sllhddn.incidentlab.incidents.inc06_deadlock.Account;
import io.github.sllhddn.incidentlab.incidents.inc06_deadlock.BuggyTransferService;
import io.github.sllhddn.incidentlab.incidents.inc06_deadlock.FixedTransferService;
import io.github.sllhddn.incidentlab.incidents.inc06_deadlock.TransferService;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class Inc06DeadlockTest {

    static final BigDecimal TEN = new BigDecimal("10.00");

    @Test
    void reproduce_oppositeTransfersDeadlock() throws Exception {
        var service = new BuggyTransferService();
        var a = new Account(1, new BigDecimal("1000.00"));
        var b = new Account(2, new BigDecimal("1000.00"));
        bothThreadsHoldTheirFirstLockBeforeContinuing(service, 5_000);

        Thread t1 = daemon("pay-a-to-b", () -> service.transfer(a, b, TEN));
        Thread t2 = daemon("pay-b-to-a", () -> service.transfer(b, a, TEN));
        t1.start();
        t2.start();

        // What jstack does: ask the JVM for threads stuck in a monitor cycle.
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] deadlocked = null;
        for (int i = 0; i < 100 && deadlocked == null; i++) {
            TimeUnit.MILLISECONDS.sleep(50);
            deadlocked = mx.findDeadlockedThreads();
        }

        assertThat(deadlocked).as("JVM-detected deadlock").isNotNull();
        assertThat(Arrays.stream(mx.getThreadInfo(deadlocked)).map(ThreadInfo::getThreadName))
                .contains("pay-a-to-b", "pay-b-to-a");
        // The daemon threads stay blocked; they cannot be interrupted out of a monitor wait.
    }

    @Test
    void fixed_globalLockOrder() throws Exception {
        var service = new FixedTransferService();
        var a = new Account(1, new BigDecimal("1000.00"));
        var b = new Account(2, new BigDecimal("1000.00"));
        bothThreadsHoldTheirFirstLockBeforeContinuing(service, 300);

        Thread t1 = daemon("pay-a-to-b", () -> service.transfer(a, b, TEN));
        Thread t2 = daemon("pay-b-to-a", () -> service.transfer(b, a, TEN));
        t1.start();
        t2.start();
        t1.join(5_000);
        t2.join(5_000);

        assertThat(t1.isAlive()).isFalse();
        assertThat(t2.isAlive()).isFalse();
        assertThat(a.balance()).isEqualByComparingTo("1000.00");
        assertThat(b.balance()).isEqualByComparingTo("1000.00");
    }

    /**
     * Forces the bad interleaving: each thread waits after its first lock until the other
     * has one too. With the fix, the second thread cannot get its first lock, so the
     * barrier times out and the first thread simply carries on.
     */
    private static void bothThreadsHoldTheirFirstLockBeforeContinuing(TransferService service, long waitMs) {
        CyclicBarrier barrier = new CyclicBarrier(2);
        service.setAfterFirstLock(() -> {
            try {
                barrier.await(waitMs, TimeUnit.MILLISECONDS);
            } catch (Exception expectedWithTheFix) {
                // timed out or broken: continue
            }
        });
    }

    private static Thread daemon(String name, Runnable r) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }
}

package server.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class RuntimeStats {

    public static final class Snapshot {
        public final long startMillis;
        public final long nowMillis;
        public final long totalConnections;
        public final int uniqueConnections;
        public final long getUpdateCalls;
        public final long authUserCalls;
        public final long getXKECCalls;
        public final long getXOSCCalls;
        public final long getPresenceCalls;
        public final long getTokenCalls;
        public final long getEngineCalls;
        public final long getNoKVCalls;

        Snapshot(long startMillis, long nowMillis,
                 long totalConnections, int uniqueConnections,
                 long getUpdateCalls, long authUserCalls,
                 long getXKECCalls, long getXOSCCalls,
                 long getPresenceCalls, long getTokenCalls,
                 long getEngineCalls, long getNoKVCalls) {
            this.startMillis = startMillis;
            this.nowMillis = nowMillis;
            this.totalConnections = totalConnections;
            this.uniqueConnections = uniqueConnections;
            this.getUpdateCalls = getUpdateCalls;
            this.authUserCalls = authUserCalls;
            this.getXKECCalls = getXKECCalls;
            this.getXOSCCalls = getXOSCCalls;
            this.getPresenceCalls = getPresenceCalls;
            this.getTokenCalls = getTokenCalls;
            this.getEngineCalls = getEngineCalls;
            this.getNoKVCalls = getNoKVCalls;
        }
    }

    private volatile long startMillis = 0L;
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final Set<String> uniq = Collections.synchronizedSet(new HashSet<>());
    private final AtomicLong getUpdateCalls = new AtomicLong(0);
    private final AtomicLong authUserCalls = new AtomicLong(0);
    private final AtomicLong getXKECCalls = new AtomicLong(0);
    private final AtomicLong getXOSCCalls = new AtomicLong(0);
    private final AtomicLong getPresenceCalls = new AtomicLong(0);
    private final AtomicLong getTokenCalls = new AtomicLong(0);
    private final AtomicLong getEngineCalls = new AtomicLong(0);
    private final AtomicLong getNoKVCalls = new AtomicLong(0);

    public void resetAndStart() {
        startMillis = System.currentTimeMillis();
        totalConnections.set(0);
        uniq.clear();
        getUpdateCalls.set(0);
        authUserCalls.set(0);
        getXKECCalls.set(0);
        getXOSCCalls.set(0);
        getPresenceCalls.set(0);
        getTokenCalls.set(0);
        getEngineCalls.set(0);
        getNoKVCalls.set(0);
    }

    public void recordConnection(String ip) {
        totalConnections.incrementAndGet();
        if (ip != null && !ip.isEmpty()) uniq.add(ip);
    }

    public void incGetUpdate() {
        getUpdateCalls.incrementAndGet();
    }

    public void incAuthUser() {
        authUserCalls.incrementAndGet();
    }

    public void incGetXKEC() {
        getXKECCalls.incrementAndGet();
    }

    public void incGetXOSC() {
        getXOSCCalls.incrementAndGet();
    }

    public void incGetPresence() {
        getPresenceCalls.incrementAndGet();
    }

    public void incGetToken() {
        getTokenCalls.incrementAndGet();
    }

    public void incGetEngine() {
        getEngineCalls.incrementAndGet();
    }

    public void incGetNoKV() {
        getNoKVCalls.incrementAndGet();
    }

    public Snapshot snapshot() {
        long now = System.currentTimeMillis();
        int uniqCount;
        synchronized (uniq) {
            uniqCount = uniq.size();
        }
        return new Snapshot(
                startMillis, now,
                totalConnections.get(), uniqCount,
                getUpdateCalls.get(),
                authUserCalls.get(),
                getXKECCalls.get(),
                getXOSCCalls.get(),
                getPresenceCalls.get(),
                getTokenCalls.get(),
                getEngineCalls.get(),
                getNoKVCalls.get()
        );
    }
}

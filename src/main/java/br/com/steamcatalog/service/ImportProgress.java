package br.com.steamcatalog.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estado compartilhado da importacao, atualizado pela thread de importacao e
 * lido pela thread do menu (por isso usa tipos atomicos / thread-safe).
 */
public class ImportProgress {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger totalRead = new AtomicInteger(0);
    private final AtomicInteger inserted = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final AtomicInteger developersCreated = new AtomicInteger(0);
    private final AtomicInteger publishersCreated = new AtomicInteger(0);
    private volatile String lastMessage = "Importacao ainda nao executada.";
    private volatile long startedAt = 0;
    private volatile long finishedAt = 0;

    public boolean isRunning() {
        return running.get();
    }

    public boolean start() {
        boolean ok = running.compareAndSet(false, true);
        if (ok) {
            totalRead.set(0);
            inserted.set(0);
            skipped.set(0);
            developersCreated.set(0);
            publishersCreated.set(0);
            finishedAt = 0;
            startedAt = System.currentTimeMillis();
            lastMessage = "Importacao em andamento...";
        }
        return ok;
    }

    public void finish(String message) {
        finishedAt = System.currentTimeMillis();
        lastMessage = message;
        running.set(false);
    }

    public void incRead() {
        totalRead.incrementAndGet();
    }

    public void incInserted() {
        inserted.incrementAndGet();
    }

    public void incSkipped() {
        skipped.incrementAndGet();
    }

    public void incDevelopersCreated() {
        developersCreated.incrementAndGet();
    }

    public void incPublishersCreated() {
        publishersCreated.incrementAndGet();
    }

    public int getTotalRead() {
        return totalRead.get();
    }

    public int getInserted() {
        return inserted.get();
    }

    public int getSkipped() {
        return skipped.get();
    }

    public int getDevelopersCreated() {
        return developersCreated.get();
    }

    public int getPublishersCreated() {
        return publishersCreated.get();
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String snapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append(running.get() ? "[EM ANDAMENTO] " : "[OCIOSO] ");
        sb.append(lastMessage).append('\n');
        sb.append(String.format(
                "  lidos=%d | jogos inseridos=%d | ignorados=%d | developers novos=%d | publishers novos=%d",
                totalRead.get(), inserted.get(), skipped.get(),
                developersCreated.get(), publishersCreated.get()));
        if (finishedAt > 0 && startedAt > 0) {
            sb.append(String.format("%n  tempo total: %.1fs",
                    (finishedAt - startedAt) / 1000.0));
        }
        return sb.toString();
    }
}

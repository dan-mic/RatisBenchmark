package de.hhu.bsinfo.ratisbenchmark.benchmark.throughput;

import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkStateMachine;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.apache.ratis.util.AutoCloseableLock;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThroughputStateMachine extends BenchmarkStateMachine {

    private byte[] content;
    private int writeCounter = 0;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static final String GET_COMMAND = "GET";

    private AutoCloseableLock readLock() {
        return AutoCloseableLock.acquire(lock.readLock());
    }

    private AutoCloseableLock writeLock() {
        return AutoCloseableLock.acquire(lock.writeLock());
    }

    @Override
    public CompletableFuture<Message> query(Message request) {
        String msg = request.getContent().toString(Charset.defaultCharset());
        if (!msg.equals(GET_COMMAND)) {
            return super.query(request);
        }
        // Gebe nur Anzahl der Schreibzugriffe und Länge der Variable content zurück, da diese zur Kontrolle genügen
        // und die Belastung des Netzwerks durch das Versenden dieser Werte von keiner großen Bedeutung ist.
        try(AutoCloseableLock readLock = readLock()) {
            return CompletableFuture.completedFuture(Message.valueOf(writeCounter + ", " + content.length));
        }
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final RaftProtos.LogEntryProto entry = trx.getLogEntry();
        ByteString logData = entry.getStateMachineLogEntry().getLogData();

        // Kurze Nachrichten betreffen nicht diesen Benchmark und werden von der Oberklasse bearbeitet.
         if (logData.size() < 100) {
            final CompletableFuture<Message> f = super.applyTransaction(trx);
            if (super.resetNecessary())
                reset();
            return f;
        }

        final long index = entry.getIndex();
        updateLastAppliedTermIndex(entry.getTerm(), index);

        try(AutoCloseableLock writeLock = writeLock()) {
            content = new byte[logData.size()];
            logData.copyTo(content, 0);
            writeCounter++;
        }

        final CompletableFuture<Message> f =
                CompletableFuture.completedFuture(Message.valueOf("OK"));

        return f;
    }

    @Override
    public void reset(){
        super.reset();
        try(AutoCloseableLock writeLock = writeLock()) {
            content = null;
            writeCounter = 0;
        }
    }
}

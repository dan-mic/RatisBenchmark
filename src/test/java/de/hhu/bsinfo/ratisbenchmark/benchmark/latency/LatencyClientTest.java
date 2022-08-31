package de.hhu.bsinfo.ratisbenchmark.benchmark.latency;

import de.hhu.bsinfo.ratisbenchmark.benchmark.TestRaftGroup;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatencyClientTest {

    // Tests deaktiviert, da zum Bestehen Server laufen müssen
    @Disabled
    @Test
    void testSend() {
        String resultsFileName = "/tmp/ratis/latencies.txt";
        LatencyClient client = new LatencyClient(TestRaftGroup.getTestRaftGroup(), resultsFileName);
        RaftClientReply reply = null;
        try {
            for (int i = 0; i < 10; i++)
                reply = client.send(Message.valueOf("INCREMENT"));
        }
        catch (IOException ignored){}
        assertEquals("10", reply.getMessage().getContent().toString(Charset.defaultCharset()));
    }

    @Disabled
    @Test
    void testSendReadOnly(){
        String resultsFileName = "/tmp/ratis/latencies.txt";
        LatencyClient client = new LatencyClient(TestRaftGroup.getTestRaftGroup(), resultsFileName);
        RaftClientReply reply = null;
        int startValue = 0;
        try {
            reply = client.sendReadOnly(Message.valueOf("GET"));
            startValue = Integer.parseInt(reply.getMessage().getContent().toString(Charset.defaultCharset()));
            for (int i = 0; i < 10; i++)
                client.send(Message.valueOf("INCREMENT"));
            reply = client.sendReadOnly(Message.valueOf("GET"));
        }
        catch (Exception ignored){}
        assertEquals(String.valueOf(startValue + 10), reply.getMessage().getContent().toString(Charset.defaultCharset()));
    }
}
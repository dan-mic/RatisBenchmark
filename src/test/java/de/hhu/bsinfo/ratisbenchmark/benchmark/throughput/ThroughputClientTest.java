package de.hhu.bsinfo.ratisbenchmark.benchmark.throughput;

import de.hhu.bsinfo.ratisbenchmark.benchmark.TestRaftGroup;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThroughputClientTest {

    // Tests deaktiviert, da zum Bestehen Server laufen müssen
    @Disabled
    @Test
    void testSend() {
        ThroughputClient client = new ThroughputClient(TestRaftGroup.getTestRaftGroup());
        int messageSize = new Random().nextInt(100) + 100;
        ByteString byteString = getRandomByteString(messageSize);
        RaftClientReply reply = null;
        try {
            reply = client.send(Message.valueOf(byteString));
        }
        catch (IOException ignored){}
        assertEquals("OK", reply.getMessage().getContent().toString(Charset.defaultCharset()));
    }

    @Disabled
    @Test
    void testSendReadOnly() {
        ThroughputClient client = new ThroughputClient(TestRaftGroup.getTestRaftGroup());
        int messageSize = new Random().nextInt(100) + 100;
        ByteString byteString = getRandomByteString(messageSize);
        RaftClientReply reply = null;
        try {
            client.send(Message.valueOf(byteString));
            reply = client.sendReadOnly(Message.valueOf("GET"));
        }
        catch (IOException ignored){}
        String replyString = reply.getMessage().getContent().toString(Charset.defaultCharset());
        int length = Integer.parseInt(replyString.split(", ")[1]);
        assertEquals(messageSize, length);
    }

    private static ByteString getRandomByteString(int size){
        byte[] content = new byte[size];
        new Random().nextBytes(content);
        content[size - 1] = Byte.MAX_VALUE;
        return ByteString.copyFrom(content);
    }
}
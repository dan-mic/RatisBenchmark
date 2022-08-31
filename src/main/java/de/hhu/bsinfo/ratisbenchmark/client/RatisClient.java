package de.hhu.bsinfo.ratisbenchmark.client;

import org.apache.ratis.RaftConfigKeys;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.netty.NettyFactory;
import org.apache.ratis.protocol.*;
import org.apache.ratis.rpc.SupportedRpcType;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public abstract class RatisClient {

    private final RaftClient raftClient;

    public RatisClient(RaftGroup raftGroup){
        RaftProperties raftProperties = new RaftProperties();

        RaftConfigKeys.Rpc.setType(raftProperties, SupportedRpcType.NETTY);

        RaftClient.Builder builder = RaftClient.newBuilder()
                .setProperties(raftProperties)
                .setRaftGroup(raftGroup)
                .setClientRpc(
                        new NettyFactory(new Parameters())
                                .newRaftClientRpc(ClientId.randomId(), raftProperties));

        this.raftClient = builder.build();
    }

    public RaftClientReply send(Message message) throws IOException{
        return raftClient.io().send(message);
    }

    public RaftClientReply sendReadOnly(Message message) throws IOException {
        return raftClient.io().sendReadOnly(message);
    }

    public void close() throws IOException {
        raftClient.close();
    }
}

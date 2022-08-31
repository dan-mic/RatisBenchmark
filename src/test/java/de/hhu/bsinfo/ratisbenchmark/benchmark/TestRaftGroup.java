package de.hhu.bsinfo.ratisbenchmark.benchmark;

import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestRaftGroup {

    public static RaftGroup getTestRaftGroup(){
        final List<RaftPeer> peers = createRaftPeers(new String[]{"127.0.0.1:6000", "127.0.0.1:6001, 127.0.0.1:6002"});
        final UUID CLUSTER_GROUP_ID = UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1");
        return RaftGroup.valueOf(RaftGroupId.valueOf(CLUSTER_GROUP_ID), peers);
    }

    private static List<RaftPeer> createRaftPeers(String[] addresses){
        List<RaftPeer> peers = new ArrayList<>(addresses.length);
        for (int i = 0; i < addresses.length; i++) {
            peers.add(RaftPeer.newBuilder().setId("n" + i).setAddress(addresses[i]).build());
        }
        return Collections.unmodifiableList(peers);
    }
}

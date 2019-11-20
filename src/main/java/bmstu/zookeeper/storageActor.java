package bmstu.zookeeper;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.List;

public class storageActor extends AbstractActor {
    List<String> serversPortList;

    @Override
    public Receive createReceive(){
        return ReceiveBuilder.create().match(ServerMSG.class, msg -> {
            serversPortList = msg.getServerPort();
        }).build();
    }
}

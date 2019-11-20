package bmstu.zookeeper;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;


import java.util.List;
import java.util.Random;


public class StorageActor extends AbstractActor {
    List<String> serversPortList;

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().match(
                ServerMSG.class,
                msg -> serversPortList = msg.getServerPort())
                .match(
                        GetRandomPort.class,
                        msg -> {
                            Random rand = new Random();
                            int len = serversPortList.size();
                            int rand_idx = rand.nextInt(len);
                            while (serversPortList.get(rand_idx).equals(msg.getRandomPort())) {
                                rand_idx = rand.nextInt(len);
                            }
                            getSender().tell(Integer.parseInt(serversPortList.get(rand_idx)), ActorRef.noSender());
                        })
                .build();
    }
}

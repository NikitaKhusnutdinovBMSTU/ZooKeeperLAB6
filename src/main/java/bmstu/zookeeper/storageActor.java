package bmstu.zookeeper;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

public class storageActor extends AbstractActor {

    @Override
    public Receive createReceive(){
        return ReceiveBuilder.create().match().build();
    }
}

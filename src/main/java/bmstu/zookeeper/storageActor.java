package bmstu.zookeeper;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import javafx.scene.SubScene;

import java.util.List;

public class storageActor extends AbstractActor {
    List<String> serversPortList;

    @Override
    public Receive createReceive(){
        return ReceiveBuilder.create().match(ServerMSG.class, msg -> {
            serversPortList = msg.getServerPort();
            for(String s : serversPortList){
                System.out.println("Port -> " + s);
            }
            if (serversPortList.size() == 0){
                System.out.println("ZERO SERVERS");
            }
        }).build();
    }
}

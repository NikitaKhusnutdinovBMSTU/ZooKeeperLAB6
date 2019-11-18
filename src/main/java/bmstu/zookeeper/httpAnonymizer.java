package bmstu.zookeeper;


import org.apache.zookeeper.*;

import java.io.IOException;

public class httpAnonymizer {

    public void main(String[] args) throws IOException, KeeperException, InterruptedException {
        
        ZooKeeper zoo = new ZooKeeper(
                "127.0.0.1:2181",
                2000,
                event -> {
                    System.out.println("MAY BE IT WORKS");
                }
        );

        zoo.create(
                "/servers/s",
                "data".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
    }
}

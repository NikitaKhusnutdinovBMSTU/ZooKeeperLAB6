package bmstu.zookeeper;


import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;

public class httpAnonymizer {
    private final static String SERVER_NUMBER_1 = "127.0.0.1:2181";
    private final static String SERVER_NUMBER_2 = "127.0.0.1:2182";
    private final static String SERVER_NUMBER_3 = "127.0.0.1:2183";
    private final static String SERVER_NUMBER_4 = "127.0.0.1:2184";
    private final static String SERVER_NUMBER_5 = "127.0.0.1:2185";


    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {

        ZooKeeper zoo = new ZooKeeper(
                "127.0.0.1:2181",
                2000,
                event -> {
                    System.out.println("MAY BE IT WORKS");
                }
        );

        zoo.create(
                "/s",
                "data".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
        List<String> servers = zoo.getChildren("/", a->{});
        for(String s : servers){
            System.out.println(s);
        }
        //zoo.delete("/s", 4);
    }
}

package bmstu.zookeeper;


import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class httpAnonymizer {

    public void main(String[] args) throws IOException {
        Watcher watcher = event -> {
            
        };
        ZooKeeper zoo = new ZooKeeper("127.0.0.1:2181", 2000, watcher);
    }
}

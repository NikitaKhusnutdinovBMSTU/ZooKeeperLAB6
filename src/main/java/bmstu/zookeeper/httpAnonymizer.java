package bmstu.zookeeper;


public class httpAnonymizer {
    public void main(String[] args) {

        ZooKeeper zoo = new ZooKeeper("127.0.0.1:2181", 2000, this);
    }

}

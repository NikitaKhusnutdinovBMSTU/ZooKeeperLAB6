package bmstu.zookeeper;

public class GetRandomPort {
    private String randomPort;

    public GetRandomPort(String port){
         this.randomPort = port;
     }

    public String getRandomPort() {
        return this.randomPort;
    }
}

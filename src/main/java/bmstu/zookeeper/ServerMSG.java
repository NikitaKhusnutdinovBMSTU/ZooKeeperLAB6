package bmstu.zookeeper;

import java.util.List;

public class ServerMSG {
    private List<String> serversList;

    public ServerMSG(List<String> port){
        this.serversList = port;
    }

    public List<String> getServerPort() {
        return this.serversList;
    }
}

package bmstu.zookeeper;


import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class httpAnonymize extends AllDirectives {

    private static ZooKeeper zoo;
    private static final String ROUTES = "routes";
    private static final String LOCALHOST = "localhost";
    private static final String SERVER_INFO = "Server online on localhost:8080/\n PRESS ANY KEY TO STOP";
    private static int PORT = 8080;
    private static final String URL = "url";
    private static final String COUNT = "count";
    private static final int TIMEOUT_MILLIS = 5000;


    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {

        zoo = new ZooKeeper(
                "127.0.0.1:2181",
                2000,
                new Watcher() {
                    @Override
                    public void process(WatchedEvent event) {
                        if (event.getType() == Event.EventType.NodeChildrenChanged) {
                            System.out.println("NODE WAS CREATED");
                        }
                        process(event);
                    }
                }
        );
        ActorSystem system = ActorSystem.create(ROUTES);


        zoo.exists("/servers", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("event_worked_again");
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                    List<String> servers = zoo.getChildren("/servers", true);
                    for(String s: servers){
                        byte[] data = zoo.getData("/servers/" + s, true, Stat.);
                        System.out.println("[Server : " + s + ", data :" + zoo.getData("/servers" + s, true));
                    }
                }
            }
        });

        Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        httpAnonymize app = new httpAnonymize();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.route().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(LOCALHOST, PORT),
                materializer
        );

        System.out.println(SERVER_INFO);
        System.in.read();

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());

    }

    private Route route() {
        return concat(
                get(
                        () -> parameter(URL, url ->
                                parameter(COUNT, count -> {
                                            int parsedCount = Integer.parseInt(count);
                                            try {
                                                zoo.getChildren("/servers", new Watcher() {
                                                    @Override
                                                    public void process(WatchedEvent event) {
                                                        System.out.println("event_worked_again");
                                                        if (event.getType() == Event.EventType.NodeChildrenChanged) {
                                                            System.out.println("New children in the crew ->" + event.getPath());
                                                        }
                                                    }
                                                });
                                                System.out.println("ya tut bil");
                                            } catch (KeeperException e) {
                                                e.printStackTrace();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            return complete("(" + Integer.toString(parsedCount) + ")");
                                        }
                                )
                        )
                )
        );
    }
}

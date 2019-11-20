package bmstu.zookeeper;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.apache.zookeeper.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class HTTPServerAkka extends AllDirectives {
    private static int port;
    private static ActorRef storageActor;
    private static ZooKeeper zoo;
    private static CountDownLatch connSignal = new CountDownLatch(0);
    private static Http http;
    private static final String ROUTES = "routes";
    private static final String LOCALHOST = "localhost";
    private static final String SERVER_INFO = "Server online on localhost:8080/\n PRESS ANY KEY TO STOP";
    private static final String URL = "url";
    private static final String COUNT = "count";
    private static final int TIMEOUT_MILLIS = 5000;

    public static void main(String[] args) throws Exception {

        Scanner in = new Scanner(System.in);
        int PORT = in.nextInt();
        ActorSystem system = ActorSystem.create(ROUTES);

        createZoo(PORT);
        port = PORT;

        http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        storageActor = system.actorOf(Props.create(storageActor.class));

        HTTPServerAkka app = new HTTPServerAkka();

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


    private static void createZoo(int port) throws IOException, KeeperException, InterruptedException {
        zoo = new ZooKeeper(
                "127.0.0.1:2181",
                2000,
                a -> {}
        );
        zoo.create(
                "/servers/" + Integer.toString(port),
                Integer.toString(port).getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );

        zoo.getChildren("/servers", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                    List<String> servers = null;
                    try {
                        servers = zoo.getChildren("/servers", true);
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    List<String> serversData = new ArrayList<>();
                    for(String s: servers){
                        byte[] data = new byte[0];
                        try {
                            data = zoo.getData("/servers/" + s, false, null);
                        } catch (KeeperException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        serversData.add(new String(data));
                    }
                    storageActor.tell(new ServerMSG(serversData), ActorRef.noSender());
                }
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                process(event);
            }
        });
    }

    CompletionStage<HttpResponse> fetchToServer(int port, String url, int parsedCount) {
        try {
            return http.singleRequest(
                    HttpRequest.create("http://localhost:" + Integer.toString(port) + "/?url=" + url + "&count=" +
                            Integer.toString(parsedCount - 1)));
        }catch(Exception e){
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity("404"));
        }
    }

    CompletionStage<HttpResponse> fetch(String url) {
        try {
            return http.singleRequest(
                    HttpRequest.create(url));
        }catch(Exception e){
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity("404"));
        }
    }


    private Route route() {
        return concat(
                get(
                        () -> parameter(URL, url ->
                                parameter(COUNT, count -> {
                                    int parsedCount = Integer.parseInt(count);
                                            System.out.println("WAS SENDED FROM " + Integer.toString(port) + "COUNT ->" + count);
                                            if (parsedCount != 0) {
                                                try {
                                                    Future<Object> new_port = CompletableFuture.completedFuture(Patterns.ask(storageActor, new GetRandomPort(Integer.toString(port)), 5000));
                                                    return complete(fetchToServer(Integer.parseInt(new_port.get()), url, parsedCount).toCompletableFuture().get());
                                                } catch (InterruptedExceptio | ExecutionException e) {
                                                    e.printStackTrace();
                                                    retur
                                                }
                                            }
                                            try {
                                                return complete(fetch(url).toCompletableFuture().get());
                                            } catch (InterruptedException | ExecutionException e) {
                                                e.printStackTrace();
                                                return complete(")");
                                            }
                                        }
                                )
                        )
                )
        );
    }
}

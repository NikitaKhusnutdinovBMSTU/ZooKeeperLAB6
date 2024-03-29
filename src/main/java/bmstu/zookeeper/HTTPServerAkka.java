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
    private static Http http;
    private static final String ROUTES = "routes";
    private static final String LOCALHOST = "localhost";
    private static final String SERVER_INFO = "Server online on localhost:";
    private static final String URL = "url";
    private static final String COUNT = "count";
    private static final String ZOO_KEEPER_HOST = "127.0.0.1:2181";
    private static final String ZOO_KEEPER_SERVER_DIR = "/servers";
    private static final String ZOO_KEEPER_CHILD_DIR = "/servers/";
    private static final String NOT_FOUND = "404";
    private static final String URL_ERROR_MESSAGE = "Unable to connect to url";
    private static final int TIMEOUT_MILLIS = 5000;

    public static void main(String[] args) throws Exception {

        Scanner in = new Scanner(System.in);
        port = in.nextInt();

        ActorSystem system = ActorSystem.create(ROUTES);
        storageActor = system.actorOf(Props.create(StorageActor.class));
        createZoo();
        http = Http.get(system);

        final ActorMaterializer materializer = ActorMaterializer.create(system);
        HTTPServerAkka app = new HTTPServerAkka();
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.route().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(LOCALHOST, port),
                materializer
        );

        System.out.println(SERVER_INFO + Integer.toString(port));
        System.in.read();

        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());

    }

    public static class UpdWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            List<String> servers = new ArrayList<>();
            try {
                servers = zoo.getChildren(ZOO_KEEPER_SERVER_DIR, this);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
            List<String> serversData = new ArrayList<>();
            getServersInfo(servers, serversData);
            storageActor.tell(new ServerMSG(serversData), ActorRef.noSender());
        }
    }

    private static void createZoo() throws IOException, KeeperException, InterruptedException {
        zoo = new ZooKeeper(
                ZOO_KEEPER_HOST,
                TIMEOUT_MILLIS,
                new UpdWatcher()
        );
        zoo.create(
                ZOO_KEEPER_CHILD_DIR + Integer.toString(port),
                Integer.toString(port).getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL
        );

        zoo.getChildren(ZOO_KEEPER_SERVER_DIR, new UpdWatcher());

    }


    private static void getServersInfo(List<String> servers, List<String> serversData) {
        for (String s : servers) {
            byte[] data = new byte[0];
            try {
                data = zoo.getData(ZOO_KEEPER_CHILD_DIR + s, false, null);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
            serversData.add(new String(data));
        }
    }

    CompletionStage<HttpResponse> fetchToServer(int port, String url, int parsedCount) {
        try {
            return http.singleRequest(
                    HttpRequest.create("http://localhost:" + Integer.toString(port) + "/?url=" + url + "&count=" +
                            Integer.toString(parsedCount - 1)));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity(NOT_FOUND));
        }
    }

    CompletionStage<HttpResponse> fetch(String url) {
        try {
            return http.singleRequest(
                    HttpRequest.create(url));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(HttpResponse.create().withEntity(NOT_FOUND));
        }
    }


    private Route route() {
        return concat(
                get(
                        () -> parameter(URL, url ->
                                parameter(COUNT, count -> {
                                            int parsedCount = Integer.parseInt(count);
                                            if (parsedCount != 0) {
                                                CompletionStage<HttpResponse> response = Patterns
                                                        .ask(
                                                                storageActor,
                                                                new GetRandomPort(Integer.toString(port)),
                                                                java.time.Duration.ofMillis(TIMEOUT_MILLIS)
                                                        )
                                                        .thenCompose(
                                                                req -> fetchToServer(
                                                                        (int) req,
                                                                        url,
                                                                        parsedCount
                                                                )
                                                        );
                                                return completeWithFuture(response);
                                            }
                                            try {
                                                return complete(fetch(url).toCompletableFuture().get());
                                            } catch (InterruptedException | ExecutionException e) {
                                                e.printStackTrace();
                                                return complete(URL_ERROR_MESSAGE);
                                            }

                                        }
                                )
                        )
                )
        );

    }
}


package bmstu.zookeeper;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class HTTPServerAkka extends AllDirectives {
    private static ZooKeeper zoo;
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
        //mainActor = system.actorOf(Props.create(MainActor.class));

        createZoo(PORT);

        http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

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
                event -> {
                    System.out.println("MAY BE IT WORKS");
                }
        );
        zoo.create(
                "/servers/" + Integer.toString(port),
                "data".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
    }

    CompletionStage<HttpResponse> fetch (String a, int parsedCount){
        return http.singleRequest(
                HttpRequest.create("http://localhost:2050/?" + "url=" + a + "&count=" +
                        Integer.toString(parsedCount - 1)));
    }

    private Route route() {
        return concat(
                get(
                        () -> parameter(URL, url ->
                                parameter(COUNT, count -> {
                                            int parsedCount = Integer.parseInt(count);
                                            System.out.println("COUNT->" + count);
                                            if (parsedCount != 0) {
                                                System.out.println(url + " " + count);
                                                return completeOKWithFuture(fetch(url, parsedCount), Jackson.marshaller());
                                            } else {
                                                return complete("HELLO BODY!");
                                            }
                                            //return completeOKWithFuture(result, Jackson.marshaller());
                                        }

                                ))));
    }
}

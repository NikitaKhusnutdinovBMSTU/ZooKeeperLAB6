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
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import scala.concurrent.Future;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class HTTPServerAkka extends AllDirectives {
    static ActorRef mainActor;
    private static final String ROUTES = "routes";
    private static final String LOCALHOST = "localhost";
    private static final String SERVER_INFO = "Server online on localhost:8080/\n PRESS ANY KEY TO STOP";
    private static final String PACKAGE_ID = "packageId";
    private static final String POST_MESSAGE = "Message was posted";
    private static final int SERVER_PORT = 8080;
    private static final int TIMEOUT_MILLIS = 5000;

    public static void main(String[] args) throws Exception {

        Scanner in = new Scanner(System.in);
        int PORT = in.nextInt();
        ActorSystem system = ActorSystem.create(ROUTES);
        //mainActor = system.actorOf(Props.create(MainActor.class));

        final Http http = Http.get(system);
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

    private Route route() {
        return concat(
                get(
                        () -> parameter(PACKAGE_ID, packageId ->
                                    parameter("count", count -> {
                                                //Future<Object> result = Patterns.ask(mainActor,
                                                        //Integer.parseInt(packageId),
                                                        //TIMEOUT_MILLIS);
                                                System.out.println(packageId + " " + count);
                                                return ("ITS OK!");
                                                //return completeOKWithFuture(result, Jackson.marshaller());
                                            }

                                    )
                        )
                )
        );
    }
}

package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import io.grpc.ManagedChannel;


public class FrontEndServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(FrontEndServer.class.getSimpleName());

        // Verifies if any of the arguments is "debug"
        boolean debug = false;
        for (String arg : args) {
            if (arg != null) {
                if (arg.equals("-debug")) {
                    System.setProperty("debug", "true");
                    debug = true;
                    break;  // Do not need to check the remaining arguments
                }
            }
        }

        // Validate the number of arguments
        if (args.length < 4 + (debug ? 1 : 0)) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=\"<frontend_port> <server_host:server_port>(s)\"");
            return;
        }

        // Front-end port
        int frontendPort;
        try {
            frontendPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            return;
        }

        // List of TupleSpaces Server Address
        List<String> serverAddresses = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i] != null) {
                if (args[i].equals("-debug")) {
                    continue;  // Skip the debug flag
                }
                serverAddresses.add(args[i]);
            }
        }

        System.out.println("Frontend will connect to the following servers:");
        for (String addr : serverAddresses) {
            System.out.println(" - " + addr);
        }

        // Create an instance of the Front-end gRPC service
        final BindableService frontendService = new FrontEndServiceImpl(serverAddresses);

        // Create and start the Front-end gRPC server
        Server server = ServerBuilder.forPort(frontendPort)
                .addService(ServerInterceptors.intercept(frontendService, new FrontEndInterceptor()))
                .build();

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Error when initializing Frontend server!");
            return;
        }

        System.out.printf("Frontend gRPC Server initialized in port: %d%n", frontendPort);

        try {
            // Shutdown for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Arrays.stream(((FrontEndServiceImpl)frontendService).getBackendStubs()) 
                        .map(stub -> (ManagedChannel) stub.getChannel())
                        .forEach(channel -> channel.shutdown());
                server.shutdown();
                System.out.println("\nFrontend server shut down.");
            }));

            // Block the main thread to wait until the server is terminated
            server.awaitTermination();
        } catch (InterruptedException e) {
            System.err.println("Frontend Server Stopped.");
        }
    }
}

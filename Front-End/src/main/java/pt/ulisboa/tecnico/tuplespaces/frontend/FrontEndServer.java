package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.BindableService;
import java.io.IOException;

public class FrontEndServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(FrontEndServer.class.getSimpleName());

        // Verifies if any of the arguments is "debug"
        for (String arg : args) {
            if (arg.equals("-debug")) {
                System.setProperty("debug", "true");
                break;  // Do not need to check the remaining arguments
            }
        }

        // Validate the number of arguments
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=\"<frontend_port> <server_host:server_port>\"");
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

        // TupleSpaces Server Address (in the future, it should be a list of servers)
        String serverAddress = args[1];

        // Create an instance of the Front-end gRPC service
        final BindableService frontendService = new FrontEndServiceImpl(serverAddress);

        // Create and start the Front-end gRPC server
        Server server = ServerBuilder.forPort(frontendPort).addService(frontendService).build();

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Error when initializing Front-end server!");
            return;
        }

        System.out.printf("Frontend gRPC Server initialized in port: %d%n", frontendPort);

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            System.err.println("Front-end Server Stopped.");
        }
    }
}

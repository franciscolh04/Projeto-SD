package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.Objects;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        // Check if any of the arguments is "debug"
        for (String arg : args) {
            if (arg.equals("-debug")) {
                System.setProperty("debug", "true");
                break;
            }
        }

        // Check arguments
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host:port> <client_id>");
            return;
        }

        // Get the host and the port of the server or front-end
        final String host_port = args[0];
        final int client_id = Integer.parseInt(args[1]);

        CommandProcessor parser = new CommandProcessor(new ClientService(host_port, client_id));
        parser.parseInput();

    }
}

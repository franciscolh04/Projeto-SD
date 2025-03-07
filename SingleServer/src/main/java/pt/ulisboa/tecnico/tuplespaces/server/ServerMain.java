package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Objects;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerMain {
    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println(ServerMain.class.getSimpleName());

        // Receber e imprimir argumentos
        /*
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }*/

        // Verifica se algum dos argumentos é "debug"
        for (String arg : args) {
            if (arg.equals("-debug")) {
                System.setProperty("debug", "true");
                break;  // Não precisamos de continuar a verificar os outros argumentos
            }
        }

        // Verificar argumentos
        if (args.length < 1) {
            System.err.println("Argument missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<port>");
            return;
        }

        // Obter e validar a porta
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            return;
        }

        final BindableService impl = new TupleSpacesServerImpl();

        // Criar e iniciar o servidor
        Server server = ServerBuilder.forPort(port).addService(impl).build();

        try {server.start();
        }
        catch (IOException e) {
        }

        System.out.printf("Server started on port: %d%n", port);

        try {server.awaitTermination();
        }
        catch (InterruptedException e){
        }
    }
}

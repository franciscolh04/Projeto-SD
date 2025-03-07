package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.BindableService;
import java.io.IOException;

public class FrontEndServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(FrontEndServer.class.getSimpleName());

        // Verifica se algum dos argumentos é "debug"
        for (String arg : args) {
            if (arg.equals("-debug")) {
                System.setProperty("debug", "true");
                break;  // Não precisamos de continuar a verificar os outros argumentos
            }
        }

        // Validar argumentos
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=\"<frontend_port> <server_host:server_port>\"");
            return;
        }

        // Porta do front-end
        int frontendPort;
        try {
            frontendPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            return;
        }

        // Endereço do servidor TupleSpaces (no futuro pode aceitar múltiplos)
        String serverAddress = args[1];

        // Criar instância do serviço gRPC do Front-end
        final BindableService frontendService = new FrontEndServiceImpl(serverAddress);

        // Criar e iniciar o servidor gRPC do Front-end
        Server server = ServerBuilder.forPort(frontendPort).addService(frontendService).build();

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor Front-end!");
            return;
        }

        System.out.printf("Frontend gRPC Server iniciado na porta: %d%n", frontendPort);

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            System.err.println("Servidor Front-end interrompido.");
        }
    }
}

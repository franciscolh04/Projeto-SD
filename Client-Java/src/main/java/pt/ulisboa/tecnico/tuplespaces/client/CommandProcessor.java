package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;

import java.util.Scanner;
import java.util.List;
import java.util.stream.Collectors;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);

            switch (split[0]) {
                case PUT:
                    this.put(split);
                    break;

                case READ:
                    this.read(split);
                    break;

                case TAKE:
                    this.take(split);
                    break;

                case GET_TUPLE_SPACES_STATE:
                    this.getTupleSpacesState();
                    break;

                case SLEEP:
                    this.sleep(split);
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    this.printUsage();
                    break;
            }
        }
        scanner.close();
    }

    private void put(String[] split) {
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        // get the tuple
        String tuple = split[1];

        // put the tuple and get the response
        PutResponse response = clientService.put(tuple);
        System.out.println("OK\n");
    }

    private void read(String[] split) {
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        // get the tuple
        String tuple = split[1];

        // read the tuple and get the response
        ReadResponse response = clientService.read(tuple);
        System.out.println("OK");

        String responseString = response.toString().replace("result: ", "").strip();
        if (responseString.startsWith("\"") && responseString.endsWith("\"")) {
            responseString = responseString.substring(1, responseString.length() - 1);
        }
        System.out.println(responseString + "\n");
    }

    private void take(String[] split) {
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        // get the tuple
        String tuple = split[1];

        // take the tuple and get the response
        TakeResponse response = clientService.take(tuple);
        System.out.println("OK");

        String responseString = response.toString().replace("result: ", "").strip();
        if (responseString.startsWith("\"") && responseString.endsWith("\"")) {
            responseString = responseString.substring(1, responseString.length() - 1);
        }
        System.out.println(responseString + "\n");
    }

    private void getTupleSpacesState() {
        // get the tuple spaces state
        getTupleSpacesStateResponse response = clientService.getTupleSpacesState();
        System.out.println("OK");
        // Transformar a lista de tuplas no formato desejado
        List<String> tuples = response.getTupleList(); // Supondo que seja uma lista de Strings
        String formattedOutput = tuples.stream()
                .collect(Collectors.joining(", ", "[", "]")); // Junta os elementos com ", " e coloca "[" e "]"

        System.out.println(formattedOutput + "\n");
    }

    private void sleep(String[] split) {
        if (split.length != 2) {
            this.printUsage();
            return;
        }

        // checks if input String can be parsed as an Integer
        try {
            int time = Integer.parseInt(split[1]);
            Thread.sleep(time * 1000L);
        } catch (NumberFormatException e) {
            System.out.println("Invalid time format. Use an integer value.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Sleep interrupted.");
        }
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState\n" +
                "- sleep <integer>\n" +
                "- exit\n");
    }

    private boolean inputIsValid(String[] input) {
        return input.length == 2 && input[1].startsWith(BGN_TUPLE) && input[1].endsWith(END_TUPLE);
    }
}

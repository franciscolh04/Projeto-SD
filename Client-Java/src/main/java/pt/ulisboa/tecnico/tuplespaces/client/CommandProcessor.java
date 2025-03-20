package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;

import java.util.ArrayList;
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

        // Add shutdown hook to capture interruption (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            clientService.shutdown();
        }));
    }

    // Method to parse the input commands
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
                    clientService.shutdown();
                    break;

                default:
                    this.printUsage();
                    break;
            }
        }
        scanner.close();
    }

    // Put a tuple in the tuple space
    private void put(String[] split) {
        // Check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        // Get the tuple
        String tuple = split[1];

        // Parse delays if provided
        List<Integer> delays = new ArrayList<>();
        for (int i = 2; i < split.length; i++) {
            try {
                delays.add(Integer.parseInt(split[i]));
            } catch (NumberFormatException e) {
                System.out.println("Invalid delay format. Use integer values.");
                return;
            }
        }

        // Put the tuple and get the response
        PutResponse response = clientService.put(tuple, delays);
        if (response != null) {
            System.out.println("OK");
        }
        System.out.println();
    }

    // Read a tuple from the tuple space
    private void read(String[] split) {
        // Check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        // Get the tuple
        String tuple = split[1];

        // Parse delays if provided
        List<Integer> delays = new ArrayList<>();
        for (int i = 2; i < split.length; i++) {
            try {
                delays.add(Integer.parseInt(split[i]));
            } catch (NumberFormatException e) {
                System.out.println("Invalid delay format. Use integer values.");
                return;
            }
        }

        // Read the tuple and get the response
        ReadResponse response = clientService.read(tuple, delays);
        if (response != null) {
            System.out.println("OK");

            String responseString = response.toString().replace("result: ", "").strip();
            if (responseString.startsWith("\"") && responseString.endsWith("\"")) {
                responseString = responseString.substring(1, responseString.length() - 1);
            }
            System.out.println(responseString);
        }
        System.out.println();
    }

    // Take a tuple from the tuple space
    private void take(String[] split) {
        // Check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        // Get the tuple
        String tuple = split[1];

        // Parse delays if provided
        List<Integer> delays = new ArrayList<>();
        for (int i = 2; i < split.length; i++) {
            try {
                delays.add(Integer.parseInt(split[i]));
            } catch (NumberFormatException e) {
                System.out.println("Invalid delay format. Use integer values.");
                return;
            }
        }

        // Take the tuple and get the response
        TakeResponse response = clientService.take(tuple, delays);
        if (response != null) {
            System.out.println("OK");

            String responseString = response.toString().replace("result: ", "").strip();
            if (responseString.startsWith("\"") && responseString.endsWith("\"")) {
                responseString = responseString.substring(1, responseString.length() - 1);
            }
            System.out.println(responseString);
        }
        System.out.println();
    }

    // Get the state of the tuple space
    private void getTupleSpacesState() {
        // Get the tuple spaces state
        getTupleSpacesStateResponse response = clientService.getTupleSpacesState();
        if (response != null) {
            System.out.println("OK");
            List<String> tuples = response.getTupleList();
            String formattedOutput = tuples.stream()
                    .collect(Collectors.joining(", ", "[", "]")); // Join the elements with ", " and enclose them in "[" and "]"

            System.out.println(formattedOutput);
        }
        System.out.println();
    }

    // Sleep for a given time
    private void sleep(String[] split) {
        if (split.length != 2) {
            this.printUsage();
            return;
        }

        // Checks if input String can be parsed as an Integer
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

    // Print the usage of the commands
    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState\n" +
                "- sleep <integer>\n" +
                "- exit\n");
    }

    // Check if the input is valid
    private boolean inputIsValid(String[] input) {
        return (input.length == 2 ||    // Without delays
                (input.length == 5 &&   // With delays
                        input[2].chars().allMatch(Character::isDigit) &&
                        input[3].chars().allMatch(Character::isDigit) &&
                        input[4].chars().allMatch(Character::isDigit)))
                // Check if the tuple is well-formed
                && input[1].startsWith(BGN_TUPLE)
                && input[1].endsWith(END_TUPLE);
    }
}

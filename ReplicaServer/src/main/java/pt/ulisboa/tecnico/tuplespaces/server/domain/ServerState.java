package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class ServerState {

  /**
   * Set flag to true to print debug messages.
   * The flag can be set using the -debug command line option.
   */
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  // Helper method to print debug messages.
  private static void debug(String debugMessage, int client_id) {
    if (DEBUG_FLAG)
      System.err.println("[DEBUG][" + client_id + "] " + debugMessage);
  }

  private List<String> tuples;
  private List<Boolean> tupleFlags;
  private final Object lock = new Object();

  public ServerState() {
    this.tuples = new ArrayList<String>();
    this.tupleFlags = new ArrayList<>();
  }

  // Add a tuple to the tuple space
  public void put(String tuple, int client_id) {
    synchronized (lock) {
      try {
        if (tuple == null || tuple.isEmpty()) {
          throw new IllegalArgumentException("Tuple cannot be Null or Empty.");
        }

        tuples.add(tuple);
        tupleFlags.add(true);
        debug("Added tuple: " + tuple, client_id);
        lock.notifyAll();
      } catch (IllegalArgumentException e) {
        System.err.println("Error adding tuple: " + e.getMessage());
      } catch (Exception e) {
        System.err.println("Unexpected error adding tuple: " + e.getMessage());
      }
    }
  }

  // Search for a tuple that matches the pattern
  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  // Search for a tuple that matches the pattern and is free
  private int getMatchingFreeTupleIndex(String pattern) {
    for (int i = 0; i < tuples.size(); i++) {
      if (tuples.get(i).matches(pattern) && tupleFlags.get(i)) {
        tupleFlags.set(i, false);
        return i;
      }
    }
    return -1;
  }

  // Read a tuple that matches the pattern
  public String read(String pattern, int client_id) {
    synchronized (lock) {
      try {
        if (pattern == null || pattern.isEmpty()) {
          throw new IllegalArgumentException("Search Pattern cannot be Null or Empty.");
        }

        // Search for a tuple that matches the pattern
        String tuple;
        boolean waitFlag = false;
        while ((tuple = getMatchingTuple(pattern)) == null) {
          try {
            waitFlag = true;
            lock.wait(); // Waits until `put()` calls `notifyAll()`
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restores the interrupted status
            System.err.println("Thread stopped while waiting for a matching tuple.");
            return null;
          }
        }

        if (!waitFlag) {
          debug("Read tuple: " + tuple, client_id);
        } else {
          debug("Read tuple after wait: " + tuple, client_id);
        }
        return tuple;

      } catch (IllegalArgumentException e) {
        System.err.println("Error reading a tuple: " + e.getMessage());
        return null;
      } catch (Exception e) { // Get any other unexpected error
        System.err.println("Unexpected error reading a tuple: " + e.getMessage());
        return null;
      }
    }
  }

  // Request access to a tuple that matches the pattern
  public int requestAccess(String pattern, int client_id) {
    synchronized (lock) {
      try {
        if (pattern == null || pattern.isEmpty()) {
          throw new IllegalArgumentException("Search Pattern cannot be Null or Empty.");
        }
        // Search for a tuple that matches the pattern
        int index = getMatchingFreeTupleIndex(pattern);
        if (index != -1) {
          debug("Granted Access: " + tuples.get(index), client_id);
          return index;
        }

        // If no matching tuple was found, wait until a matching tuple is added
        int matchingTuple;
        while ((matchingTuple = getMatchingFreeTupleIndex(pattern)) == -1) {
          try {
            lock.wait(); // Waits until `put()` calls `notifyAll()`
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread stopped while waiting for a matching tuple.");
            return -1;
          }
        }

        debug("Granted Access: " + matchingTuple, client_id);
        return matchingTuple;

      } catch (IllegalArgumentException e) {
        System.err.println("Error requesting access: " + e.getMessage());
        return -1;
      } catch (Exception e) {
        System.err.println("Unexpected error requesting access: " + e.getMessage());
        return -1;
      }
    }
  }

  // Take a tuple that matches the pattern with the given index
  public String takeWithIndex(int index, int client_id) {
    synchronized (lock) {
      try {
        if (index >= 0 && index < tuples.size()) {
          String removed = tuples.remove(index);
          tupleFlags.remove(index);
          debug("Removed tuple at index " + index + ": " + removed, client_id);
          return removed;
        } else {
          throw new IllegalArgumentException("Invalid index for take: " + index);
        }
      } catch (IllegalArgumentException e) {
        System.err.println("Error taking tuple: " + e.getMessage());
        return null;
      } catch (Exception e) {
        System.err.println("Unexpected error taking tuple: " + e.getMessage());
        return null;
      }
    }
  }

  // Get the current state of the tuple spaces
  public List<String> getTupleSpacesState(int client_id) {
    synchronized (lock) {
      try {
        // Returns a copy of the list to avoid external modifications
        debug("TupleSpaces Current State: " + this.tuples, client_id);
        return List.copyOf(this.tuples);
      } catch (Exception e) { // Get any other unexpected error
        System.err.println("Unexpected error when getting state of TupleSpaces: " + e.getMessage());
        return Collections.emptyList(); // Return an empty list if an error occurs
      }
    }
  }
}
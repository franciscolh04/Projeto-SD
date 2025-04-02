package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;


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

  // Tuple space hash map
  private ConcurrentKeyStore<String> tuples = new ConcurrentKeyStore<>();
  private final Object lock = new Object();

  public ServerState() {
    this.tuples = new ConcurrentKeyStore<>();
  }

  // Add a tuple to the tuple space
  public void put(String tuple, int client_id) {
    synchronized (lock) {
      try {
        if (tuple == null || tuple.isEmpty()) {
          throw new IllegalArgumentException("Tuple cannot be Null or Empty.");
        }

        tuples.putOrIncrement(tuple);
        debug("Added tuple: " + tuple, client_id);

        lock.notifyAll();
      } catch (IllegalArgumentException e) {
        System.err.println("Error adding tuple: " + e.getMessage());
      } catch (Exception e) {
        System.err.println("Unexpected error adding tuple: " + e.getMessage());
      }
    }
  }


  // Search for a tuple that matches the pattern in the tuple space
  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples.getAllKeys()) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  // Search for a tuple that matches the pattern and is free (counter > 0 && lock is available)
  private String getMatchingAvailableTuple(String pattern) {
    for (String tuple : tuples.getAllKeys()) {
      var entry = tuples.getEntry(tuple);
      if (tuple.matches(pattern) && entry != null && entry.getCounter() > 0) {
          if (entry.lock.tryLock())
            return tuple;
        }
      }
    return null;
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
        // if the tuple is not found, wait until a tuple is added to try again
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
        // if the tuple is found (it reached here), print debug messages

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


  // Request access to a tuple(s) that matches the pattern
  public List<String> requestAccess(String pattern, int client_id) {
    synchronized (lock) {
      try {
        if (pattern == null || pattern.isEmpty()) {
          throw new IllegalArgumentException("Search Pattern cannot be Null or Empty.");
        }

        // We have to check if the pattern is a regex or not
        boolean isRegex = pattern.contains("[^,]+");
        return isRegex
                ? handleRegexAccess(pattern, client_id) // Regex pattern
                : handleSimpleAccess(pattern, client_id); // Simple pattern

      } catch (IllegalArgumentException e) {
        System.err.println("Error requesting access: " + e.getMessage());
        return null;
      } catch (Exception e) {
        System.err.println("Unexpected error requesting access: " + e.getMessage());
        return null;
      }
    }
  }

  // Helps requestAccess method to handle regex request access
  private List<String> handleRegexAccess(String pattern, int client_id) {
    List<String> reserved = new ArrayList<>();

    // Loop until we can lock ALL the tuples that match the pattern
    while (true) {

      // Get all tuples that match the pattern
      List<String> matchingTuples = tuples.getAllKeys().stream()
              .filter(t -> t.matches(pattern))
              .toList();

      // Try to lock all the matching tuples
      boolean allLocked = true;
      for (String tuple : matchingTuples) {
        var entry = tuples.getEntry(tuple);
        if (entry != null && entry.getCounter() > 0 && entry.lock.tryLock()) {
          reserved.add(tuple);
        } else { // we set the flag to false if we can't lock all the tuples
          allLocked = false;
          break;
        }
      }

      // If we can't lock all the tuples, we have to release the ones we locked
      if (!allLocked || reserved.size() < matchingTuples.size()) {
        // Release all the reserved tuples
        for (String t : reserved) {
          var entry = tuples.getEntry(t);
          if (entry != null && entry.lock.isHeldByCurrentThread()) {
            entry.lock.unlock();
          }
        }
        reserved.clear();

        // And now we wait for a new tuple to be added or unlocked
        // in order to try again to lock all the tuples
        try {
          lock.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.err.println("Thread interrupted while waiting for matching regex tuples.");
          return null;
        }
      } else { // Success: If we locked all the tuples, we can return the reserved tuples
        debug("Reserved tuples (regex): " + reserved, client_id);
        return reserved;
      }
    }
  }

  // Helps requestAccess method to handle simple request access
  private List<String> handleSimpleAccess(String pattern, int client_id) {
    List<String> reserved = new ArrayList<>();
    // Although we are using a list, we are only going to lock one tuple
    // This is to ensure consistency with the regex request access method

    // Check if we can lock a tuple that matches the pattern
    String tuple = getMatchingAvailableTuple(pattern);
    if (tuple != null) { // If we can lock the tuple, we add it to the reserved list
      debug("Granted Access (lock acquired): " + tuple, client_id);
      reserved.add(tuple);
      return reserved;
    }

    // If we can't lock a tuple, we have to wait until we can
    while ((tuple = getMatchingAvailableTuple(pattern)) == null) {
      try {
        lock.wait(); // Waits until a tuple is added or unlocked (notifyAll)
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("Thread stopped while waiting for a matching tuple.");
        return null;
      }
    }

    // If we can lock the tuple after the wait, we add it to the reserved list and return it
    debug("Granted Access (after wait): " + tuple, client_id);
    reserved.add(tuple);
    return reserved;
  }


  // Take a tuple that matches the pattern given
  public String takeServer(String tuple, int client_id) {
    synchronized (lock) {
      try {
        if (tuple == null) {
          throw new IllegalArgumentException("Tuple cannot be Null.");
        }

        // Wait until the tuple is added to the tuple space if needed (wait for put() if delayed)
        while (tuples.getEntry(tuple) == null) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted while waiting for tuple: " + tuple);
            return null;
          }
        }

        // Gets the tuple and decrements the counter
        var entry = tuples.getEntry(tuple);
        entry.decrementCounter();
        if (entry.getCounter() == 0) { // If the counter reaches 0, we remove the tuple
          tuples.remove(tuple);
          debug("Removed tuple (counter == 0): " + tuple, client_id);
        } else {
          debug("Decremented tuple counter: " + tuple + " (remaining: " + entry.getCounter() + ")", client_id);
        }

        // Unlock the tuple because we have now taken it
        if (entry.lock.isHeldByCurrentThread()) {
          entry.lock.unlock();
        }

        return tuple;
      } catch (IllegalArgumentException e) {
        System.err.println("Error in takeServer: " + e.getMessage());
        return null;
      } catch (Exception e) {
        System.err.println("Unexpected error in takeServer: " + e.getMessage());
        return null;
      }
    }
  }


  // Release multiple tuples and notify waiting threads after releasing them all
  // This method is used by the client to release the remaining tuples it has locked before in a regex requestAccess
  public void release(List<String> tuplesToRelease, int client_id) {
    boolean releasedSomething = false;

    // If the list is empty, there is nothing to release
    if (tuplesToRelease == null || tuplesToRelease.isEmpty()) {
      return;
    }

    // Release all the remaining tuples that the client had locked
    synchronized (lock) {
      for (String tuple : tuplesToRelease) {
        var entry = tuples.getEntry(tuple);
        if (entry != null && entry.lock.isHeldByCurrentThread()) {
          entry.lock.unlock();
          releasedSomething = true; // Set the flag to true if we released at least one tuple (verification Flag)
          debug("Released tuple lock: " + tuple, client_id);
        }
      }

      // Notify all the waiting threads that at least 1 tuple was released (unlocked)
      if (releasedSomething) {
        debug("Calling notifyAll() after releasing tuples.", client_id);
        lock.notifyAll();
      }
    }
  }


  // Get the current state of the tuple space
  public List<String> getTupleSpacesState(int client_id) {
    synchronized (lock) {
      try {
        // Create a list with all the tuples in the tuple space
        List<String> tuplesArray = new ArrayList<>();
        for (String tuple : tuples.getAllKeys()) { // for each tuple in the tuple space
          var entry = tuples.getEntry(tuple);
          if (entry != null) {
            for (int i = 0; i < entry.getCounter(); i++) { // add the tuple to the list as many times as the counter
              tuplesArray.add(tuple);
            }
          }
        }

        debug("TupleSpaces Current State: " + tuplesArray, client_id);
        return tuplesArray;
      } catch (Exception e) {
        System.err.println("Unexpected error when getting state of TupleSpaces: " + e.getMessage());
        return Collections.emptyList();
      }
    }
  }


}
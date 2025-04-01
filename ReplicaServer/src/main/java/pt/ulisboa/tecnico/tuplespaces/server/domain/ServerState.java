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

  // Search for a tuple that matches the pattern
  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples.getAllKeys()) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  // Search for a tuple that matches the pattern and is free
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
  public List<String> requestAccess(String pattern, int client_id) {
    synchronized (lock) {
      try {
        if (pattern == null || pattern.isEmpty()) {
          throw new IllegalArgumentException("Search Pattern cannot be Null or Empty.");
        }
        List<String> reserved = new ArrayList<>();
        boolean isRegex = pattern.contains("[^,]+");

        // REGEX
        if (isRegex) {

          for (String tuple : tuples.getAllKeys()) {
            var entry = tuples.getEntry(tuple);
            if (tuple.matches(pattern) && entry != null && entry.getCounter() > 0) {
              if (entry.lock.tryLock()) {
                reserved.add(tuple);
              }
            }
          }

          if (reserved.isEmpty()) {
            while (reserved.isEmpty()) {
              try {
                lock.wait();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread interrupted while waiting for matching regex tuples.");
                return null;
              }

              for (String tuple : tuples.getAllKeys()) {
                var entry = tuples.getEntry(tuple);
                if (tuple.matches(pattern) && entry != null && entry.getCounter() > 0) {
                  if (entry.lock.tryLock()) {
                    reserved.add(tuple);
                  }
                }
              }
            }
          }

          debug("Reserved tuples (regex): " + reserved, client_id);
          return reserved;
        }

        //NOT REGEX

        // Search for a tuple that matches the pattern
        String tuple = getMatchingAvailableTuple(pattern);
        if (tuple != null) {
          debug("Granted Access (lock acquired): " + tuple, client_id);
          reserved.add(tuple);
          return reserved;
        }

        // If no matching tuple was found, wait until a matching tuple is added
        String matchingTuple;
        while ((matchingTuple = getMatchingAvailableTuple(pattern)) == null) {
          try {
            lock.wait(); // Waits until `put()` calls `notifyAll()`
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread stopped while waiting for a matching tuple.");
            return null;
          }
        }

        debug("Granted Access (after wait): " + matchingTuple, client_id);
        reserved.add(matchingTuple);
        return reserved;

      } catch (IllegalArgumentException e) {
        System.err.println("Error requesting access: " + e.getMessage());
        return null;
      } catch (Exception e) {
        System.err.println("Unexpected error requesting access: " + e.getMessage());
        return null;
      }
    }
  }

  public String takeServer(String tuple, int client_id) {
    synchronized (lock) {
      try {
        if (tuple == null) {
          throw new IllegalArgumentException("Tuple cannot be Null.");
        }

        while (tuples.getEntry(tuple) == null) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted while waiting for tuple: " + tuple);
            return null;
          }
        }

        var entry = tuples.getEntry(tuple); // volta a obter após o wait
        entry.decrementCounter();
        if (entry.getCounter() == 0) {
          tuples.remove(tuple);
          debug("Removed tuple (counter == 0): " + tuple, client_id);
        } else {
          debug("Decremented tuple counter: " + tuple + " (remaining: " + entry.getCounter() + ")", client_id);
        }

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

  // Release a tuple (unlock it without modifying the counter)
  public void release(String tuple, int client_id) {
    var entry = tuples.getEntry(tuple);
    if (entry != null && entry.lock.isHeldByCurrentThread()) {
      entry.lock.unlock();
      debug("Released tuple lock: " + tuple, client_id);
    }
  }



  public List<String> getTupleSpacesState(int client_id) {
    synchronized (lock) {
      try {
        List<String> tuplesArray = new ArrayList<>();
        for (String tuple : tuples.getAllKeys()) {
          var entry = tuples.getEntry(tuple);
          if (entry != null) {
            for (int i = 0; i < entry.getCounter(); i++) {
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
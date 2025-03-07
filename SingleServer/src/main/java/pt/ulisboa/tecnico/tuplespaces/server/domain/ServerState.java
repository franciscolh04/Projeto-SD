package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

  /** Set flag to true to print debug messages.
     * The flag can be set using the -debug command line option. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println("[DEBUG] " + debugMessage);
    }

  private List<String> tuples;
  private final Object lock = new Object();

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public void put(String tuple) {
    synchronized (lock) {
      tuples.add(tuple);
      debug("Added tuple: " + tuple);
      lock.notifyAll();
    }
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public String read(String pattern) {
    synchronized (lock) {
      String tuple;
        while ((tuple = getMatchingTuple(pattern)) == null) {
            try {
            lock.wait();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
        }
      debug("Read tuple: " + pattern);
      return getMatchingTuple(pattern);
    }
  }

  public String take(String pattern) {
    // Procura o primeiro tuplo que corresponde ao padrão
    synchronized (lock) {
      for (String tuple : this.tuples) {
        if (tuple.matches(pattern)) {
          this.tuples.remove(tuple);
          debug("Removed tuple: " + tuple);

          return tuple;
        }
      }
      String matchingTuple;
      while ((matchingTuple = getMatchingTuple(pattern)) == null) {
        try {
          lock.wait(); // Aguarda até `put()` chamar `notifyAll()`
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return null; // Retorna null se a thread for interrompida
        }
      }
      tuples.remove(matchingTuple);
      debug("Removed tuple after wait: " + matchingTuple);
      return matchingTuple;
    }
  }

  public List<String> getTupleSpacesState() {
    // Retorna uma cópia da lista para evitar modificações externas
    debug("TupleSpaces Current State: " + this.tuples);
    return List.copyOf(this.tuples);
  }
}

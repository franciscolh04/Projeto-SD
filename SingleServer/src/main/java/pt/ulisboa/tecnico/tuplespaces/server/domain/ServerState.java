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

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public void put(String tuple) {
    tuples.add(tuple);
    debug("Added tuple: " + tuple);
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
    debug("Read tuple: " + pattern);
    return getMatchingTuple(pattern);
  }

  public String take(String pattern) {
    // Procura o primeiro tuplo que corresponde ao padrão
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        this.tuples.remove(tuple);
        debug("Removed tuple: " + tuple);
        return tuple;
      }
    }
    return null; // Se nenhum tuplo corresponder
  }

  public List<String> getTupleSpacesState() {
    // Retorna uma cópia da lista para evitar modificações externas
    debug("TupleSpaces Current State: " + this.tuples);
    return List.copyOf(this.tuples);
  }
}

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

  /**
   * Helper method to print debug messages.
   */
  private static void debug(String debugMessage, int client_id) {
    if (DEBUG_FLAG)
      System.err.println("[DEBUG][" + client_id + "] " + debugMessage);
  }

  private List<String> tuples;
  private final Object lock = new Object();

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }

  public void put(String tuple, int client_id) {
    synchronized (lock) {
      try {
        if (tuple == null || tuple.isEmpty()) {
          throw new IllegalArgumentException("Tuple cannot be Null or Empty.");
        }

        tuples.add(tuple);
        debug("Added tuple: " + tuple, client_id);
        lock.notifyAll();
      } catch (IllegalArgumentException e) {
        System.err.println("Error adding tuple: " + e.getMessage());
      } catch (Exception e) {
        System.err.println("Unexpected error adding tuple: " + e.getMessage());
      }
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

  public String read(String pattern, int client_id) {
    synchronized (lock) {
      try {
        if (pattern == null || pattern.isEmpty()) {
          throw new IllegalArgumentException("Search Pattern cannot be Null or Empty.");
        }

        String tuple;
        while ((tuple = getMatchingTuple(pattern)) == null) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura o estado de interrupção
            System.err.println("Thread stopped while waiting for a matching tuple.");
            return null; // Opcional: pode retornar null ou lançar uma exceção
          }
        }

        debug("Read tuple: " + tuple, client_id);
        return tuple;

      } catch (IllegalArgumentException e) {
        System.err.println("Error reading a tuple: " + e.getMessage());
        return null;
      } catch (Exception e) { // Captura erros inesperados
        System.err.println("Unexpected error reading a tuple: " + e.getMessage());
        return null;
      }
    }
  }


  public String take(String pattern, int client_id) {
    synchronized (lock) { // Adquirir o bloqueio antes de qualquer operação crítica
      try {
        if (pattern == null || pattern.isEmpty()) {
          throw new IllegalArgumentException("Search Pattern cannot be Null or Empty.");
        }

        // Procura um tuplo correspondente e remove
        for (String tuple : this.tuples) {
          if (tuple.matches(pattern)) {
            this.tuples.remove(tuple);
            debug("Removed tuple: " + tuple, client_id);
            return tuple;
          }
        }

        // Se não encontrou, espera até que um tuplo correspondente seja adicionado
        String matchingTuple;
        while ((matchingTuple = getMatchingTuple(pattern)) == null) {
          try {
            lock.wait(); // Aguarda até `put()` chamar `notifyAll()`
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread stopped while waiting for a matching tuple.");
            return null;
          }
        }

        // Remove o tuplo encontrado após o wait
        tuples.remove(matchingTuple);
        debug("Removed tuple after wait: " + matchingTuple, client_id);
        return matchingTuple;

      } catch (IllegalArgumentException e) {
        System.err.println("Error removing a tuple: " + e.getMessage());
        return null;
      } catch (Exception e) {
        System.err.println("Unexpected error removing a tuple: " + e.getMessage());
        return null;
      }
    }
  }


  public List<String> getTupleSpacesState(int client_id) {
    try {
      // Retorna uma cópia da lista para evitar modificações externas
      debug("TupleSpaces Current State: " + this.tuples, client_id);
      return List.copyOf(this.tuples);
    } catch (Exception e) { // Captura qualquer erro inesperado
      System.err.println("Unexpected error when getting state of TupleSpaces: " + e.getMessage());
      return Collections.emptyList(); // Retorna uma lista vazia em caso de erro
    }
  }

}
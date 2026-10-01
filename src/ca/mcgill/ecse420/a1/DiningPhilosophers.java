package ca.mcgill.ecse420.a1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Question 3.1: simulates n dining philosophers, where each philosopher is a thread and each
 * chopstick is a shared object.
 *
 * <p>Every philosopher picks up the chopstick on their left first and then the one on their right.
 * Because all of them follow the same order, the program can deadlock: if every philosopher holds
 * their left chopstick at the same moment, each one waits forever for the right one.
 */
public class DiningPhilosophers {

  /** Longest time (in milliseconds) a philosopher thinks or eats for. */
  private static final int MAX_ACTIVITY_MS = 5;

  /**
   * Time (in milliseconds) a philosopher waits between picking up the first and the second
   * chopstick. This only makes the deadlock show up quickly; it does not cause it.
   */
  private static final int PAUSE_BETWEEN_PICKUPS_MS = 10;

  /**
   * Creates the chopsticks and philosophers, then starts every philosopher in its own thread.
   *
   * @param args optionally, the number of philosophers (defaults to 5)
   */
  public static void main(String[] args) {

    int numberOfPhilosophers = args.length > 0 ? Integer.parseInt(args[0]) : 5;
    if (numberOfPhilosophers < 2) {
      throw new IllegalArgumentException("Need at least 2 philosophers (and 2 chopsticks).");
    }

    Philosopher[] philosophers = new Philosopher[numberOfPhilosophers];
    Object[] chopsticks = new Object[numberOfPhilosophers];

    // One chopstick between each pair of neighbours.
    for (int i = 0; i < numberOfPhilosophers; i++) {
      chopsticks[i] = new Object();
    }

    // The pool needs exactly one thread per philosopher, otherwise some philosophers would sit
    // in the queue and never run at the same time as the others.
    ExecutorService executor = Executors.newFixedThreadPool(numberOfPhilosophers);

    for (int i = 0; i < numberOfPhilosophers; i++) {
      // Philosopher i has chopstick i on their left and chopstick i + 1 on their right. The
      // modulo makes the table circular: the last philosopher's right chopstick is chopstick 0.
      Object left = chopsticks[i];
      Object right = chopsticks[(i + 1) % numberOfPhilosophers];

      philosophers[i] = new Philosopher(i, left, right);
      executor.execute(philosophers[i]);
    }
  }

  /** A philosopher who forever thinks, picks up two chopsticks, eats, and puts them down. */
  public static class Philosopher implements Runnable {

    private final int id;
    private final Object leftChopstick;
    private final Object rightChopstick;

    /**
     * Creates a philosopher.
     *
     * @param id this philosopher's number, used in the printed messages
     * @param leftChopstick the chopstick on this philosopher's left
     * @param rightChopstick the chopstick on this philosopher's right
     */
    public Philosopher(int id, Object leftChopstick, Object rightChopstick) {
      this.id = id;
      this.leftChopstick = leftChopstick;
      this.rightChopstick = rightChopstick;
    }

    @Override
    public void run() {
      try {
        while (true) {
          think();

          // synchronized gives mutual exclusion: only one thread at a time can hold the lock of
          // a given object, so only one philosopher can hold a given chopstick. Any other
          // philosopher who wants it blocks here until it is released.
          synchronized (leftChopstick) {
            System.out.println("Philosopher " + id + " picked up left chopstick");

            // Hold the left chopstick for a moment before reaching for the right one.
            Thread.sleep(PAUSE_BETWEEN_PICKUPS_MS);

            // Hold-and-wait: we keep the left chopstick while waiting for the right one.
            synchronized (rightChopstick) {
              System.out.println("Philosopher " + id + " picked up right chopstick");
              eat();
            }
            // Leaving the inner block puts down the right chopstick.
          }
          // Leaving the outer block puts down the left chopstick.
          System.out.println("Philosopher " + id + " put down both chopsticks");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    /** Thinks for a random amount of time. */
    private void think() throws InterruptedException {
      System.out.println("Philosopher " + id + " is thinking");
      Thread.sleep(ThreadLocalRandom.current().nextInt(MAX_ACTIVITY_MS));
    }

    /** Eats for a random amount of time. */
    private void eat() throws InterruptedException {
      System.out.println("Philosopher " + id + " is eating");
      Thread.sleep(ThreadLocalRandom.current().nextInt(MAX_ACTIVITY_MS));
    }
  }
}

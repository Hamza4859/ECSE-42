package ca.mcgill.ecse420.a1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question 3.2: dining philosophers that never deadlock.
 *
 * <p>The chopsticks are numbered 0 to n - 1 and every philosopher picks up the lower-numbered of
 * their two chopsticks first, then the higher-numbered one. This removes the circular wait, so a
 * ring of philosophers each holding one chopstick and waiting for the next can never form.
 */
public class DiningPhilosophersFixed {

  /** Longest time (in milliseconds) a philosopher thinks or eats for. */
  private static final int MAX_ACTIVITY_MS = 5;

  /**
   * Time (in milliseconds) a philosopher waits between picking up the first and the second
   * chopstick. Question 3.1 deadlocked with this exact pause, so it shows the fix works.
   */
  private static final int PAUSE_BETWEEN_PICKUPS_MS = 10;

  /** How often (in seconds) the total number of meals eaten by each philosopher is printed. */
  private static final int REPORT_INTERVAL_SECONDS = 1;

  /**
   * Whether to print every think, pick-up, eat and put-down event. When false, only the periodic
   * meal totals are printed, which keeps them easy to read. Set to true to see every event.
   */
  private static final boolean VERBOSE = false;

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

    // The pool needs exactly one thread per philosopher so that all of them run at the same time.
    ExecutorService executor = Executors.newFixedThreadPool(numberOfPhilosophers);

    for (int i = 0; i < numberOfPhilosophers; i++) {
      // Philosopher i sits between chopstick i (left) and chopstick i + 1 (right). The modulo
      // makes the table circular: the last philosopher's right chopstick is chopstick 0.
      int leftIndex = i;
      int rightIndex = (i + 1) % numberOfPhilosophers;

      // The fix: always pick up the lower-numbered chopstick first. For every philosopher except
      // the last this is the left chopstick. The last philosopher has chopsticks n - 1 and 0, so
      // they pick up chopstick 0 first, the opposite of what they did in question 3.1.
      Object first = chopsticks[Math.min(leftIndex, rightIndex)];
      Object second = chopsticks[Math.max(leftIndex, rightIndex)];

      philosophers[i] = new Philosopher(i, first, second);
      executor.execute(philosophers[i]);
    }

    // A separate thread prints how many meals each philosopher has eaten so far. It only reads the
    // counters and never touches the chopsticks, so it cannot affect deadlock or starvation.
    ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();
    reporter.scheduleAtFixedRate(
        () -> printMealCounts(philosophers),
        REPORT_INTERVAL_SECONDS,
        REPORT_INTERVAL_SECONDS,
        TimeUnit.SECONDS);
  }

  /**
   * Prints, on a single line, the number of meals each philosopher has eaten so far.
   *
   * @param philosophers the philosophers whose meals are counted
   */
  private static void printMealCounts(Philosopher[] philosophers) {
    StringBuilder line = new StringBuilder("=== Meals eaten so far:");
    int total = 0;
    for (Philosopher philosopher : philosophers) {
      int meals = philosopher.getMealsEaten();
      total += meals;
      line.append(" P").append(philosopher.id).append("=").append(meals);
    }
    line.append(" | Total=").append(total);
    System.out.println(line);
  }

  /**
   * Prints a message about what a philosopher is doing, but only when {@link #VERBOSE} is true.
   *
   * @param message the message to print
   */
  private static void log(String message) {
    if (VERBOSE) {
      System.out.println(message);
    }
  }

  /** A philosopher who forever thinks, picks up two chopsticks, eats, and puts them down. */
  public static class Philosopher implements Runnable {

    private final int id;
    private final Object firstChopstick;
    private final Object secondChopstick;

    /**
     * Number of meals this philosopher has eaten. It is written by the philosopher's own thread and
     * read by the reporter thread, so it is atomic to make sure the reporter sees up-to-date values.
     */
    private final AtomicInteger mealsEaten = new AtomicInteger();

    /**
     * Creates a philosopher.
     *
     * @param id this philosopher's number, used in the printed messages
     * @param firstChopstick the lower-numbered chopstick, which is picked up first
     * @param secondChopstick the higher-numbered chopstick, which is picked up second
     */
    public Philosopher(int id, Object firstChopstick, Object secondChopstick) {
      this.id = id;
      this.firstChopstick = firstChopstick;
      this.secondChopstick = secondChopstick;
    }

    @Override
    public void run() {
      try {
        while (true) {
          think();

          // synchronized gives mutual exclusion: only one philosopher at a time can hold a given
          // chopstick. Any other philosopher who wants it blocks until it is released.
          synchronized (firstChopstick) {
            log("Philosopher " + id + " picked up first chopstick");

            // Hold the first chopstick for a moment before reaching for the second one.
            Thread.sleep(PAUSE_BETWEEN_PICKUPS_MS);

            synchronized (secondChopstick) {
              log("Philosopher " + id + " picked up second chopstick");
              eat();
            }
            // Leaving the inner block puts down the second chopstick.
          }
          // Leaving the outer block puts down the first chopstick.
          log("Philosopher " + id + " put down both chopsticks");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    /** Thinks for a random amount of time. */
    private void think() throws InterruptedException {
      log("Philosopher " + id + " is thinking");
      Thread.sleep(ThreadLocalRandom.current().nextInt(MAX_ACTIVITY_MS));
    }

    /** Eats for a random amount of time and counts the meal. */
    private void eat() throws InterruptedException {
      log("Philosopher " + id + " is eating");
      mealsEaten.incrementAndGet();
      Thread.sleep(ThreadLocalRandom.current().nextInt(MAX_ACTIVITY_MS));
    }

    /**
     * Returns how many meals this philosopher has eaten so far.
     *
     * @return the number of meals eaten
     */
    public int getMealsEaten() {
      return mealsEaten.get();
    }
  }
}

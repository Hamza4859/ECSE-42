package ca.mcgill.ecse420.a1;

/**
 * Question 2: demonstrates a deadlock between two threads.
 *
 * <p>Mike takes the hotdog and then needs the ketchup, while Bob takes the ketchup and then needs
 * the hotdog. Each one holds what they took and waits for the other's item, so neither can ever
 * finish eating and the program hangs.
 */
public class Deadlock {

  /** First shared resource; only one thread can hold it at a time. */
  private static final Object HOTDOG = new Object();

  /** Second shared resource; only one thread can hold it at a time. */
  private static final Object KETCHUP = new Object();

  /**
   * Starts the two threads that deadlock.
   *
   * @param args unused
   */
  public static void main(String[] args) {

    // Mike acquires the locks in the order: hotdog, then ketchup.
    Thread mike = new Thread(() -> {
      synchronized (HOTDOG) {
        System.out.println("Mike has the hotdog");

        // Pause while holding the hotdog so that Bob has time to take the ketchup.
        // Without this, Mike could take both locks before Bob even starts.
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Mike now waits for the ketchup while still holding the hotdog (hold and wait).
        synchronized (KETCHUP) {
          System.out.println("Mike has both and eats");
        }
      }
    });

    // Bob acquires the locks in the opposite order: ketchup, then hotdog.
    Thread bob = new Thread(() -> {
      synchronized (KETCHUP) {
        System.out.println("Bob has the ketchup");

        // Pause while holding the ketchup so that Mike has time to take the hotdog.
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Bob now waits for the hotdog while still holding the ketchup (circular wait).
        synchronized (HOTDOG) {
          System.out.println("Bob has both and eats");
        }
      }
    });

    mike.start();
    bob.start();
  }
}

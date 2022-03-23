//Kailey Daniel
//Dr.Liao, CPS240 HW9
//This program utilizes multithreading to simulate a dining room
//A Mom will continuously make cookies for her 4 children (5 threads) until they are full.

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HappyMom {

	// variables
	private static int cookies = 0;
	private static Lock lock = new ReentrantLock();
	// Condition to determine if Mom can make cookies
	private static Condition momCond = lock.newCondition();
	// Condition to determine if Child can eat cookies
	private static Condition childCond = lock.newCondition();
	private static Room room = new Room();
	static boolean threadsTerminated = false;

	// Mom class
	public static class Mom implements Runnable {
		private String name;
		// volatile boolean to use as condition to terminate thread
		private volatile boolean momExit = false;
		// Integer to count the number of times the plate is full
		// Integer used to count how many times Mom makes 0 cookies
		private int plateFull = 0;
		// Integer to count total number of cookies made
		private int cookiesMade = 0;

		public Mom(String name) {
			this.name = name;
		}

		public void run() {
			// Call putCookies from room class

			try { // Purposely delay it

				while (!momExit) {
					// determine how many cookies needed for full plate
					int cookiesNeeded = 10 - room.getBalance();

					room.putCookies(cookiesNeeded);
					cookiesMade += cookiesNeeded;

					System.out.println("Mom: made " + cookiesNeeded + " cookies.");
					Thread.sleep(1000);
					System.out.println("Mom: tired, sleep...");
					if (cookiesNeeded == 0) {
						plateFull++;
					}
					if (plateFull > 3) {
						momExit = true;

					}
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}

		// Get cookies made method to use in main
		public int getCookiesMade() {
			return cookiesMade;
		}

		// Get name method to use in main
		public String getName() {
			return name;
		}

	}

	// Child class
	public static class Child implements Runnable {
		private String name;
		// volatile boolean used to determine if condition is met to terminate thread
		private volatile boolean childExit = false;
		// Integer to count total number of cookies eaten by a child
		private int cookiesEaten = 0;
		// final int to represent when child is full
		final int MAX_COOKIES = 20;

		public Child(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void run() {
			// Call eatCookies from room class

			try {
				while (!childExit) {
					// Generate random number of cookies 1-10 child will eat
					// add eaten cookies to variable
					cookiesEaten += room.eatCookies((int) (Math.random() * 10) + 1);
					Thread.sleep(1000); // Go play outside after eating
					System.out.println(name + ": playing...");
					if (cookiesEaten > MAX_COOKIES) {
						childExit = true;

					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//Get cookies eaten method to use in main
		public int getCookiesEaten() {
			return cookiesEaten;
		}
	}

	public static class Room {

		// create semephores
		// 2 permits available for children, 1 permit always available for mom
		private static Semaphore childSem = new Semaphore(2);
		private static Semaphore momSem = new Semaphore(1);
		private static int balance = 0;

		//get balance method to return the number of cookies present on plate
		public int getBalance() {
			return balance;
		}

		public int eatCookies(int cookies) {

			try {
				System.out.println(Thread.currentThread().getName() + ": wants to enter the room.");
				childSem.acquire();
				System.out.println(Thread.currentThread().getName() + ": enters the room. "
						+ (2 - childSem.availablePermits()) + " children and " + balance + " cookies are in the room.");
				System.out.println(Thread.currentThread().getName() + ": wants to eat " + cookies + " cookies");
				lock.lock();
				// If there are not enough cookies on the plate, must wait for mom
				while (balance < cookies) {
					childCond.await();
				}
				System.out.println(Thread.currentThread().getName() + ": eats " + cookies + " cookies");
				//update balance of cookie plate after eating
				balance -= cookies;
			} catch (InterruptedException ex) {

			} finally {
				//Release permit for another child
				childSem.release();
				System.out.println(Thread.currentThread().getName() + ": left the room.");
				lock.unlock();
			}
			return cookies;
		}

		public int putCookies(int cookies) {

			lock.lock();

			try {
				System.out.println("Mom: enters the room. " + (2 - childSem.availablePermits()) + " children and "
						+ balance + " cookies are in the room.");
				momSem.acquire();
				//update balance of cookie plate after making
				balance += cookies;

				childCond.signalAll(); // signal children for cookies
			} catch (InterruptedException ex) {
			} finally {
				// Release a permit
				momSem.release();
				lock.unlock(); // release the lock
			}
			return cookies;
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Child c1 = new Child("Adam");
		Thread t1 = new Thread(c1);
		t1.setName("Adam");
		Child c2 = new Child("Bob");
		Thread t2 = new Thread(c2);
		t2.setName("Bob");
		Child c3 = new Child("Caroline");
		Thread t3 = new Thread(c3);
		t3.setName("Caroline");
		Child c4 = new Child("Darcy");
		Thread t4 = new Thread(c4);
		t4.setName("Darcy");
		Mom mom = new Mom("Mom");
		Thread tmom = new Thread(mom);
		tmom.setName("Mom");
		tmom.start();
		t1.start();
		t2.start();
		t3.start();
		t4.start();

		while (!threadsTerminated) {
			threadsTerminated = t1.getState().equals(Thread.State.TERMINATED)
					& t2.getState().equals(Thread.State.TERMINATED) & t3.getState().equals(Thread.State.TERMINATED)
					& t4.getState().equals(Thread.State.TERMINATED) & tmom.getState().equals(Thread.State.TERMINATED);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(mom.getName() + ": made " + mom.getCookiesMade() + " cookies.");
		System.out.println(c1.getName() + ": ate " + c1.getCookiesEaten() + " cookies.");
		System.out.println(c2.getName() + ": ate " + c2.getCookiesEaten() + " cookies.");
		System.out.println(c3.getName() + ": ate " + c3.getCookiesEaten() + " cookies.");
		System.out.println(c4.getName() + ": ate " + c4.getCookiesEaten() + " cookies.");
		System.out.println(room.getBalance() + " cookies are left on the plate.");

	}

}

package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	static final int oahu=0;
	static final int molokai=1;
	static int boatLocation=oahu;
	static int passenger=0;
	static int childrenOahu=0;
	static int adultsOahu=0;
	static int childrenMolokai=0;
	static int adultsMolokai=0;

	static Lock lock = new Lock();
	static Condition2 waitOahu = new Condition2(lock);
	static Condition2 waitMolokai = new Condition2(lock);
	static Condition2 waitBoatFull = new Condition2(lock);
	static Communicator communicator = new Communicator();

	public static void selfTest() {
		BoatGrader b = new BoatGrader();


		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
	}

	public static void begin( int adults, int children, BoatGrader b ) {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
		bg = b;

	// Instantiate global variables here
		adultsOahu=adults;
		adultsMolokai=0;
		childrenOahu=children;
		childrenMolokai=0;

	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

		Runnable r = new Runnable() {
			public void run() {
				SampleItinerary();
			}
		};

		Runnable tChild = new Runnable() {
			public void run() {
				int location = oahu;
				ChildItinerary(location);
			}
		};

		Runnable tAdult = new Runnable() {
			public void run() {
				int location = oahu;
				AdultItinerary(location);
			}
		};

		for (int i=0;i<children;i++) {
			KThread t = new KThread(tChild);
			t.setName("Child Boat Thread:" + (i+1));
			t.fork();
		}

		for (int i=0;i<children;i++) {
			KThread t = new KThread(tAdult);
			t.setName("Adult Boat Thread:" + (i+1));
			t.fork();
		}

		while(true) {
			int received=communicator.listen();

			System.out.println("Received: " + received);
			if (received==children+adults) {
				break;
			}
		}

		KThread t = new KThread(r);
		t.setName("Sample Boat Thread");
		t.fork();
	}

	static void AdultItinerary(int location) {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/

	}

	static void ChildItinerary(int location) {
		lock.acquire();

		while(true) {
			if (location==oahu) {
				while(boatLocation!=oahu || passenger==2 || (adultsOahu>0 && childrenOahu==1)) {
					waitOahu.sleep();
				}

				waitOahu.wakeAll();

				if (adultsOahu==0 && childrenOahu==1) {
					childrenOahu--;
					bg.ChildRideToMolokai();

					boatLocation=molokai;
					location=molokai;
					childrenMolokai++;

					passenger=0;
					communicator.speak(childrenMolokai+adultsMolokai);
					waitMolokai.sleep();
				} else if (childrenOahu>1) {
					passenger++;
					if (passenger==2) {
						waitBoatFull.wake();
						waitBoatFull.sleep();
						childrenOahu--;
						bg.ChildRideToMolokai();
						passenger-=2;
						boatLocation=molokai;
						childrenMolokai++;
						communicator.speak(childrenMolokai+adultsMolokai);
						waitMolokai.wakeAll();
						waitMolokai.sleep();
					} else if (passenger==1) {
						waitBoatFull.sleep();
						childrenOahu--;
						bg.ChildRowToMolokai();
						location=molokai;
						childrenMolokai++;
						waitBoatFull.wake();
						waitMolokai.sleep();
					} else if (location == molokai) {
						while (boatLocation!=molokai) {
							waitMolokai.sleep();
						}

						childrenMolokai--;
						bg.ChildRowToOahu();
						boatLocation = oahu;
						location=oahu;
						childrenOahu++;

						waitOahu.wakeAll();
						waitOahu.sleep();
					}
				}
			}
		}

		//lock.release();
	}

	static void SampleItinerary() {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
}

package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat 
{
        static BoatGrader bg;
        static Lock boatLock;              //Lock used for condition variables defined below
        static boolean oneOnBoat;          //true if the boat is already occupied by one child
        static Condition molokaiChild;     //child at Molokai sleeps on this CV unless woken up by an adult to ferry back to Oahu
        static Condition oahu_boatchild_wait_for_second; //To allow for sychronisation while two children are trying to climb into the boat
	static Semaphore childAtMolokai; //For adult to know that there is at least one child at Molokai before travelling there
        static Semaphore boat_reserve;   //To allow exclusive access to boat
        static Semaphore duo_boat_reserve; //To allow for two child travel in the boat
        static boolean complete = false;   //know if the process is complete
        static int crossedChildren = 0, crossedAdults = 0; //Number of children and adults who have crossed at a point of time

        public static void selfTest(int adults, int children) 
	{
                BoatGrader b = new BoatGrader();
                System.out.println("Testing with No. of adults: "+adults+"\tNo. of children: "+children+"\n");
                begin(adults,children,b);
        }

        public static void begin(int adults,int children,BoatGrader b)
	{
                bg = b;
                childAtMolokai = new Semaphore(0);
                boat_reserve = new Semaphore(1); //Boat to be reserved by pilot
                duo_boat_reserve = new Semaphore(2); //Two-child Oahu-Molokai ride
                boatLock = new Lock();
                oneOnBoat = false;
                molokaiChild = new Condition(boatLock);
                oahu_boatchild_wait_for_second = new Condition(boatLock);
                complete = false;
                crossedChildren = 0;
                crossedAdults = 0;
                for (int i = 0; i < adults; i++) //Create the required number of adult threads
		{
                        Runnable r = new Runnable() 
			{
                                public void run() 
				{
                                        AdultItinerary();
                                }
                        };
                        KThread t = new KThread(r);
                        t.setName("The adult No." + i);
                        t.fork();
                }

                for (int i = 0; i < children; i++)  //Create the required number of Child threads
		{
                        Runnable r = new Runnable() 
			{
                                public void run() 
				{
                                        ChildItinerary();
                                }
                        };
                        KThread t = new KThread(r);
                        t.setName("The child No." + i);
                        t.fork();
                }

                while (!complete) //This ensures that all the threads have at least been ready once
		{
                        complete = ((crossedAdults == adults) && (crossedChildren == children - 1));
                        KThread.yield(); //Main thread yields itself to give the other threads a chance to get ready
                }//Even if 

                bg.ChildRowToMolokai(); //End with a child rowing from Oahu to Molokai alone

        }

        /**
         * The algorithm is pretty simple.One child keeps going back and forth between Molokai and Oahu.
	 * If an adult is on Oahu he takes the boat and rows to Molokai given that there is at least one
	 * child already there. Once and adult is transported he stays at Molokai and a child brings the boat
	 * back to Oahu to see if anybody is remaining on Oahu.All trips from Oahu to Molokai are two-child
         * or one adult trips and all trips from Molokai to Oahu are single child trips.
         */

        static void AdultItinerary() 
	{
                childAtMolokai.P(); //wait for at least one child to be present at Molokai
                boat_reserve.P();   
                boatLock.acquire();
                bg.AdultRowToMolokai(); //Row to Molokai
                molokaiChild.wake();    //Wake up one of the sleeping children at Molokai to send him back on the boat
                crossedAdults++;        //Set the counters appropriately
                crossedChildren--;
                boatLock.release();
        }

        static void ChildItinerary() 
	{
                boolean atMolokai = false; //True if child is at Molokai.Child starts on Oahu
                while (!complete)          
		{
                        if (atMolokai)     //Code for child behaviour if at Molokai
			{
                                boatLock.acquire();
                                bg.ChildRowToOahu(); //Row to Oahu on the boat alone
                                atMolokai = false;
                                boatLock.release();
                                boat_reserve.V();    //Release the boat at Oahu
                        } 
			else 
			{
                                duo_boat_reserve.P(); //If there is to be a child in the trip from O to M, then it has to be a duo trip
                                boatLock.acquire();
                                if (oneOnBoat)        //If the child sees that one child is already on the boat
				{
                                        oneOnBoat = false;
                                        boatLock.release();
                                        boat_reserve.P(); //Pilot the boat
                                        boatLock.acquire();
                                        oahu_boatchild_wait_for_second.wake(); //Wake up the first child on the boat
                                        bg.ChildRowToMolokai(); //Row to Molokai
                                        atMolokai = true;       //Get down at Molokai and let the other rider be the pilot for the trip back to Oahu
                                        crossedChildren++;
                                        molokaiChild.sleep();   //Sleep in Molokai
                                } 
				else 
				{
                                        oneOnBoat = true;       //Tries to get onto the boat and sleep till the second child climbing to wake it up
                                        oahu_boatchild_wait_for_second.sleep();
                                        bg.ChildRideToMolokai(); //Ride to Molokai
                                        childAtMolokai.V();      //Dropped off a child at Molokai
                                        bg.ChildRowToOahu();     //Pilot the boat back to Oahu
                                        boat_reserve.V();        //Give up the boat 
                                        duo_boat_reserve.V();    //Give up reservations on the boat for the initial duo trip.
                                        duo_boat_reserve.V();    //This was not done earlier as we did not want anybody to climb in at Molokai
                                }
                                boatLock.release();
                        }
                }
        }
}
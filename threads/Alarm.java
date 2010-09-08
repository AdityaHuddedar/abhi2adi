package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
        private PriorityQueue<Long> wakeTimes; //PriorityQueue of wakeTimes to store the values in increasing order
        private HashMap<Long,LinkedList<KThread>> waiters; //A HashMap mapping the wakeTime to the list of waiters waiting till that time
        /**
         * Allocate a new Alarm. Set the machine's timer interrupt handler to this
         * alarm's callback.
         *
         * <p><b>Note</b>: Nachos will not function correctly with more than one
         * alarm.
         */
        public Alarm() {
                Machine.timer().setInterruptHandler(new Runnable() {
                        public void run() { timerInterrupt(); }
                });

                wakeTimes = new PriorityQueue<Long>();
                waiters = new HashMap<Long,LinkedList<KThread>>();
        }

        /**
         * The timer interrupt handler. This is called by the machine's timer
         * periodically (approximately every 500 clock ticks). Causes the current
         * thread to yield, forcing a context switch if there is another thread
         * that should be run.
         */
        public void timerInterrupt() {
                boolean intStatus = Machine.interrupt().disable();
		LinkedList<KThread> firstWaiters;
                if(wakeTimes.size() > 0){
                        long firstWakeTime = wakeTimes.peek();
			while (Machine.timer().getTime()>=firstWakeTime) //Browse the wakeTime queue till the wakeTime<=currentTime
			{
			    firstWaiters = waiters.get(firstWakeTime);
			    if(firstWaiters != null)
			    {
				    for(int i=0;i<firstWaiters.size();i++)
				    firstWaiters.get(i).ready();  //Ready all the threads waiting till the given wakeTime
				    
				    waiters.remove(firstWaiters); //Remove the newly awakened Thread List
				    wakeTimes.poll();             //and its wakeTime entry in the HashMap  
				    if(wakeTimes.size()==0)
				    break;                        //Break if the wakeTimes queue becomes empty
				    firstWakeTime=wakeTimes.peek();
				    
			    }
			}
                }
                Machine.interrupt().restore(intStatus);
                KThread.yield();
        }

        /**
         * Put the current thread to sleep for at least <i>x</i> ticks,
         * waking it up in the timer interrupt handler. The thread must be
         * woken up (placed in the scheduler ready set) during the first timer
         * interrupt where
         *
         * <p><blockquote>
         * (current time) >= (WaitUntil called time)+(x)
         * </blockquote>
         *
         * @param       x       the minimum number of clock ticks to wait.
         *
         * @see nachos.machine.Timer#getTime()
         */
        public void waitUntil(long x) {
		boolean intStatus = Machine.interrupt().disable();
                long wakeTime = Machine.timer().getTime() + x;
                KThread temp = KThread.currentThread();

		if(waiters.containsKey(wakeTime)) //wakeTime entry already present
		{
		    LinkedList templist = waiters.get(wakeTime);
		    templist.add(temp);
		    waiters.put(wakeTime,templist);
		}
		else
		{
		    wakeTimes.add(wakeTime);
		    LinkedList<KThread> templist = new LinkedList<KThread>();
		    templist.add(temp);
		    waiters.put(wakeTime,templist);
		}

                KThread.sleep();
                Machine.interrupt().restore(intStatus);
        }
	
	
	private static class alarmTest implements Runnable //Runnable Class for an alarmThread
	{
	    private long wTime;
	    Alarm a;
	    public alarmTest(long x,final Alarm a) {
	    wTime=x;
	    this.a=a;
	    }
	    
	    public void run() {
		//set wait time for thread
		a.waitUntil(wTime);
		//finished waiting
		System.out.println("Alarm Active! (time = "
		+Machine.timer().getTime()+")");
	    }
	}
        public static void selfTest(final Alarm a) 
        {        
		//Create 3 threads with different waitTimes
		KThread Thread1 = new KThread( new alarmTest(5000,a));
		KThread Thread2 = new KThread( new alarmTest(5200,a));
		KThread Thread3 = new KThread( new alarmTest(5400,a));
		
		Thread1.fork();
		Thread2.fork();
		Thread3.fork();
		
		Thread1.join();
		Thread2.join();
		Thread3.join();
        }
}
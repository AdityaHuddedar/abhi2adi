package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
        private PriorityQueue<Long> wakeTimes;
        private HashMap<Long,LinkedList<KThread>> waiters;
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
			while (Machine.timer().getTime()>=firstWakeTime)
			{
                        firstWaiters = waiters.get(firstWakeTime);
			
                        if(firstWaiters != null){
                                for(int i=0;i<firstWaiters.size();i++)
				    firstWaiters.get(i).ready();
				
                                // Remove the newly awakened thread, and it's corresponding
                                // wait time entry.
                                waiters.remove(firstWaiters);
                                wakeTimes.poll();
				if(wakeTimes.size()==0)
				    break;
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

		if(waiters.containsKey(wakeTime))
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

        public static void selfTest(final Alarm a) {
	        AlarmTest.runTest();
                /*System.out.println();
                System.out.println("Testing Alarm...");
                KThread t1 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("thread 1 waiting.");
                                a.waitUntil(10000250);
                                System.out.println("thread 1 waited.");
                        }
                });

                KThread t2 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("thread 2 waiting.");
                                a.waitUntil(10000250);
                                System.out.println("thread 2 waited.");
                        }
                });

                t1.fork();
                t2.fork();

                t1.join();
                t2.join();*/
        }
}
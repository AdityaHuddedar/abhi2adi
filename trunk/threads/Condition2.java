package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 
{
        /**
         * Allocate a new condition variable.
         *
         * @param       conditionLock   the lock associated with this condition
         *                              variable. The current thread must hold this
         *                              lock whenever it uses <tt>sleep()</tt>,
         *                              <tt>wake()</tt>, or <tt>wakeAll()</tt>.
         */
        public Condition2(Lock conditionLock) 
        {
                this.conditionLock = conditionLock;
                waitQueue = new LinkedList<KThread>();
        }

        /**
         * Atomically release the associated lock and go to sleep on this condition
         * variable until another thread wakes it using <tt>wake()</tt>. The
         * current thread must hold the associated lock. The thread will
         * automatically reacquire the lock before <tt>sleep()</tt> returns.
         */
        public void sleep() 
        {
                Lib.assertTrue(conditionLock.isHeldByCurrentThread());
                boolean intStatus = Machine.interrupt().disable();
                waitQueue.add(KThread.currentThread());
                conditionLock.release(); //Release the lock before going to sleep
                KThread.sleep();
                conditionLock.acquire(); //Acquire the lock after waking up
                Machine.interrupt().restore(intStatus);
        }

        /**
         * Wake up at most one thread sleeping on this condition variable. The
         * current thread must hold the associated lock.
         */
	
        public void wake() 
        {
                Lib.assertTrue(conditionLock.isHeldByCurrentThread());
                boolean intStatus = Machine.interrupt().disable();              
                if(!waitQueue.isEmpty())
		{
                        waitQueue.poll().ready(); //Make the status of the head of the waitQueue as ready and remove it
                }
                Machine.interrupt().restore(intStatus);         
        }

        /**
         * Wake up all threads sleeping on this condition variable. The current
         * thread must hold the associated lock.
         */
	
        public void wakeAll() 
        {       
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
                boolean intStatus = Machine.interrupt().disable();
                while(!waitQueue.isEmpty())
		{
			waitQueue.poll().ready(); //Ready all the threads in the waitQueue
		}
                Machine.interrupt().restore(intStatus);  
        }
	
	/**
		Allocate two threads and make one wake the other.We use a Semaphore to 
		maintain a 'happens-before' relationship.Thread 1 sleeps and waits for
		Thread 2 to wake it up.
	*/
		
        public static void selfTest(final Alarm a) 
        {
		
                System.out.println();
                System.out.println("Testing Condition2...");
                final Lock l = new Lock();
		final Semaphore s=new Semaphore(0);
                final Condition2 testCond = new Condition2(l);
		
                KThread t1 = new KThread(new Runnable()
                {
                        public void run()
                        {
                                System.out.println("Thread 1 sleeping...");
                                l.acquire();
                                testCond.sleep();
				s.P();
                                l.release();
                                System.out.println("Thread 1 awake.");
                        }
                });

                KThread t2 = new KThread(new Runnable()
                {
                        public void run()
                        {
				s.V();
                                System.out.println("Thread 2 waking thread 1 in a second or so...");
                                a.waitUntil(1000);
                                l.acquire();
                                testCond.wake();
                                l.release();
                                System.out.println("Thread 2 woke thread 1.");
                        }
                });

                t1.fork();
                t2.fork();
                t1.join();
                t2.join();
        }
        private Lock conditionLock;
        private LinkedList<KThread> waitQueue;
}

package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
* A scheduler that chooses threads based on their priorities.
*
* <p>
* A priority scheduler associates a priority with each thread. The next thread
* to be dequeued is always a thread with priority no less than any other
* waiting thread's priority. Like a round-robin scheduler, the thread that is
* dequeued is, among all the threads of the same (highest) priority, the
* thread that has been waiting longest.
*
* <p>
* Essentially, a priority scheduler gives access in a round-robin fashion to
* all the highest-priority threads, and ignores all other threads. This has
* the potential to
* starve a thread if there's always a thread waiting with higher priority.
*
* <p>
* A priority scheduler must partially solve the priority inversion problem; in
* particular, priority must be donated through locks, and through joins.
*/
public class PriorityScheduler extends Scheduler {
	/**
	* Allocate a new priority scheduler.
	*/
	public PriorityScheduler() {
	}
	
	/**
	* Allocate a new priority thread queue.
	*
	* @param   transferPriority        <tt>true</tt> if this queue should
	*                                  transfer priority from waiting threads
	*                                  to the owning thread.
	* @return  a new priority thread queue.
	*/
	public ThreadQueue newThreadQueue(boolean transferPriority) 
	{
		return new PriorityQueue(transferPriority);
	}
	
	/**
	* getPriority()
	*/
	public int getPriority(KThread thread) 
	{
		Lib.assertTrue(Machine.interrupt().disabled());	
		return getThreadState(thread).getPriority();
	}
	
	/**
	* getEffectivePriority()
	*/
	public int getEffectivePriority(KThread thread) 
	{
		Lib.assertTrue(Machine.interrupt().disabled());
		return getThreadState(thread).getEffectivePriority();
	}
	
	/**
	* setPriority()
	*/
	public void setPriority(KThread thread, int priority) 
	{
		Lib.assertTrue(Machine.interrupt().disabled());	
		Lib.assertTrue(priority >= priorityMinimum &&
		priority <= priorityMaximum);
		getThreadState(thread).setPriority(priority);
	}
	
	/**
	* The self test
	*/
	public static void selfTestRun(KThread t1, int t1p, KThread t2, int t2p)
	{

	    boolean int_state;

	    int_state = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(t1, t1p);
	    ThreadedKernel.scheduler.setPriority(t2, t2p);
	    Machine.interrupt().restore(int_state);

	    t1.setName("a").fork();
	    t2.setName("b").fork();
	    t1.join();
	    t2.join();

	}

	public static void selfTestRun(KThread t1, int t1p, KThread t2, int t2p, KThread t3, int t3p)
	{
	    boolean int_state;

	    int_state = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(t1, t1p);
	    ThreadedKernel.scheduler.setPriority(t2, t2p);
	    ThreadedKernel.scheduler.setPriority(t3, t3p);
	    Machine.interrupt().restore(int_state);

	    t1.setName("a").fork();
	    t2.setName("b").fork();
	    t3.setName("c").fork();
	    t1.join();
	    t2.join();
	    t3.join();
	}
	
	public static void selfTest() 
	{
	    KThread t1, t2, t3;
	    final Lock lock;
	    final Condition2 condition;

	    /**
	    * Case 1: Tests priority scheduler without donation
	    *
	    * This runs t1 with priority 4, and t2 with priority 7.
	    *
	    */

	    System.out.println("Case 1:");

	    t1 = new KThread(new Runnable()
	    {
		public void run()
		{
		    System.out.println(KThread.currentThread().getName() + " started working");
		    for (int i = 0; i < 10; ++i)
		    {
			System.out.println(KThread.currentThread().getName() + " working " + i);
			KThread.yield();
		    }
		    System.out.println(KThread.currentThread().getName() + " finished working");
		}
	    });

	    t2 = new KThread(new Runnable()
	    {
		public void run()
		{
		    System.out.println(KThread.currentThread().getName() + " started working");
		    for (int i = 0; i < 10; ++i)
		    {
			System.out.println(KThread.currentThread().getName() + " working " + i);
			KThread.yield();
		    }
		    System.out.println(KThread.currentThread().getName() + " finished working");
		}
	    });

	    selfTestRun(t1, 4, t2, 7);

	    /**
	    * Case 2: Tests priority scheduler without donation, altering
	    * priorities of threads after they've started running
	    *
	    * This runs t1 with priority 7, and t2 with priority 4, but
	    * half-way through t1's process its priority is lowered to 2.
	    *
	    */

	    System.out.println("Case 2:");

	    t1 = new KThread(new Runnable()
	    {
		public void run()
		{
		    System.out.println(KThread.currentThread().getName() + " started working");
		    for (int i = 0; i < 10; ++i)
		    {
			System.out.println(KThread.currentThread().getName() + " working " + i);
			KThread.yield();
			if (i == 4)
			{
			    System.out.println(KThread.currentThread().getName() + " reached 1/2 way, changing priority");
			    boolean int_state = Machine.interrupt().disable();
			    ThreadedKernel.scheduler.setPriority(2);
			    Machine.interrupt().restore(int_state);
			}
		    }
		    System.out.println(KThread.currentThread().getName() + " finished working");
		}
	    });

	    t2 = new KThread(new Runnable()
	    {
		public void run()
		{
		    System.out.println(KThread.currentThread().getName() + " started working");
		    for (int i = 0; i < 10; ++i)
		    {
			System.out.println(KThread.currentThread().getName() + " working " + i);
			KThread.yield();
		    }
		    System.out.println(KThread.currentThread().getName() + " finished working");
		}

	    });
	    selfTestRun(t1, 7, t2, 4);
	    
	    /**
	    * Case 3: Tests priority donation
	    *
	    * This runs t1 with priority 4, t2 with priority 5 and t3 with
	    * priority 7. t1 will wait on a lock, and while t2 would normally
	    * then steal all available CPU, priority donation will ensure that
	    * t3 is given control in order to help unlock t1.
	    *
	    */

	    System.out.println("Case 3:");

	    lock = new Lock();
	    condition = new Condition2(lock);

	    t1 = new KThread(new Runnable()
	    {
		public void run()
		{
		System.out.println(KThread.currentThread().getName() + " started working");
		lock.acquire();
		System.out.println(KThread.currentThread().getName() + " active");
		lock.release();
		System.out.println(KThread.currentThread().getName() + " done");
		}
	    });

	    t2 = new KThread(new Runnable()
	    {
		public void run()
		{
		    System.out.println(KThread.currentThread().getName() + " started working");
		    for (int i = 0; i < 3; ++i)
		    {
			System.out.println(KThread.currentThread().getName() + " working " + i);
			KThread.yield();
		    }
		    System.out.println(KThread.currentThread().getName() + " finished working");
		}

	    });

	    t3 = new KThread(new Runnable()
	    {
		public void run()
		{
		    System.out.println(KThread.currentThread().getName() + " started working");
		    lock.acquire();
		    boolean int_state = Machine.interrupt().disable();
		    ThreadedKernel.scheduler.setPriority(2);
		    Machine.interrupt().restore(int_state);
		    KThread.yield();
		    // t1.acquire() will now have to realise that t3 owns the lock it wants to obtain
		    // so program execution will continue here.
		    System.out.println(KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)");
		    lock.release();
		    KThread.yield();
		    lock.acquire();
		    System.out.println(KThread.currentThread().getName() + " active-again (should be after 'a' and 'b' done)");
		    lock.release();
		}
	    });
	    selfTestRun(t1, 4, t2, 5, t3, 7);
	}
	
	/**
	* The default priority for a new thread. Do not change this value.
	*/
	public static final int priorityDefault = 1;
	/**
	* The minimum priority that a thread can have. Do not change this value.
	*/
	public static final int priorityMinimum = 0;
	/**
	* The maximum priority that a thread can have. Do not change this value.
	*/
	public static final int priorityMaximum = 7;    
	
	/**
	* Return the scheduling state of the specified thread.
	*
	* @param   thread  the thread whose scheduling state to return.
	* @return  the scheduling state of the specified thread.
	*/
	public boolean increasePriority() 
	{
	    boolean intStatus = Machine.interrupt().disable();
			
	    KThread thread = KThread.currentThread();

	    int priority = getPriority(thread);
	    if (priority == priorityMaximum)
		return false;

	    setPriority(thread, priority+1);

	    Machine.interrupt().restore(intStatus);
	    return true;
	}
	
	public boolean decreasePriority() 
	{
	    boolean intStatus = Machine.interrupt().disable();
			
	    KThread thread = KThread.currentThread();

	    int priority = getPriority(thread);
	    if (priority == priorityMinimum)
		return false;

	    setPriority(thread, priority-1);

	    Machine.interrupt().restore(intStatus);
	    return true;
	}
	
	protected ThreadState getThreadState(KThread thread) 
	{
		if (thread.schedulingState == null) 
		{
			thread.schedulingState = new ThreadState(thread);
		}
		
		return (ThreadState) thread.schedulingState;
	}
	
	/**
	* A <tt>ThreadQueue</tt> that sorts threads by priority.
	*/
	protected class PriorityQueue extends ThreadQueue 
	{
		public KThread owner = null;
		public boolean transferPriority;
		private Queue<KThread> waitQueue; //waitQueue that sorts first according to priority and then according to sleepTimes (ascending)
		PriorityQueue(boolean transferPriority) 
		{
			this.transferPriority = transferPriority;
			waitQueue = new java.util.PriorityQueue<KThread>(1, new comparePriority()); //Initialize the waitQueue to a java.util.PriorityQueue with a custom comparator defined below
		}
		
		/**
		* The thread declares its intent to wait for access to the
		* "resource" guarded by this priority queue. This method is only called
		* if the thread cannot immediately obtain access.
		*
		* @param       thread       The thread
		*
		* @see nachos.threads.ThreadQueue#waitForAccess
		*/
		
		public void waitForAccess(KThread thread) 
		{
			Lib.assertTrue(Machine.interrupt().disabled());
                        ThreadState waiterState = getThreadState(thread);
                        waiterState.waitForAccess(this); //Call waitForAccess of ThreadState class
                        waitQueue.add(thread); //Add this thread to this waitQueue 
                        if(owner != null)
			{
			    getThreadState(owner).donatePriority(waiterState.getPriority()-getThreadState(owner).getPriority()); //See if the incoming thread has to donate priority to the owner
			}
		}
	
		public void print() 
		{
			for (Iterator i=waitQueue.iterator(); i.hasNext(); ) 
			{
				System.out.print((KThread) i.next() + " ");
			}
		}
		
		/**
		* The specified thread has received exclusive access, without using
		* <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
		* threads are waiting for access.
		*/
		
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitQueue.size()==0); //Can acquire resourse only if the waitQueue is empty
			owner = thread; //Set the owner of this priority queue to this thread
			getThreadState(owner).acquire(this); //Call the acquire method of ThreadState of this thread
		}
		
		/**
		* Select the next thread in the ThreadQueue
		*/
		
		public KThread nextThread() 
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			if (waitQueue.isEmpty())
			{
				owner=null;
				return null;
			}
			owner=waitQueue.peek(); //Set the head of the waitQueue as owner
			ThreadState waiterState = getThreadState(owner);
			getThreadState(owner).acquire(this); //Make the new owner acquire the queue
			return waitQueue.poll();  //return next thread
		}
		
		/**
		* Return the next thread that <tt>nextThread()</tt> would return,
		* without modifying the state of this queue.
		*
		* @return      the next thread that <tt>nextThread()</tt> would
		*              return.
		*/
		
		protected ThreadState pickNextThread() //Returns the ThreadState of the head of the PriorityQueue or null if the queue is empty
		{
			Lib.assertTrue(Machine.interrupt().disabled());
                        KThread thread = waitQueue.peek();
                        if(thread == null)
			{
                                return null;
                        }
                        else
			{
                                return getThreadState(thread);
                        }
		}
		
		/**Define a comparator for the java.util.PriorityQueue that sorts according
		in decreasing order of priority and then subsorts according to increasing order of
		sleepTime i.e the time at which the thread went to sleepTime
		*/
		
		
		public class comparePriority implements Comparator<KThread> 
		{
			public int compare(KThread left, KThread right) 
			{
				int leftP=getThreadState(left).getEffectivePriority();
				int rightP=getThreadState(right).getEffectivePriority();
				if(leftP<rightP)
					return 1; //Define the ordering such that threads are sorted in decreasing order of priority
				else if(leftP>rightP)
					return -1;
				else // If priorities are same consider the sleepTimes and arrange in ascending order
				{
				    if(getThreadState(left).getSleepTime()<getThreadState(right).getSleepTime())
					return -1;
				    else if(getThreadState(left).getSleepTime()>getThreadState(right).getSleepTime())
					return 1;
				    else
					return 0;
				}
			}
		}
		
		public Integer getMaxPriority()
		{
                        KThread thread = waitQueue.peek();
                        if(thread != null)
			{
                                return getThreadState(thread).getPriority();
                        }
                        else
			{
                                return null;
                        }
                }
		
	}
	
	
	/**
	* The scheduling state of a thread. This should include the thread's
	* priority, its effective priority, any objects it owns, and the queues
	* it's waiting for, etc. This is a convenience class so that
	* no modification to the KThread class are needed for a new scheduler.
	* Each scheduler keeps track of scheduler specific KThread information
	* in its own declaration of the ThreadState class.
	*
	* @see     nachos.threads.KThread#schedulingState
	*/
	protected class ThreadState 
	{
                protected int donation = 0; //Any donation that this thread must receive any of the members of its waitQueue
                protected int lastEffectivePriority = -1; //A flag that we maintain to know when do we have to calculate EffectivePriority
                protected PriorityQueue waitQueue; //The waitQueue corresponding to this thread
                /**
                 * Allocate a new <tt>ThreadState</tt> object and associate it with the
                 * specified thread.
                 *
                 * @param       thread  the thread this state belongs to.
                 */
                public ThreadState(KThread thread) 
                {
                        this.thread = thread;
                        setPriority(priorityDefault);
                }

                /**
                 * Return the priority of the associated thread.
                 *
                 * @return      the priority of the associated thread.
                 */
                public int getPriority() 
                {
                        return priority;
                }

                /**
                 * Return the effective priority of the associated thread.
                 *
                 * @return      the effective priority of the associated thread.
                 */
                public int getEffectivePriority() 
                {
                        if(waitQueue == null)
			{
                                return priority;
                        }
                        
                        if(lastEffectivePriority < 0) //If there is a need to update the last effective priority
			{
                                if(waitQueue != null) 
				{
                                        if(waitQueue.transferPriority) //If the queue concerned is a join or a lock waitQueue and not the readyQueue
					{
						lastEffectivePriority=Math.max(waitQueue.getMaxPriority(),priority+donation); //Maximum of the priority of the head of the queue and priority+proposed donation
                                        }
                                        else
					{
                                                return priority;
                                        }
                                }
                                
                                else
				{
                                        lastEffectivePriority = priority; //Set effective priority for the first time
                                }
                        }
                        return lastEffectivePriority;
                }

                /**
                 * Set the priority of the associated thread to the specified value.
                 *
                 * @param       priority        the new priority.
                 */
		
                public void setPriority(int priority) 
                {
                        if (this.priority == priority)
                                return;

                        this.priority = priority;
                        lastEffectivePriority = -1; //Possible need to update effective priority
                }

                /**
                 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
                 * the associated thread) is invoked on the specified priority queue.
                 * The associated thread is therefore waiting for access to the
                 * resource guarded by <tt>waitQueue</tt>. This method is only called
                 * if the associated thread cannot immediately obtain access.
                 *
                 * @param       waitQueue       the queue that the associated thread is
                 *                              now waiting on.
                 *
                 * @see nachos.threads.ThreadQueue#waitForAccess
                 */
                public void waitForAccess(PriorityQueue waitQueue) 
                {
                        this.waitQueue = waitQueue; //Assign the waitQueue in the ThreadState object to the passed PriorityQueue
			sleepTime=Machine.timer().getTime(); //Record the time at which the thread sleeps
                }
		
		public long getSleepTime()
		{
		    return this.sleepTime;
		}
		
                /**
                 * Called when the associated thread has acquired access to whatever is
                 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
                 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
                 * <tt>thread</tt> is the associated thread), or as a result of
                 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
                 *
                 * @see nachos.threads.ThreadQueue#acquire
                 * @see nachos.threads.ThreadQueue#nextThread
                 */
                public void acquire(PriorityQueue waitQueue) 
                {
                        waitQueue = null;
                        donation = 0;
                        lastEffectivePriority = -1; //Need to recalculate the effectivePriority as the leaving thread might take off its donation effects or 
                        //the thread is acquiring the resource and needs to calculate the effective priority for the first time.
                }

                public void donatePriority(int donation)
                {
                        if(donation > this.donation)
			{
                                this.donation = donation; //Update the donation value to the new value if it is greater
                        }
                        lastEffectivePriority = -1; //Need to update priority as possible 
                }

                /** The thread with which this object is associated. */    
                protected KThread thread;
                /** The priority of the associated thread. */
                protected int priority;
		private long sleepTime; //Time at which this thread sleeps
		
        }
}
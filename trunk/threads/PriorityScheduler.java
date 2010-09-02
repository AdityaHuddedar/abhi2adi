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
        public static void selfTest(final Alarm a){
                             System.out.println("Testing priority scheduler...");
                             KThread t1 = new KThread(new Runnable(){
                                     public void run(){
                                             Machine.interrupt().disable();
                                             KThread.sleep();
                                             Machine.interrupt().enable();
                                             System.out.println("t1 running");
                                     }
                             });
                             t1.setName("T1");
                             
                             KThread t2 = new KThread(new Runnable(){
                                     public void run(){
                                             Machine.interrupt().disable();
                                             KThread.sleep();
                                             Machine.interrupt().enable();
                                             System.out.println("t2 running");
                                     }
                             });
                             t2.setName("T2");
                             
                             KThread t3 = new KThread(new Runnable(){
                                     public void run(){
                                             Machine.interrupt().disable();
                                             KThread.sleep();
                                             Machine.interrupt().enable();
                                             System.out.println("t3 running");
                                     }
                             });
                             t3.setName("T3");               
                             
                             t1.fork();
                             t2.fork();
                             t3.fork();
                
                             Machine.interrupt().disable();
                             ThreadState s1 = (ThreadState)t1.schedulingState;
                             ThreadState s2 = (ThreadState)t2.schedulingState;
                             ThreadState s3 = (ThreadState)t3.schedulingState;
                             
                             s1.setPriority(2);
                             s2.setPriority(3);
                             s3.setPriority(priorityMaximum);
                             
                             t1.ready();
                             t2.ready();
                             t3.ready();
                             Machine.interrupt().enable();
                
                             t1.join();
                             t2.join();
                             t3.join();
        }

        /**
         * Allocate a new priority scheduler.
         */
        public PriorityScheduler() {
        }

        /**
         * Allocate a new priority thread queue.
         *
         * @param       transferPriority        <tt>true</tt> if this queue should
         *                                      transfer priority from waiting threads
         *                                      to the owning thread.
         * @return      a new priority thread queue.
         */
        public ThreadQueue newThreadQueue(boolean transferPriority) {
                return new PriorityQueue(transferPriority);
        }

        public int getPriority(KThread thread) {
                Lib.assertTrue(Machine.interrupt().disabled());

                return getThreadState(thread).getPriority();
        }

        public int getEffectivePriority(KThread thread) {
                Lib.assertTrue(Machine.interrupt().disabled());

                return getThreadState(thread).getEffectivePriority();
        }

        public void setPriority(KThread thread, int priority) {
                Lib.assertTrue(Machine.interrupt().disabled());

                Lib.assertTrue(priority >= priorityMinimum &&
                                priority <= priorityMaximum);

                getThreadState(thread).setPriority(priority);
        }

        public boolean increasePriority() {
                boolean intStatus = Machine.interrupt().disable();

                KThread thread = KThread.currentThread();

                int priority = getPriority(thread);
                if (priority == priorityMaximum)
                        return false;

                setPriority(thread, priority+1);

                Machine.interrupt().restore(intStatus);
                return true;
        }

        public boolean decreasePriority() {
                boolean intStatus = Machine.interrupt().disable();

                KThread thread = KThread.currentThread();

                int priority = getPriority(thread);
                if (priority == priorityMinimum)
                        return false;

                setPriority(thread, priority-1);

                Machine.interrupt().restore(intStatus);
                return true;
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
         * @param       thread  the thread whose scheduling state to return.
         * @return      the scheduling state of the specified thread.
         */
        protected ThreadState getThreadState(KThread thread) {
                if (thread.schedulingState == null)
                        thread.schedulingState = new ThreadState(thread);

                return (ThreadState) thread.schedulingState;
        }

        /**
         * A <tt>ThreadQueue</tt> that sorts threads by priority.
         */
        protected class PriorityQueue extends ThreadQueue {
                protected KThread owner;
                protected java.util.PriorityQueue<KThread> waitQueue;

                PriorityQueue(boolean transferPriority) {
                        this.transferPriority = transferPriority;
                        waitQueue =
                                new java.util.PriorityQueue<KThread>(10, new Comparator<KThread>(){
                                        public int compare(KThread t1, KThread t2){
                                                int p1 = getEffectivePriority(t1);
                                                int p2 = getEffectivePriority(t2);

                                                if(p1 == p2){
                                                        return 0;
                                                }else if(p1 > p2){
                                                        return -1;
                                                }else{
                                                        return 1;
                                                }
                                        }
                                });
                }

                public void waitForAccess(KThread thread) {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        ThreadState waiterState = getThreadState(thread);
                        waiterState.waitForAccess(this);

                        waitQueue.add(thread);
                        if(owner != null){
                                getThreadState(owner).donatePriority(waiterState.getPriority());
                        }
                }

                public void acquire(KThread thread) {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        getThreadState(thread).acquire(this);
                        owner = thread;
                }

                public KThread nextThread() {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        
                        ArrayList<KThread> potentialNextThreads = new ArrayList<KThread>();

                        /** TODO
                         * ADD TIME WAITED HERE.
                         */
                        KThread next = waitQueue.poll();
                        potentialNextThreads.add(next);
                        while(getThreadState(waitQueue.peek()).getEffectivePriority() ==
                                getThreadState(next).getEffectivePriority()){
                                potentialNextThreads.add(waitQueue.poll());
                        }

                        if(next != null){
                                owner = next;
                        }
                        return next;
                }

                /**
                 * Return the next thread that <tt>nextThread()</tt> would return,
                 * without modifying the state of this queue.
                 *
                 * @return      the next thread that <tt>nextThread()</tt> would
                 *              return.
                 */
                protected ThreadState pickNextThread() {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        KThread thread = waitQueue.peek();
                        if(thread == null){
                                return null;
                        }else{
                                return getThreadState(thread);
                        }
                }

                public void print() {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        for(KThread t : waitQueue){
                                System.out.println(t.getName() + ": " +
                                                getThreadState(t).getPriority() + ", eff: " +
                                                getThreadState(t).getEffectivePriority());
                        }
                }

                public Integer getMaxPriority(){
                        KThread thread = waitQueue.peek();
                        if(thread != null){
                                return getThreadState(thread).getPriority();
                        }else{
                                return null;
                        }
                }

                /**
                 * <tt>true</tt> if this queue should transfer priority from waiting
                 * threads to the owning thread.
                 */
                public boolean transferPriority;
        }

        /**
         * The scheduling state of a thread. This should include the thread's
         * priority, its effective priority, any objects it owns, and the queue
         * it's waiting for, if any.
         *
         * @see nachos.threads.KThread#schedulingState
         */
        protected class ThreadState {
                protected int donation = 0;
                protected int lastEffectivePriority = -1;
                protected PriorityQueue waitQueue;
                /**
                 * Allocate a new <tt>ThreadState</tt> object and associate it with the
                 * specified thread.
                 *
                 * @param       thread  the thread this state belongs to.
                 */
                public ThreadState(KThread thread) {
                        this.thread = thread;

                        setPriority(priorityDefault);
                }

                /**
                 * Return the priority of the associated thread.
                 *
                 * @return      the priority of the associated thread.
                 */
                public int getPriority() {
                        return priority;
                }

                /**
                 * Return the effective priority of the associated thread.
                 *
                 * @return      the effective priority of the associated thread.
                 */
                public int getEffectivePriority() {
                        if(waitQueue == null){
                                return priority;
                        }

                        if(lastEffectivePriority < 1){
                                if(waitQueue != null){
                                        if(waitQueue.transferPriority){
                                                lastEffectivePriority = Math.max(
                                                                waitQueue.getMaxPriority(),
                                                                priority+donation);
                                        }else{
                                                return priority;
                                        }
                                }else{
                                        lastEffectivePriority = priority;
                                }
                        }

                        return lastEffectivePriority;
                }

                /**
                 * Set the priority of the associated thread to the specified value.
                 *
                 * @param       priority        the new priority.
                 */
                public void setPriority(int priority) {
                        if (this.priority == priority)
                                return;

                        this.priority = priority;
                        lastEffectivePriority = -1;
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
                public void waitForAccess(PriorityQueue waitQueue) {
                        this.waitQueue = waitQueue;
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
                public void acquire(PriorityQueue) {
                        waitQueue = null;
                        donation = 0;
                        lastEffectivePriority = -1;
                }

                public void donatePriority(int donation){
                        if(donation > this.donation){
                                this.donation = donation;
                        }
                        lastEffectivePriority = -1;
                }

                /** The thread with which this object is associated. */    
                protected KThread thread;
                /** The priority of the associated thread. */
                protected int priority;
        }
}

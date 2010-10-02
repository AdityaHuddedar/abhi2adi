package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.ThreadState;
import nachos.userprog.UThread;

import java.util.*;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
        public static final int priorityMinimum = 1;
        public static final int priorityMaximum = Integer.MAX_VALUE;

        /**
         * Allocate a new lottery scheduler.
         */
        public LotteryScheduler() {
        }
        
        /**
         * Return the scheduling state of the specified thread.
         *
         * @param       thread  the thread whose scheduling state to return.
         * @return      the scheduling state of the specified thread.
         */
        protected LotteryState getThreadState(KThread thread) {
                if (thread.schedulingState == null)
                        thread.schedulingState = new LotteryState(thread);

                return (LotteryState) thread.schedulingState;
        }

        /**
         * Increases a process's ticket count by 1
         */
        public boolean increasePriority() {
                boolean intStatus = Machine.interrupt().disable();
                LotteryState state = getThreadState(KThread.currentThread());
                
                // Can't increase if we're at max priority.
                if(state.getPriority() == priorityMaximum){
                        Machine.interrupt().restore(intStatus);
                        return false;
                }

                state.setPriority(state.getPriority() + 1);
                Machine.interrupt().restore(intStatus);
                return true;
        }

        /**
         * Decreases a process's ticket count by 1
         */
        public boolean decreasePriority() {
                boolean intStatus = Machine.interrupt().disable();
                LotteryState state = getThreadState(KThread.currentThread());
                
                // Can't decrease if we're at min priority.
                if(state.getPriority() == priorityMinimum){
                        Machine.interrupt().restore(intStatus);
                        return false;
                }

                state.setPriority(state.getPriority() - 1);
                Machine.interrupt().restore(intStatus);
                return true;
        }

        /**
         * Allocate a new lottery thread queue.
         *
         * @param       transferPriority        <tt>true</tt> if this queue should
         *                                      transfer tickets from waiting threads
         *                                      to the owning thread.
         * @return      a new lottery thread queue.
         */
        public ThreadQueue newThreadQueue(boolean transferPriority) {
                return new LotteryQueue(transferPriority);
        }

        protected class LotteryQueue extends ThreadQueue{
                public boolean transferPriority;
                private LinkedList<KThread> waiters;

                public LotteryQueue(boolean transferPriority){
                        this.transferPriority = transferPriority;
                        waiters = new LinkedList<KThread>();
                }

                public void acquire(KThread thread) {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        getThreadState(thread).acquire(this);
                }

                public KThread nextThread() {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        if(waiters.size() < 1){
                                return null;
                        }else{
                                int totalTickets;
                                if(transferPriority){
                                        totalTickets = getTotalEffectiveTickets();
                                }else{
                                        totalTickets = getTotalTickets();
                                }

                                int lotteryNumber = new Random().nextInt(totalTickets);
                                Collections.sort(waiters, new Comparator<KThread>(){
                                        public int compare(KThread t1, KThread t2){
                                                int p1;
                                                int p2;
                                                
                                                if(transferPriority){
                                                        p1 = getThreadState(t1).getEffectivePriority();
                                                        p2 = getThreadState(t2).getEffectivePriority();
                                                }else{
                                                        p1 = getThreadState(t1).getPriority();
                                                        p2 = getThreadState(t2).getPriority();
                                                }

                                                if(p1 < p2){
                                                        return -1;
                                                }else if(p1 > p2){
                                                        return 1;
                                                }else{
                                                        return 0;
                                                }
                                        }
                                });

                                int intervalStart = 0;
                                for(KThread t : waiters){
                                        int prio;
                                        if(transferPriority){
                                                prio = getThreadState(t).getEffectivePriority();
                                        }else{
                                                prio = getThreadState(t).getPriority();
                                        }

                                        // Use the random index above to pick a thread from the list.
                                        // Threads with many tickets will have wider intervals and
                                        // thus are more likely to be chosen.
                                        if(lotteryNumber >= intervalStart &&
                                                        lotteryNumber < intervalStart + prio){
                                                waiters.remove(t);
                                                return t;
                                        }else{
                                                intervalStart += prio;
                                        }
                                }
                        }

                        return null;
                }

                public void waitForAccess(KThread thread) {
                        Lib.assertTrue(Machine.interrupt().disabled());
                        LotteryState waiterState = getThreadState(thread);
                        waiterState.waitForAccess(this);

                        waiters.add(thread);
                }

                public void print() {
                        // Maybe implement later.
                }

                public int getTotalTickets(){
                        int sum = 0;
                        for(KThread t : waiters){
                                sum += getThreadState(t).getPriority();
                        }

                        return sum;
                }
                
                public int getTotalEffectiveTickets(){
                        int sum = 0;
                        for(KThread t : waiters){
                                sum += getThreadState(t).getEffectivePriority();
                        }

                        return sum;
                }
        }

        protected class LotteryState extends ThreadState {
                LotteryQueue waitQueue = null;

                public LotteryState(KThread thread) {
                        super(thread);
                }
                
                public int getEffectivePriority(){
                        if(waitQueue != null){
                                if(waitQueue.transferPriority){
                                        return waitQueue.getTotalTickets() + priority;
                                }else{
                                        return priority;
                                }
                        }else{
                                return priority;
                        }
                }

                public void acquire(LotteryQueue lotteryQueue) {
                        waitQueue = null;
                }
                
                public void waitForAccess(LotteryQueue lotteryQueue){
                        waitQueue = lotteryQueue;
                }
        }
}

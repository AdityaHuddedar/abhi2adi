package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
        private Lock commLock;
        private Condition2 readyCond;
        private Integer word;
        private boolean hasListener;
        private boolean hasSpoken;

        /**
         * Allocate a new communicator.
         */
        public Communicator() {
                commLock = new Lock();
                readyCond = new Condition2(commLock);
                word = null;
                hasListener = false;
                hasSpoken = false;
        }

        /**
         * Wait for a thread to listen through this communicator, and then transfer
         * <i>word</i> to the listener.
         *
         * <p>
         * Does not return until this thread is paired up with a listening thread.
         * Exactly one listener should receive <i>word</i>.
         *
         * @param       word    the integer to transfer.
         */
        public void speak(int word) {
                // If we have a listener, give them the word and wake them up.
                // Otherwise, sleep until the listener arrives.
                commLock.acquire();

                this.word = word;
                hasSpoken = true;

                if(hasListener){
                        readyCond.wakeAll();
                }else{
                        readyCond.sleep();
                }
                commLock.release();
        }

        /**
         * Wait for a thread to speak through this communicator, and then return
         * the <i>word</i> that thread passed to <tt>speak()</tt>.
         *
         * @return      the integer transferred.
         */    
        public int listen() {
                commLock.acquire();

                if(hasSpoken){
                        readyCond.wakeAll();

                        int temp = word;
                        word = null;

                        hasListener = false;
                        hasSpoken = false;
                        commLock.release();
                        return temp;
                }else{
                        hasListener = true;
                        readyCond.sleep();

                        int temp = word;
                        word = null;

                        hasListener = false;
                        hasSpoken = false;
                        readyCond.wakeAll();
                        commLock.release();
                        return temp;
                }
        }

        public static void selfTest(){
                System.out.println("Testing Communicator...");
                final Communicator c = new Communicator();

                KThread t1 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("Listening...");
                                int w = c.listen();
                                System.out.println("Got: " + w);
                        }
                });

                final Alarm a = new Alarm();
                KThread t2 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("Speaking word 134 soon...");
                                a.waitUntil(Machine.timer().getTime() + 5000000);
                                System.out.println("Speaking now.");
                                c.speak(134);
                                System.out.println("Spoke 134.");
                        }
                });

                t1.fork();
                t2.fork();

                t1.join();
                t2.join();

                t1 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("Listening soon...");
                                a.waitUntil(Machine.timer().getTime() + 5000000);
                                System.out.println("Listening now.");
                                int w = c.listen();
                                System.out.println("Got: " + w);
                        }
                });

                t2 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("Speaking word 392...");
                                c.speak(392);
                                System.out.println("Spoke 392.");
                        }
                });

                t1.fork();
                t2.fork();

                t1.join();
                t2.join();
        }
}

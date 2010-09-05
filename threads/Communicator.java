package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.There should always be equal number of speakers and listeners during the whole execution
 */
public class Communicator {
        private Lock commLock;
        private Condition2 readyCond;
        private Condition2 hasWord;
        //private Condition2 speakers;
        private Integer word;
        private int count;
        //private boolean hasListener;
        //private boolean hasSpoken;

        /**
         * Allocate a new communicator.
         */
        public Communicator() {
                commLock = new Lock();
                readyCond = new Condition2(commLock);
                hasWord=new Condition2(commLock);
                word = null;
                count=0;
                //hasListener = false;
                //hasSpoken = false;
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

                //count++;
                //hasSpoken = true;
                this.word=word;
                if(count>=0){
			count++;
			//this.word=word;
                        readyCond.sleep();
                        this.word=word;
                        hasWord.wake();
                        count--;
                }else{
			this.word=word;
                        readyCond.wake();
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
		if(count<=0){
			count--;
			readyCond.sleep();
			count++;
			commLock.release();
			return word;
			//return word;
//                         readyCond.wakeAll();
// 
//                         int temp = word;
//                         word = null;
// 
//                         hasListener = false;
//                         hasSpoken = false;
//                         commLock.release();
//                         return temp;
                }else{
                        //hasListener = true;
                       // count--;
			readyCond.wake();
                        hasWord.sleep();
                        commLock.release();
                        return word;

                        //int temp = word;
                        //word = null;

                        //hasListener = false;
                        //hasSpoken = false;
                        //readyCond.wakeAll();
                        
                        //return word;
                        //return temp;
                }
                
               
        }

        public static void selfTest(final Alarm a){
                System.out.println("Testing Communicator...");
                final Communicator c = new Communicator();

                KThread t2 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("Speaking word 134 soon...");
                                a.waitUntil(1000000);
                                System.out.println("Speaking now.");
                                c.speak(134);
                                /*c.speak(345);
				c.speak(900)*/;
                                System.out.println("Spoke 134.");
                        }
                });
               KThread t1 = new KThread(new Runnable(){
                        public void run(){
				//t2.join();
                                System.out.println("Listening...");
                                System.out.println("Got: " + c.listen());
                                //System.out.println("Got: " + c.listen());
                                //System.out.println("Got: " + c.listen());
                                
                        }
                });

               
		

                t1.fork();
                t2.fork();

                t1.join();
                t2.join();

                t1 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("Got a listener..t1.");
                                a.waitUntil(10000000);
                                System.out.println("Listening now.");
                                int w = c.listen();
                                System.out.println("t1Got: " + w);
                        }
                });
//                KThread t3 = new KThread(new Runnable(){
// 			public void run(){
// 				System.out.println("Got a listener..t3.");
// 				a.waitUntil(1000000);
// 				System.out.println("Listening now.");
// 				int w = c.listen();
// 				System.out.println("t3Got: " + w);
// 				}
// 				});

                t2 = new KThread(new Runnable(){
                        public void run(){
                                System.out.println("t2Speaking word 392...");
                                c.speak(392);
                                System.out.println("t2Spoke 392.");
                        }
                });
                KThread t4 = new KThread(new Runnable(){
			public void run(){
				System.out.println("t4Speaking word 392...");
				c.speak(392);
				System.out.println("t4Spoke 392.");
				}
				});

                t1.fork();
                //t3.fork();
                t2.fork();
                t4.fork();
                

                t1.join();
                //t3.join();
                t2.join();
                t4.join();
                
        }
}

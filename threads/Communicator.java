package nachos.threads;
import java.util.Random;
import nachos.machine.*;
import java.util.*;
/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.There should always be equal number of speakers and listeners during the whole execution
 */
public class Communicator 
{
        private Lock commLock; //Lock corresponding to the Communicator
        private Condition2 readyCond; //The CV on which speakers and listeners
        private Condition2 hasWord; //To ensure that the speaker has spoken before the listener listens
        private LinkedList<Integer> words; // List of words to be spoken by speakers waiting on the readyCond CV
        private int count; //Equals # of speakers - # of listeners
	
        /**
         * Allocate a new communicator.
         */
        public Communicator() 
        {
                commLock = new Lock();
                readyCond = new Condition2(commLock);
                hasWord=new Condition2(commLock);
                words = new LinkedList<Integer>();
                count=0;
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
        
	public void speak(int word) 
        {
                commLock.acquire();
                if(count>=0) //Only speakers waiting on the CV
		{
			count++; //Record that this speaker is going to go to sleep
                        readyCond.sleep();
			words.add(word); //Add the word to the wordList after a listener wakes it up
                        hasWord.wake(); //Signal on the hasWord CV that the word has been delivered to the wordList
                }
                else
		{
			words.add(word); //Listener(s) already present on the readyCond CV
			count++; //Record that a listener is ready to wake up
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
        public int listen() 
        {
                commLock.acquire();
		if(count<=0) //Only listeners present on the CV
		{
			count--; //Record that the listener is going to go to sleep
			readyCond.sleep();
			commLock.release(); //Woken up by a speaker. Release the speaker
			return words.remove(0); //Remove the word and return it
                }
                else
		{
			readyCond.wake(); //Only speakers waiting on the CV. Wake one up.
			count--; //Record that a speaker has woken up
			hasWord.sleep(); //Wait for the woken up speaker to give up its word
                        commLock.release();
                        return words.remove(0); //Remove the word and return it
                }      
        }
	
	/**We create 2 listeners and two speakers which run the RandomThread class.Each speaker and listener
	   get a chance to speak/listen 'numIter' times.We make a speaker/listener wait for a random interval of time
	   (between 500 and 1000) and then start speaking or listening.The output can be tracked on the console as in which
	   speaker gives its word to which listener.
	*/
	
	private static class RandomThread implements Runnable 
	{
		private String name; //Name
		private Communicator comm; //Communicator object
		private boolean isSpeaker; //Determine if speaker or listener
		private Random rng; //Random number generator
		private int numIter; //Number of times this thread speaks or listens
		
		RandomThread(String name, Communicator comm, boolean isSpeaker, Random rng,int numIter) 
		{
		    this.name = name;
		    this.comm = comm;
		    this.isSpeaker = isSpeaker;
		    this.rng = rng;
		    this.numIter=numIter;
		}

		public void run() 
		{
			System.out.println("Beginning:"+name);
			for (int i = 0; i < numIter; i++) 
			{
			    int randomDelay = 500 + rng.nextInt(500);
			    System.out.println("Sleeping:" + name + "\tuntil time="+ (randomDelay + Machine.timer().getTime()));
			    ThreadedKernel.alarm.waitUntil(randomDelay);
			    System.out.println("Finished waitUntil: "+name);

			    if (isSpeaker) 
			    {
				int randomWord = rng.nextInt(150);
				System.out.println(name + " speaking " + randomWord + "\t(Time="+ Machine.timer().getTime() + ")");
				comm.speak(randomWord);
				System.out.println(name + " spoke and returned "+randomWord+"\t(Time=" + Machine.timer().getTime() + ")");
			    }
			    
			    else 
			    {
				System.out.println(name + " listening\t(time=" + Machine.timer().getTime() + ")");
				int word = comm.listen();
				System.out.println(name + " listened and got " + word + "\t(Time="+ Machine.timer().getTime() + ")");
			    }
			}
			System.out.println(name + " exits.");
		}
	}
	
	public static void selfTest(final Alarm a)
	{
		    System.out.println("\nStarting Communicator testing\n");
		    Random rng = new Random();
		    int numIter=4;
		    Communicator comm = new Communicator();
		    KThread speak1 = new KThread(new RandomThread("Speaker 1",comm,true,rng,numIter));
		    KThread speak2 = new KThread(new RandomThread("Speaker 2",comm,true,rng,numIter));
		    KThread listen1 = new KThread(new RandomThread("Listener 1",comm,false,rng,numIter));
		    KThread listen2 = new KThread(new RandomThread("Listener 2",comm,false,rng,numIter));
		    speak1.fork();speak2.fork();listen1.fork();listen2.fork();
		    speak1.join();speak2.join();listen1.join();listen2.join();
		    System.out.println("\nCommunicator testing ends\n");

	}
                
}

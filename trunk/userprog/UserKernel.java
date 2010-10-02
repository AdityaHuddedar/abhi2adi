package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
        /**
         * Allocate a new user kernel.
         */
        public UserKernel() {
                super();
        }

        /**
         * Initialize this kernel. Creates a synchronized console and sets the
         * processor's exception handler.
         */
        public void initialize(String[] args) {
                super.initialize(args);

                freeListLock = new Lock();
                freePhysicalPages = new LinkedList<Integer>();

                for(int i=0; i<Machine.processor().getNumPhysPages(); i++){
                        freePhysicalPages.add(i);
                }

                console = new SynchConsole(Machine.console());

                Machine.processor().setExceptionHandler(new Runnable() {
                        public void run() { exceptionHandler(); }
                });
        }

        /**
         * Test the console device.
         */    
        public void selfTest() {
	  super.selfTest();
	UserProcess P=new UserProcess();
	byte[] data=new byte[20];
	byte[] in_data="success".getBytes();
	int k=P.writeVirtualMemory(200,in_data);
	int l=P.readVirtualMemory(200,data);	
	int n=P.handleSyscall(4,200,0,0,0);
	//int m=P.handleSyscall(4,200,0,0,0);
	int test=P.handleSyscall(5,200,0,0,0);
	//int y=P.handleSyscall(6,3,300,7,0);
	int x=P.handleSyscall(9,200,0,0,0);
	int test_close=P.handleSyscall(8,n,0,0,0);
	String s=P.readVirtualMemoryString(300,7);
	for(int i=0;i<in_data.length;i++) System.out.println((char)in_data[i]);
	System.out.println(n+" "+data[0]+" "+k+" "+l+" "+test+" "+x+" "+s+" "+test_close);
	
        }

        /**
         * Returns the current process.
         *
         * @return      the current process, or <tt>null</tt> if no process is current.
         */
        public static UserProcess currentProcess() {
                if (!(KThread.currentThread() instanceof UThread))
                        return null;

                return ((UThread) KThread.currentThread()).process;
        }

        /**
         * The exception handler. This handler is called by the processor whenever
         * a user instruction causes a processor exception.
         *
         * <p>
         * When the exception handler is invoked, interrupts are enabled, and the
         * processor's cause register contains an integer identifying the cause of
         * the exception (see the <tt>exceptionZZZ</tt> constants in the
         * <tt>Processor</tt> class). If the exception involves a bad virtual
         * address (e.g. page fault, TLB miss, read-only, bus error, or address
         * error), the processor's BadVAddr register identifies the virtual address
         * that caused the exception.
         */
        public void exceptionHandler() {
                Lib.assertTrue(KThread.currentThread() instanceof UThread);

                UserProcess process = ((UThread) KThread.currentThread()).process;
                int cause = Machine.processor().readRegister(Processor.regCause);
                process.handleException(cause);
        }

        /**
         * Start running user programs, by creating a process and running a shell
         * program in it. The name of the shell program it must run is returned by
         * <tt>Machine.getShellProgramName()</tt>.
         *
         * @see nachos.machine.Machine#getShellProgramName
         */
        public void run() {
                super.run();

                UserProcess process = UserProcess.newUserProcess();

                String shellProgram = Machine.getShellProgramName();
                Lib.assertTrue(process.execute(shellProgram, new String[] { }));

                KThread.finish();
        }

        /**
         * Terminate this kernel. Never returns.
         */
        public void terminate() {
                super.terminate();
        }

        public int getFreePage(){
                freeListLock.acquire();
                int freePage = freePhysicalPages.removeFirst();
                freeListLock.release();
                return freePage;
        }
       
        public void free(int ppn){
                freeListLock.acquire();
                Lib.assertTrue(!freePhysicalPages.contains(ppn),
                                "Duplicate free page entry: " + ppn + "\n" + freePhysicalPages);

                freePhysicalPages.add(ppn);
                freeListLock.release();
        }
       
        public int numFreePages(){
                freeListLock.acquire();
                int size = freePhysicalPages.size();
                freeListLock.release();
                return size;
        }

        /** Globally accessible reference to the synchronized console. */
        public static SynchConsole console;

        // dummy variables to make javac smarter
        private static Coff dummy1 = null;

        /**
         * List of free pages in physical memory. Initialized to the entire
         * physical address space.
         */
        protected Lock freeListLock;
        protected LinkedList<Integer> freePhysicalPages;
}


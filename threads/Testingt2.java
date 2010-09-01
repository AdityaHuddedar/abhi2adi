package nachos.threads;
class Testingt2 implements Runnable
{
    KThread temp=null;
    public void run()
    {
	System.out.println("T2 Starting....");
	System.out.println("T1 disabled....");
	System.out.println("T2 finished execution...");
	temp.join();
    }
    
    public Testingt2(KThread temp)
    {
	this.temp=temp;
    }
}
	   

  
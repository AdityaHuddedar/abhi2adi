package nachos.threads;
class Testing implements Runnable
{
	public void run()
	{
		for(int i=1;i<=10;i++)
		{
		  System.out.println(i);
		  if(i==5)
		  {
		    KThread t2=new KThread(new Testingt2(KThread.currentThread()));
		    t2.fork();
		  }
		}
	}
}
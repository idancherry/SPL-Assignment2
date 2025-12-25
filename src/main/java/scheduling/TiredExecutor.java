package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        
        workers = new TiredThread[numThreads];

        for(int i=0; i<numThreads; i++){
            workers[i] = new TiredThread(i, Math.random() + 0.5);
            idleMinHeap.add(workers[i]);
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
        if (task==null)
            throw new NullPointerException("Task is null.");

        inFlight.incrementAndGet();
        final TiredThread worker;

        try {
            worker = idleMinHeap.take();
        }catch (InterruptedException e) {
            inFlight.decrementAndGet();
            Thread.currentThread().interrupt();
            return;
        }
        try{
            worker.newTask(() -> {
                try {
                    task.run();
                } finally {
                    inFlight.decrementAndGet();
                    idleMinHeap.add(worker);
                    synchronized (TiredExecutor.this) {
                        TiredExecutor.this.notifyAll();
                    }
                }
            });
        }catch (RuntimeException ex){
            // worker returns & fixes counter even if task fails
            idleMinHeap.add(worker);
            inFlight.decrementAndGet();
            synchronized (TiredExecutor.this) {
                TiredExecutor.this.notifyAll();
            }
            throw ex;
        }

    }

    public void submitAll(Iterable<Runnable> tasks) {
        //submit tasks one by one and wait until all finish
       for (Runnable task : tasks){
            submit(task);
        }
        synchronized(this){
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // If thread is interrupted while waiting we should enforce the interrupt
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        for (TiredThread worker : workers){
            if (worker != null) {
                worker.shutdown();
            }
        }

        for (TiredThread worker : workers) {
            if (worker != null) {
                worker.join(); 
            }
        }
    }
    

    public synchronized String getWorkerReport() {
        StringBuilder sb = new StringBuilder();
        for (TiredThread worker : workers) {
            if (worker == null) continue;
            sb.append("Worker name=").append(worker.getName())
                    .append(", id=").append(worker.getWorkerId())
                    .append(", timeUsed(ns)=").append(worker.getTimeUsed())
                    .append(", timeIdle(ns)=").append(worker.getTimeIdle())
                    .append(", fatigue=").append(worker.getFatigue())
                    .append("\n");
        }
        return sb.toString();
    }
}

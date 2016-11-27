package br.com.example.bean;


import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static br.com.example.request.Request.GET;
import static br.com.example.request.Request.POST;

/**
 * Created by jordao on 27/11/16.
 */
public class Master {

    private final static Logger LOGGER = LoggerFactory.getLogger(Master.class);

    private String serialNumber;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnableFutures;

    public Master(String serialNumber, int minimumPullingInterval, int pullingOffset,
                  int minimumPushingInterval, int pushingOffset) {
        this.serialNumber = serialNumber;
        this.MINIMUM_PULLING_INTERVAL = minimumPullingInterval;
        this.PULLING_OFFSET = pullingOffset;
        this.MINIMUM_PUSHING_INTERVAL = minimumPushingInterval;
        this.PUSHING_OFFSET = pushingOffset;
    }

    public Master(String serialNumber) {
        this(serialNumber, 8, 4, 4, 6);
    }

    public void init() {
        runnableFutures = new ArrayList<Future>();
    }

    public void start() {
        Runnable puller = createPuller();
        Runnable pusher = createPusher();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        Future<?> pullerFuture = executorService.scheduleAtFixedRate(puller, 0, MINIMUM_PULLING_INTERVAL, TimeUnit.SECONDS);
        Future<?> pusherFuture = executorService.scheduleAtFixedRate(pusher, 0, MINIMUM_PUSHING_INTERVAL, TimeUnit.SECONDS);
        runnableFutures.add(pullerFuture);
        runnableFutures.add(pusherFuture);
    }

    public void stop() {
        Iterator<Future> runnableFutureIterator = runnableFutures.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            runnableFutures.clear();
            runnableFutures = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable createPuller() {
        return new MasterPuller();
    }

    private Runnable createPusher() {
        return new MasterPusher();
    }


    /**
     * executes GET /pull for centrals
     * @return
     */
    private String pull(){

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);

        String response = GET("/pull", headers);

        return response;
    }

    /**
     * executes POST /pc for centrals
     * @param message
     * @return
     */
    private String pc(Message message){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);
        headers.put("Application-ID", message.getApplicationID());
        String body = message.getMessage();

        String response = POST("/pc", headers, body);

        return response;
    }

    public void run() {
        String response = pull();
        LOGGER.info("PULL CENTRAL-SN [" + serialNumber + "]: " + response);
        if(!response.equals("{}")){
            Message message = (new Gson()).fromJson(response, Message.class);
            response = pc(message);
            LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
        }
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    class MasterPuller extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String response = pull();
            LOGGER.info("PULL CENTRAL-SN [" + serialNumber + "]: " + response);
            if(!response.equals("{}")){
                Message message = (new Gson()).fromJson(response, Message.class);
                response = pc(message);
                LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }


    class MasterPusher extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // do push
        }

        public void shutdown() {
            shutdown = true;
        }
    }

}

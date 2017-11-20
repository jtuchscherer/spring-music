package org.cloudfoundry.samples.music.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/errors")
public class ErrorController {
    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping(value = "/kill")
    public void kill() {
        logger.info("Forcing application exit");
        System.exit(1);
    }

    @RequestMapping(value = "/throw")
    public void throwException() {
        logger.info("Forcing an exception to be thrown");
        throw new NullPointerException("Forcing an exception to be thrown");
    }

    @RequestMapping(value = "/oom")
    public void throwOOM() throws InterruptedException {
        logger.info("Forcing an OOM situation");
        int iteratorValue = 20;
        logger.info("=================> OOM test started..");
        for (int outerIterator = 1; outerIterator < 20; outerIterator++) {
            logger.info("Iteration " + outerIterator + " Free Mem: " + Runtime.getRuntime().freeMemory());
            int loop1 = 2;
            int[] memoryFillIntVar = new int[iteratorValue];
            // feel memoryFillIntVar array in loop..
            do {
                memoryFillIntVar[loop1] = 0;
                loop1--;
            } while (loop1 > 0);
            iteratorValue = iteratorValue * 5;
            logger.info("\nRequired Memory for next loop: " + iteratorValue);
            Thread.sleep(1000);
        }
    }

    @RequestMapping(value = "/threadkill")
    public void killThreads() throws InterruptedException {
        logger.info("Starting threads until it ends");
        logger.info("=================> Thread test started..");
        for (int outerIterator = 1; outerIterator < 100000; outerIterator++) {
            logger.info("Iteration " + outerIterator + " Free Mem: " + Runtime.getRuntime().freeMemory());
            Thread thread = new MyThread();
            thread.start();
            Thread.sleep(10);
        }
    }
}

class MyThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MyThread.class);

    public void run() {
        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while sleeping", e);
        }
    }
}
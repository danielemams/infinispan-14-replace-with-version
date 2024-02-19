package com.edw.helper;

import jakarta.annotation.PostConstruct;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 *     com.edw.helper.GenerateCacheHelper
 * </pre>
 *
 * @author Muhammad Edwin < edwin at redhat dot com >
 * 29 Jan 2024 15:56
 */
@Service
public class GenerateCacheHelper {

    @Autowired
    private RemoteCacheManager cacheManager;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private List<String> listOfUuid = new ArrayList<>();

    @PostConstruct
    public void prepareData() {
        for (int i = 0; i < NUM_ENTRIES; i++) {
            listOfUuid.add(UUID.randomUUID().toString());
        }
    }

    public void initiate() {
        final RemoteCache cache = cacheManager.getCache("Balance");
        for (int i = 0; i < NUM_ENTRIES; i++) {
            cache.putIfAbsent(listOfUuid.get(i), new BigDecimal(1000));
        }
    }

    public void generate() {
        logger.info("starting ====================");
        final RemoteCache cache = cacheManager.getCache("Balance");
        for (int j = 0; j < NUM_EXECUTORS; j++) {
            executor.execute(() -> {
                for (int i = 0; i < NUM_ENTRIES; i++) {
                    while (true) {
                        Long timestamp = System.currentTimeMillis();
                        MetadataValue metadataValue = cache.getWithMetadata(listOfUuid.get(i));
                        BigDecimal newValue =
                                (new BigDecimal((String) metadataValue.getValue())).add(new BigDecimal(1000));
                        Boolean success = cache.replaceWithVersion(listOfUuid.get(i), newValue, metadataValue.getVersion());

                        if (success)
                            break;
                        else {
                            logger.info("{} printing {} version is {} for {}", success, listOfUuid.get(i),
                                    metadataValue.getVersion(),
                                    System.currentTimeMillis() - timestamp);
//                            try {
//                                Thread.sleep(new Random().nextInt(100));
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
                        }
                    }
                }
            });
        }
    }


    private static final int NUM_EXECUTORS = 9;
    private static final int NUM_ENTRIES = 10;

    /**
     *
     *  @author Daniele Mammarella <dmammare@redhat.com>
     */
    public void generate2() throws InterruptedException, ExecutionException {
        final RemoteCache<String, String> cache = cacheManager.getCache("Balance");
        long timeSpent = System.currentTimeMillis();
        final ExecutorService executor = Executors.newFixedThreadPool(NUM_EXECUTORS);
        final List<Future<Void>> results = new ArrayList<>(NUM_EXECUTORS * NUM_ENTRIES);
        for (int j = 0; j < NUM_EXECUTORS; j++) {
            for (int i = 0; i < NUM_ENTRIES; i++) {
                final MyCallable app = new MyCallable(logger, cache, listOfUuid.get(i));
                Future<Void> result = executor.submit(app);
                results.add(result);
            }
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        timeSpent = System.currentTimeMillis() - timeSpent;
        for (Future<Void> f : results) {
            f.get();
        }
        logger.info("Time spent: " + timeSpent / 1000.0 + " secs.");
    }

    /**
     *
     *  @author Daniele Mammarella <dmammare@redhat.com>
     */
    public void generate3() throws InterruptedException {
        final RemoteCache<String, String> cache = cacheManager.getCache("Balance");
        long timeSpent = System.currentTimeMillis();
        final ExecutorService executor = Executors.newFixedThreadPool(NUM_EXECUTORS);
        for (int j = 0; j < NUM_EXECUTORS; j++) {
            executor.execute(() -> {
                for (int i = 0; i < NUM_ENTRIES; i++) {
                    while (true) {
                        long timestamp = System.currentTimeMillis();
                        MetadataValue<String> metadataValue = cache.getWithMetadata(listOfUuid.get(i));
                        BigDecimal currentValue = new BigDecimal(metadataValue.getValue());
                        long currentVersion = metadataValue.getVersion();
                        String newValue = currentValue.add(new BigDecimal(1000)).toString();
                        if (cache.replaceWithVersion(listOfUuid.get(i), newValue, currentVersion)) {
                            break;
                        } else {
                            logger.info("false printing {} version is {} for {}",
                                    listOfUuid.get(i), currentVersion, System.currentTimeMillis() - timestamp);
                        }
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        timeSpent = System.currentTimeMillis() - timeSpent;
        logger.info("Time spent: " + timeSpent / 1000.0 + " secs.");
    }

    /**
     *
     *  @author Daniele Mammarella <dmammare@redhat.com>
     */
    static class MyCallable implements Callable<Void> {
        private final Logger logger;
        private final RemoteCache<String, String> cache;
        private final String key;

        MyCallable(
                final Logger logger,
                final RemoteCache<String, String> cache,
                final String key) {
            this.logger = logger;
            this.cache = cache;
            this.key = key;
        }

        @Override
        public Void call() {
            while (true) {
                long timestamp = System.currentTimeMillis();
                MetadataValue<String> metadataValue = cache.getWithMetadata(this.key);
                BigDecimal currentValue = new BigDecimal(metadataValue.getValue());
                long currentVersion = metadataValue.getVersion();
                String newValue = currentValue.add(new BigDecimal(1000)).toString();
                if (cache.replaceWithVersion(key, newValue, currentVersion)) {
                    return null;
                } else {
                    logger.info("false printing {} version is {} for {}",
                            this.key, currentVersion, System.currentTimeMillis() - timestamp);
                }
            }
        }
    }
}

package com.edw.controller;

import com.edw.helper.GenerateCacheHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

/**
 * <pre>
 *     com.edw.controller.IndexController
 * </pre>
 *
 * @author Muhammad Edwin < edwin at redhat dot com >
 * 29 Jan 2024 15:55
 */
@RestController
public class IndexController {

    @Autowired
    private GenerateCacheHelper generateCacheHelper;

    @GetMapping(path = "/initiate")
    public String init() {
        generateCacheHelper.initiate();
        return "good";
    }

    @GetMapping(path = "/process")
    public String process() {
        generateCacheHelper.generate();
        return "good";
    }

    /**
     *
     *  @author Daniele Mammarella <dmammare@redhat.com>
     */
    @GetMapping(path = "/process2")
    public String process2() throws InterruptedException, ExecutionException {
        generateCacheHelper.generate2();
        return "good";
    }

    /**
     *
     *  @author Daniele Mammarella <dmammare@redhat.com>
     */
    @GetMapping(path = "/process3")
    public String process3() throws InterruptedException {
        generateCacheHelper.generate3();
        return "good";
    }
}
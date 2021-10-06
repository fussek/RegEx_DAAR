package com.company;


import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class UnitTest {
    @Test
    public void consecutiveRequestsTest() {
        String[] regEx = new String[] {"S(a|g|r)rgon"};
        List<Long> performanceList = new ArrayList<>();
        for (int i=0; i<100; i++){
            long start2 = System.currentTimeMillis();
            try {
                RegEx.main(regEx);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            long end2 = System.currentTimeMillis();
            performanceList.add(end2-start2);
        }
        System.out.println("Elapsed Time in milli seconds for 100 requests: \n");
        System.out.println(performanceList);
    }

}



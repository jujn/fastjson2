package com.alibaba.fastjson2.benchmark.eishay;

import org.openjdk.jmh.infra.Blackhole;

public class EishayWriteUTF8BytesTest {
    static final Blackhole BH = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
    static final EishayWriteUTF8Bytes benchmark = new EishayWriteUTF8Bytes();

    public static void fastjson2() {
        for (int j = 0; j < 5; j++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10_000_000; ++i) {
                benchmark.fastjson2(BH);
            }
            long millis = System.currentTimeMillis() - start;
            System.out.println("fastjson2 millis : " + millis);
            // zulu8.58.0.13 : 2527
            // zulu11.52.13 : 337 314 289 2888 2606 2441 2026
            // zulu17.40.19 : 317 320 285 2446 2069
            // zulu17.40.19_vec : 267 250
            // zulu21.32.17 : 2081
        }
    }

    public static void wast() {
        for (int j = 0; j < 5; j++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10_000_000; ++i) {
                benchmark.wast(BH);
            }
            long millis = System.currentTimeMillis() - start;
            System.out.println("fastjson2 millis : " + millis);
            // zulu8.58.0.13 :
            // zulu11.52.13 :
            // zulu17.40.19 :
            // zulu21.32.17 : 3258
        }
    }

    public static void fastjson2_features() {
        for (int j = 0; j < 5; j++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10_000_000; ++i) {
                benchmark.fastjson2_features(BH);
            }
            long millis = System.currentTimeMillis() - start;
            System.out.println("fastjson2_features millis : " + millis);
            // zulu8.58.0.13 :
            // zulu11.52.13 :
            // zulu17.40.19 : 2393
            // zulu17.40.19_vec :
            // graalvm_17.0.7
        }
    }

    public static void jackson() throws Exception {
        for (int j = 0; j < 10; j++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; ++i) {
                benchmark.jackson(BH);
            }
            long millis = System.currentTimeMillis() - start;
            System.out.println("jackson millis : " + millis);
            // zulu8.58.0.13 : 641
            // zulu11.52.13 : 721
            // zulu17.40.19 : 667
        }
    }

    public static void gson() throws Exception {
        for (int j = 0; j < 10; j++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; ++i) {
                benchmark.gson(BH);
            }
            long millis = System.currentTimeMillis() - start;
            System.out.println("gson millis : " + millis);
            // zulu8.58.0.13 : 1569
            // zulu11.52.13 :
            // zulu17.32.13 :
        }
    }

    public static void main(String[] args) throws Exception {
//        wast();
        fastjson2();
//        fastjson2_features();
//        gson();
//        jackson();
    }
}

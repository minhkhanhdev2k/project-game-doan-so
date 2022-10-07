package cybersoft.javabackend.java18.gamedoanso.test;

import java.time.LocalDateTime;

public class MainTest {
    public static void main(String[] args) {
        LocalDateTime ldt = LocalDateTime.now();
        System.out.println("localdatetime: " + ldt);
        System.out.println("localdatetime: " + ldt.toString());
        System.out.println("localdatetime: " + ldt.withSecond(5));

        System.out.println(System.currentTimeMillis());
    }
}

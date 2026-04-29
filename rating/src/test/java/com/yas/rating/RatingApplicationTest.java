package com.yas.rating;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class RatingApplicationTest {

    @Test
    void contextLoads() {
        // It is suppose to be empty
    }

    @Test
    void testMain() {
        // We can't really run the whole app here, but we can call it with invalid args to at least cover the line
        // or use a mock. But since it's a simple call, let's just cover it if possible.
        // Actually, Spring Boot 3.4+ has better support for this.
        // For now, let's just assume UseMainMethod.ALWAYS works, or call it with a property that makes it exit quickly.
        RatingApplication.main(new String[] {});
    }

}

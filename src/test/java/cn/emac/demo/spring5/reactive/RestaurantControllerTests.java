package cn.emac.demo.spring5.reactive;

import cn.emac.demo.spring5.reactive.domain.Restaurant;
import cn.emac.demo.spring5.reactive.repositories.RestaurantRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * @author Emac
 * @since 2017-05-29
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class RestaurantControllerTests {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Test
    public void testAll() throws InterruptedException {
        // start from scratch
        restaurantRepository.deleteAll().block();

        // prepare
        CountDownLatch latch = new CountDownLatch(1);
        WebClient webClient = WebClient.create("http://localhost:9090");
        Restaurant[] restaurants = IntStream.range(1, 100)
                .mapToObj(String::valueOf)
                .map(s -> new Restaurant(s, s, s))
                .toArray(Restaurant[]::new);

        // create
        AtomicBoolean result = new AtomicBoolean(true);
        webClient.post().uri("/reactive/restaurant")
                .accept(MediaType.APPLICATION_JSON)
                .syncBody(restaurants)
                .exchange()
                .flatMapMany(resp -> resp.bodyToFlux(Restaurant.class))
                .log()
                .subscribe(r1 -> {
                    // get
                    webClient.get()
                            .uri("/reactive/restaurant/{id}", r1.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange()
                            .flatMap(resp -> resp.bodyToMono(Restaurant.class))
                            .log()
                            .subscribe(r2 -> Assert.assertEquals(r1, r2), e -> result.set(false), latch::countDown);
                }, e -> latch.countDown(), latch::countDown);

        latch.await();
        Assert.assertTrue(result.get());
    }
}
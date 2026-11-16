package io.github.sllhddn.incidentlab;

import io.github.sllhddn.incidentlab.incidents.inc01_npe.DiscountPolicy;
import io.github.sllhddn.incidentlab.incidents.inc01_npe.FixedDiscountPolicy;
import io.github.sllhddn.incidentlab.incidents.inc09_timezone.BusinessCalendar;
import io.github.sllhddn.incidentlab.incidents.inc09_timezone.FixedBusinessCalendar;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The running application always uses the fixed version of each incident. */
@Configuration
public class LabConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    BusinessCalendar businessCalendar(Clock clock, @Value("${lab.business-zone}") String zone) {
        return new FixedBusinessCalendar(clock, ZoneId.of(zone));
    }

    @Bean
    DiscountPolicy discountPolicy() {
        return new FixedDiscountPolicy();
    }
}

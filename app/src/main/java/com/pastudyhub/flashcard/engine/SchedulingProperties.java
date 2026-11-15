package com.pastudyhub.flashcard.engine;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Which scheduler decides when a card comes back, from the "scheduling" section of
 * application.yml. Both always run, so this only chooses whose date is used.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "scheduling")
public class SchedulingProperties {

    public enum Algorithm { SM2, FSRS }

    /** SM-2 stays the default until session 20 has measured which one predicts better. */
    private Algorithm algorithm = Algorithm.SM2;

    /**
     * For FSRS: the chance of remembering a card on the day it comes back. 0.9 is the
     * usual choice. Outside 0.7 to 0.97 the number of reviews gets unreasonable.
     */
    @DecimalMin("0.7")
    @DecimalMax("0.97")
    private double desiredRetention = 0.9;
}

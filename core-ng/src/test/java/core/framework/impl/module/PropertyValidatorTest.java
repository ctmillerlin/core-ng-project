package core.framework.impl.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
class PropertyValidatorTest {
    private PropertyValidator validator;

    @BeforeEach
    void createPropertyValidator() {
        validator = new PropertyValidator();
    }

    @Test
    void validate() {
        validator.usedProperties.add("app.usedKey");

        assertThatThrownBy(() -> validator.validate(Set.of("app.usedKey", "app.notUsedKey")))
                .isInstanceOf(Error.class)
                .hasMessageContaining("found not used properties")
                .hasMessageContaining("keys=[app.notUsedKey]");
    }
}

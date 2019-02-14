package core.framework.impl.module;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author neo
 */
public class PropertyValidator {
    final Set<String> usedProperties = new HashSet<>();

    void validate(Set<String> keys) {
        List<String> notUsedKeys = new ArrayList<>();
        for (String key : keys) {
            if (!usedProperties.contains(key)) notUsedKeys.add(key);
        }
        if (!notUsedKeys.isEmpty()) {
            throw new Error("found not used properties, please remove unnecessary config, keys=" + notUsedKeys);
        }
    }
}

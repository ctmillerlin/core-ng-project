package core.framework.internal.validate.type;

import java.lang.reflect.Field;

/**
 * @author neo
 */
public interface TypeVisitor {
    default void visitClass(Class<?> objectClass, String path) {
    }

    default void visitField(Field field, String parentPath) {
    }

    default void visitEnum(Class<?> enumClass) {
    }
}
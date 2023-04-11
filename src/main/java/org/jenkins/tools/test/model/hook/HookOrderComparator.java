package org.jenkins.tools.test.model.hook;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator that will order first based on the {@code @HookOrder}. Objects whose classes are
 * missing the annotation order will be treated as if they have the default order. Where two objects
 * have the same order sorting will occur based on the classname.
 * This Compatator is not {@code null} safe.
 */
public class HookOrderComparator implements Comparator<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Object left, Object right) {
        int leftOrder = getHookOrder(left);
        int rightOrder = getHookOrder(right);
        if (leftOrder < rightOrder) {
            return 1;
        }
        if (leftOrder > rightOrder) {
            return -1;
        }
        return left.getClass().getName().compareTo(right.getClass().getName());
    }

    private static int getHookOrder(Object obj) {
        HookOrder hookOrder = obj.getClass().getDeclaredAnnotation(HookOrder.class);
        return hookOrder == null ? 0 : hookOrder.order();
    }
}

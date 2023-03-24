package org.jenkins.tools.test.model.hook;

import java.util.Comparator;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataExtractor;

/**
 * Comparator that will order first based on the {@code HookOrder}. Objects whose classes are
 * missing an order will be treated as if they have the default order, Where two objects have the
 * same order sorting will occur based on the classname
 */
public class HookOrderComprator implements Comparator<PluginMetadataExtractor> {

    @Override
    public int compare(PluginMetadataExtractor left, PluginMetadataExtractor right) {
        int leftOrder = getHookOrder(left);
        int rightOder = getHookOrder(right);
        if (leftOrder < rightOder) {
            return 1;
        }
        if (leftOrder > rightOder) {
            return -1;
        }
        return left.getClass().getName().compareTo(right.getClass().getName());
    }

    private static int getHookOrder(PluginMetadataExtractor obj) {
        HookOrder hookOrder = obj.getClass().getDeclaredAnnotation(HookOrder.class);
        return hookOrder == null ? 0 : hookOrder.order();
    }
}

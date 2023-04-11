package org.jenkins.tools.test.model.hook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.comparator.ComparatorMatcherBuilder.comparedBy;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.Test;

class HookOrderComparatorTest {

    @Test
    void testIndividual() {
        HookOrderComparator hookOrderComparator = new HookOrderComparator();

        assertThat(new AlphaNoPriority(), comparedBy(hookOrderComparator).lessThan(new AlphaLowPriority()));
        assertThat(new AlphaNoPriority(), comparedBy(hookOrderComparator).greaterThan(new AlphaHighPriority()));

        assertThat(new AlphaNoPriority(), comparedBy(hookOrderComparator).lessThan(new BetaNoPriority()));
        assertThat(new AlphaNoPriority(), comparedBy(hookOrderComparator).lessThan(new GammaLowPriority()));

        assertThat(new AlphaNoPriority(), comparedBy(hookOrderComparator).comparesEqualTo(new AlphaNoPriority()));
    }

    @Test
    void testOrdering() {
        HookOrderComparator hookOrderComparator = new HookOrderComparator();
        List<?> priorityList;

        // annotation only
        priorityList = toSortedList(
                new AlphaLowPriority(),
                new AlphaHighPriority(),
                new BetaLowPriority(),
                new BetaHighPriority(),
                new GammaLowPriority(),
                new GammaHighPriority());
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(
                        new GammaHighPriority(),
                        new BetaHighPriority(),
                        new AlphaHighPriority(),
                        new AlphaLowPriority(),
                        new BetaLowPriority(),
                        new GammaLowPriority()));
        // sort is stable
        priorityList.sort(hookOrderComparator);
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(
                        new GammaHighPriority(),
                        new BetaHighPriority(),
                        new AlphaHighPriority(),
                        new AlphaLowPriority(),
                        new BetaLowPriority(),
                        new GammaLowPriority()));

        // no annotation - classname only
        priorityList = toSortedList(new GammaNoPriority(), new AlphaNoPriority(), new BetaNoPriority());
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(
                        new AlphaNoPriority(), new BetaNoPriority(), new GammaNoPriority()));
        // sort is stable
        priorityList.sort(hookOrderComparator);
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(
                        new AlphaNoPriority(), new BetaNoPriority(), new GammaNoPriority()));

        // mix of annotation and no annotation
        priorityList = toSortedList(new AlphaHighPriority(), new BetaLowPriority(), new GammaNoPriority());
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(
                        new AlphaHighPriority(), new GammaNoPriority(), new BetaLowPriority()));
        // sort is stable
        priorityList.sort(hookOrderComparator);
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(
                        new AlphaHighPriority(), new GammaNoPriority(), new BetaLowPriority()));

        // With Colliding annotations
        priorityList = toSortedList(new GammaHighPriority(), new DeltaCollidingHighPriority());
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(new DeltaCollidingHighPriority(), new GammaHighPriority()));
        // sort is stable
        priorityList.sort(hookOrderComparator);
        assertThat(
                priorityList,
                IsIterableContainingInOrder.contains(new DeltaCollidingHighPriority(), new GammaHighPriority()));
    }

    static class Base {
        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }

        @Override
        public boolean equals(Object arg0) {
            return this.getClass().equals(arg0.getClass());
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }
    }

    private List<Object> toSortedList(Object... objects) {
        ArrayList<Object> l = new ArrayList<>();
        for (Object o : objects) {
            l.add(o);
        }
        l.sort(new HookOrderComparator());
        return l;
    }

    @HookOrder(order = -10)
    static class AlphaLowPriority extends Base {}

    static class AlphaNoPriority extends Base {}

    @HookOrder(order = 10)
    static class AlphaHighPriority extends Base {}

    @HookOrder(order = -20)
    static class BetaLowPriority extends Base {}

    static class BetaNoPriority extends Base {}

    @HookOrder(order = 20)
    static class BetaHighPriority extends Base {}

    @HookOrder(order = -30)
    static class GammaLowPriority extends Base {}

    static class GammaNoPriority extends Base {}

    @HookOrder(order = 30)
    static class GammaHighPriority extends Base {}

    @HookOrder(order = 30)
    static class DeltaCollidingHighPriority extends Base {}
}

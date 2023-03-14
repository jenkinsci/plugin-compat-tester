package org.jenkins.tools.test.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

public class CheckoutInfo {

    @NonNull private final String connection;

    @NonNull private final String tag;

    public CheckoutInfo(@NonNull String connection, @NonNull String tag) {
        this.connection = connection;
        this.tag = tag;
    }

    @NonNull
    public String getConnection() {
        return connection;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckoutInfo that = (CheckoutInfo) o;
        return getConnection().equals(that.getConnection()) && getTag().equals(that.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConnection(), getTag());
    }
}

package net.flycamel.locationserver.domain;

public interface Indexable<K, S extends Comparable<S>> {
    K getKey();
    S getSecondKey();
}

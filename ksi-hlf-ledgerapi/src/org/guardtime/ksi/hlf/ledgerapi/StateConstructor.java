package org.guardtime.ksi.hlf.ledgerapi;

@FunctionalInterface
public interface StateConstructor {
    State make();
}

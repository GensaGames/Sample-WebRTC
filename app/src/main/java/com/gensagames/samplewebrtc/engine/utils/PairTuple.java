package com.gensagames.samplewebrtc.engine.utils;

/**
 * Created by GensaGames
 * GensaGames
 */

public class PairTuple<X, Y> {
    private final X x;
    private final Y y;

    public PairTuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public X getFirst() {
        return x;
    }

    public Y getSecond() {
        return y;
    }
}

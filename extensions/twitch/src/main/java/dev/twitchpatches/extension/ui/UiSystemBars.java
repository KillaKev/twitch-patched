package dev.twitchpatches.extension.ui;

final class UiSystemBars {
    private int original, owned;

    int hostFlags(int current) {
        return (current & owned) != owned ? current : (current & ~owned) | (original & owned);
    }

    int apply(int current, int requested) {
        if ((current & owned) != owned) original = current;
        else original = (original & owned) | (current & ~owned);
        int next = (current & ~owned) | (original & owned) | requested;
        owned = requested;
        return next;
    }
}

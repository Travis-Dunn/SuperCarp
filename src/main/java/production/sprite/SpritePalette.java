package production.sprite;

public final class SpritePalette {

    private boolean init;
    private int maxIdx;

    public int getMaxIdx() {
        assert(init);

        return maxIdx;
    }
}

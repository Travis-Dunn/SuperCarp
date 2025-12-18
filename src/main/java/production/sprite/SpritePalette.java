package production.sprite;

import java.util.Collections;
import java.util.Map;

public final class SpritePalette {
    public final int[] colors;
    public final Map<Integer, Integer> colorToIndex;
    public final int maxIdx;
    public final int count;

    SpritePalette(int[] colors, Map<Integer, Integer> colorToIndex) {
        this.colors = colors;
        this.colorToIndex = Collections.unmodifiableMap(colorToIndex);
        this.count = colors.length;
        this.maxIdx = this.count - 1;
    }

    public int getIndex(int colorPackedARGB) {
        return colorToIndex.getOrDefault(colorPackedARGB, -1);
    }
}
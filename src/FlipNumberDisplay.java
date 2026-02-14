public class FlipNumberDisplay {
    public enum WrapMode { WRAP, CLAMP }

    private int value;
    private final int min;
    private final int max;
    private final WrapMode wrapMode;

    public FlipNumberDisplay(int initial, int min, int max, WrapMode wrapMode) {
        this.value = initial;
        this.min = min;
        this.max = max;
        this.wrapMode = wrapMode;
    }

    public int getValue() {
        return value;
    }

    public void add(int delta) {
        setValue(value + delta);
    }

    public void setValue(int newValue) {
        if (wrapMode == WrapMode.WRAP) {
            int range = (max - min + 1);
            int normalized = (newValue - min) % range;
            if (normalized < 0) normalized += range;
            value = min + normalized;
        } else {
            value = Math.max(min, Math.min(max, newValue));
        }
    }
}
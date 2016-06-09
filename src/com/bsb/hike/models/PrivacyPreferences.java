package com.bsb.hike.models;

public class PrivacyPreferences {
    /**
     * Bit positions for configData. These positions start from the least significant bit on the right hand side
     */
    public static final byte LAST_SEEN = 0;

    public static final byte STATUS_UPDATE = 1;

    /**
     * Bit positions end here.
     */

    public static final int DEFAULT_VALUE = 0;

    private int config = DEFAULT_VALUE;

    public PrivacyPreferences(int config) {
        this.config = config;
    }

    private boolean isBitSet(int bitPosition) {
        return ((config >> bitPosition) & 1) == 1;
    }

    private void setBit(int bitPosition) {
        config = config | (1 << bitPosition);
    }

    private void resetBit(int bitPosition) {
        config = config & (~(1 << bitPosition));
    }

    public boolean shouldShowLastSeen() {
        return isBitSet(LAST_SEEN);
    }

    public boolean shouldShowStatusUpdate() {
        return isBitSet(STATUS_UPDATE);
    }

    public void setLastSeen(boolean showLastSeen) {
        if (showLastSeen)
            setBit(LAST_SEEN);
        else
            resetBit(LAST_SEEN);
    }

    public void setStatusUpdate(boolean showStatusUpdate) {
        if (showStatusUpdate)
            setBit(STATUS_UPDATE);
        else
            resetBit(STATUS_UPDATE);
    }


}

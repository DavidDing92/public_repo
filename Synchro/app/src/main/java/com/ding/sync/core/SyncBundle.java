package com.ding.sync.core;

import android.os.Bundle;

/**
 * Created by xwding on 4/27/2018.
 */

public interface SyncBundle {
    void fromBundle(Bundle parcel);
    Bundle toBundle();
}

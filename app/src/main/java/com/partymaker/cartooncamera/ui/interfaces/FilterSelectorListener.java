package com.partymaker.cartooncamera.ui.interfaces;

import com.partymaker.cartooncamera.lib.FilterType;

/**
 * Filter Selector Listener interface
 */
public interface FilterSelectorListener {

    /**
     * Callback method on selecting new filter from selector panel. The new filter type is passed as parameter.
     * @param filterType
     */
    void onFilterSelect(FilterType filterType);
}

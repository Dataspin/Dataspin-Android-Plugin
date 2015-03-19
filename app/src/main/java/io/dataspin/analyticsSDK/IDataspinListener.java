package io.dataspin.analyticsSDK;

import java.util.List;

/**
 * Created by rafal on 17.03.15.
 */
public interface IDataspinListener {
    void OnUserRegistered(String uuid);
    void OnDeviceRegistered();
    void OnSessionStarted();
    void OnSessionEnded();
    void OnItemPurchased(DataspinItem item);
    void OnEventRegistered(DataspinEvent event);
    void OnItemsListReceived(List<DataspinItem> itemsList);
    void OnEventsListReceived(List<DataspinEvent> eventsList);
    void OnError(DataspinError error);
}

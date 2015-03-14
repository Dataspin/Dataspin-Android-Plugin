package io.dataspin.analyticsSDK;

/**
 * Created by rafal on 14.03.15.
 */
public enum DataspinMethod {
    REGISTER_USER  (0),
    REGISTER_DEVICE (1),
    START_SESSION   (2),
    REGISTER_EVENT   (3),
    PURCHASE_ITEM (4),
    GET_ITEMS (5),
    GET_EVENTS (6),
    END_SESSION (7),
    REGISTER_OLD_SESSION (8)
    ;

    private final int methodCode;

    private DataspinMethod(int methodCode) {
        this.methodCode = methodCode;
    }
}

package com.mall.service.chat.intent;

public enum ChatIntent {
    SEARCH_PRODUCT("SEARCH_PRODUCT"),
    RECOMMEND_PRODUCT("RECOMMEND_PRODUCT"),
    ADD_TO_CART("ADD_TO_CART"),
    VIEW_CART("VIEW_CART"),
    BUY_NOW("BUY_NOW"),
    VIEW_ORDER("VIEW_ORDER"),
    PAY_GUIDE("PAY_GUIDE"),
    GENERAL_QA("GENERAL_QA");

    private final String code;

    ChatIntent(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

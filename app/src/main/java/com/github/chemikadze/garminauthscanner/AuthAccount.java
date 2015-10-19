package com.github.chemikadze.garminauthscanner;

import android.support.annotation.Nullable;

public final class AuthAccount {

    private String name;
    private String code;
    private String provider;

    public AuthAccount(String name, String code, @Nullable String provider) {
        this.name = name;
        this.code = code;
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Nullable
    public String getProvider() {
        return provider != null ? provider : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthAccount that = (AuthAccount) o;

        if (!code.equals(that.code)) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + code.hashCode();
        return result;
    }
}

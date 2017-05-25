package com.illumina.snaps.restsnappack;

import com.snaplogic.account.api.Account;
import com.snaplogic.account.api.AccountType;
import com.snaplogic.account.api.capabilities.AccountCategory;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.common.properties.builders.PropertyBuilder;
import com.snaplogic.snap.api.PropertyValues;
import org.apache.http.Header;

@AccountCategory(
        type = AccountType.NONE
)
public class RestDefaultAccount implements Account<Header> {
    public RestDefaultAccount() {
    }

    public Header connect() throws ExecutionException {
        return null;
    }

    public void disconnect() throws ExecutionException {
    }

    public void defineProperties(PropertyBuilder propertyBuilder) {
    }

    public void configure(PropertyValues propertyValues) {
    }
}

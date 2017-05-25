package com.illumina.snaps.restsnappack;

import com.snaplogic.account.api.AccountType;
import com.snaplogic.account.api.capabilities.AccountCategory;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.snap.api.account.oauth2.DynamicOauth2Account;
import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Version;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

@General(
        title = "REST Dynamic OAuth2 Account"
)
@Version(
        snap = 1
)
@AccountCategory(
        type = AccountType.CUSTOM
)
public class RestDynamicOauth2Account extends DynamicOauth2Account<Header> {
    private static final String BEARER_TOKEN = "%s %s";
    private static final String BEARER_PREFIX1 = "Bearer";
    private static final String BEARER_PREFIX2 = "bearer";

    public RestDynamicOauth2Account() {
    }

    public Header connect() {
        if(!this.isHeaderAuthenticated()) {
            return null;
        } else {
            String token = super.getAccessToken();
            return !token.startsWith("Bearer") && !token.startsWith("bearer")?new BasicHeader("Authorization", String.format("%s %s", new Object[]{"Bearer", token})):new BasicHeader("Authorization", token);
        }
    }

    public void disconnect() throws ExecutionException {
    }
}

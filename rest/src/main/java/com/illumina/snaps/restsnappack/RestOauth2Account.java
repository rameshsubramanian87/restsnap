
package com.illumina.snaps.restsnappack;

import com.google.common.collect.ImmutableSet.Builder;
import com.snaplogic.account.api.AccountType;
import com.snaplogic.account.api.AccountVariableProvider;
import com.snaplogic.account.api.capabilities.AccountCategory;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.common.utilities.ExpressionVariableAdapter;
import com.snaplogic.snap.api.account.oauth2.GenericOauth2Account;
import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Version;
import java.util.Map;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

@General(
        title = "REST OAuth2 Account"
)
@Version(
        snap = 1
)
@AccountCategory(
        type = AccountType.OAUTH2
)
public class RestOauth2Account extends GenericOauth2Account<Header> implements AccountVariableProvider {
    private static final String BEARER_TOKEN = "Bearer %s";

    public RestOauth2Account() {
    }

    public Header connect() {
        if(!this.isHeaderAuthenticated()) {
            return null;
        } else {
            String token = this.getAccessToken();
            return new BasicHeader("Authorization", String.format("Bearer %s", new Object[]{token}));
        }
    }

    public void disconnect() throws ExecutionException {
    }

    public Map<String, Object> getAccountVariableValue() {
        return new ExpressionVariableAdapter() {
            public Set<Entry<String, Object>> entrySet() {
                return (new Builder()).add(entry("access_token", RestOauth2Account.this.getAccessToken())).build();
            }
        };
    }
}

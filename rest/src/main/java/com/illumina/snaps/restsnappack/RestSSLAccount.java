package com.illumina.snaps.restsnappack;


import com.snaplogic.account.api.AccountType;
import com.snaplogic.account.api.capabilities.AccountCategory;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.snap.api.account.ssl.SSLAccount;
import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Version;
import org.apache.http.Header;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;

@General(
        title = "REST SSL Account"
)
@Version(
        snap = 1
)
@AccountCategory(
        type = AccountType.SSL
)
public class RestSSLAccount extends SSLAccount<Header> {
    public static final boolean IS_PROXY_AUTHORIZATION = false;

    public RestSSLAccount() {
    }

    public Header connect() throws ExecutionException {
        if(this.username != null) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.username, this.password);
            return BasicScheme.authenticate(credentials, "UTF-8", false);
        } else {
            return null;
        }
    }

    public void disconnect() throws ExecutionException {
    }
}


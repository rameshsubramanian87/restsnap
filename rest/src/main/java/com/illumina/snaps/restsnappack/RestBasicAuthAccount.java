package com.illumina.snaps.restsnappack;


import com.snaplogic.account.api.AccountType;
import com.snaplogic.account.api.capabilities.AccountCategory;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.snap.api.account.basic.BasicAuthAccount;
import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Version;
import org.apache.http.Header;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;

@General(
        title = "REST Basic Auth Account"
)
@Version(
        snap = 1
)
@AccountCategory(
        type = AccountType.BASIC_AUTH
)
public class RestBasicAuthAccount extends BasicAuthAccount<Header> {
    public RestBasicAuthAccount() {
    }

    public Header connect() throws ExecutionException {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        return BasicScheme.authenticate(credentials, "UTF-8", false);
    }

    public void disconnect() throws ExecutionException {
    }
}

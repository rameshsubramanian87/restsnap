package com.illumina.snaps.restsnappack;


import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Version;


@General(
        title = "REST Put",
        purpose = "Issues an HTTP PUT to a REST API service endpoint."
)
@Version(
        snap = 1
)
public class Put extends RestWriteCommon {
    public Put() {
        super("PUT");
    }
}

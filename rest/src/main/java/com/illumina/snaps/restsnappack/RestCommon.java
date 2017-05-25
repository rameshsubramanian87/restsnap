
package com.illumina.snaps.restsnappack;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.snaplogic.account.api.Account;
import com.snaplogic.account.api.capabilities.Accounts;
import com.snaplogic.account.api.capabilities.MultiAccountBinding;
import com.snaplogic.api.ConfigurationException;
import com.snaplogic.api.DependencyManager;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.api.ViewProvider;
import com.snaplogic.common.SnapType;
import com.snaplogic.common.properties.SnapProperty;
import com.snaplogic.common.properties.SnapProperty.DecoratorType;
import com.snaplogic.common.properties.builders.PropertyBuilder;
import com.snaplogic.common.properties.builders.ViewBuilder;
import com.snaplogic.snap.api.Document;
import com.snaplogic.snap.api.DocumentUtility;
import com.snaplogic.snap.api.ErrorViews;
import com.snaplogic.snap.api.ExpressionProperty;
import com.snaplogic.snap.api.OutputViews;
import com.snaplogic.snap.api.PropertyValues;
import com.snaplogic.snap.api.SimpleSnap;
import com.snaplogic.snap.api.ViewCategory;
import com.snaplogic.snap.api.account.oauth2.Oauth2Provider;
import com.snaplogic.snap.api.capabilities.ViewType;
import com.snaplogic.snap.api.rest.RestHttpClient;
import com.snaplogic.snap.api.rest.RestRequestExecutor;
import com.snaplogic.snap.api.rest.RestResponseProcessor;
import com.snaplogic.snap.api.rest.RestResponseStreamProcessor;
import com.snaplogic.snap.api.rest.URLResolver;
import com.snaplogic.snap.api.rest.RestResponseProcessor.ResponseEntityType;
import com.snaplogic.snap.api.xml.XmlUtils;
import com.snaplogic.snap.api.xml.XmlUtilsImpl;
import com.illumina.snaps.restsnappack.RestBasicAuthAccount;
import com.illumina.snaps.restsnappack.RestDefaultAccount;
import com.illumina.snaps.restsnappack.RestDynamicOauth2Account;
import com.illumina.snaps.restsnappack.RestOauth2Account;
import com.illumina.snaps.restsnappack.RestSSLAccount;
import com.snaplogic.util.HttpHeaderUtils;
import com.snaplogic.util.JsonPathBuilder;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;

@Accounts(
        provides = {RestBasicAuthAccount.class, RestOauth2Account.class, RestDynamicOauth2Account.class, RestSSLAccount.class},
        optional = true
)
public abstract class RestCommon extends SimpleSnap implements MultiAccountBinding<Account<Header>>, DependencyManager, ViewProvider {
    private static final String HEADER_KEY_PROP = "headerKey";
    private static final String HEADER_VALUE_PROP = "headerValue";
    private static final String HTTP_HEADER_PROP = "header";
    private static final String QUERY_PARAM_PROP = "queryParam";
    private static final String QUERY_PARAM_VALUE_PROP = "queryParamValue";
    private static final String QUERY_PARAMS_PROP = "queryParams";
    private static final String TRUST_ALL_CERTS_PROP = "trustAllCerts";
    private static final String FOLLOW_REDIRECTS_PROP = "followRedirects";
    private static final String RAW_DATA_PROP = "rawData";
    private static final String RESPONSE_ENTITY_TYPE_PROP = "responseEntityType";
    private static final String MAX_REQUEST_ATTEMPTS_PROP = "retry";
    private static final int DEFAULT_MAX_REQUEST_ATTEMPTS = 5;
    private static final String RETRY_INTERVAL_PROP = "retryInterval";
    private static final int DEFAULT_RETRY_INTERVAL_IN_SEC = 3;
    private static final boolean TRUST_ALL_CERTS_DEFAULT = false;
    private static final boolean RAW_DATA_DEFAULT = false;
    private static final ResponseEntityType DEFAULT_RESPONSE_ENTITY_TYPE;
    private static final int DEFAULT_SOCKET_TIMEOUT_IN_SEC = 900;
    private List<Pair<String, ExpressionProperty>> queryParams;
    private static final String QUERY_PARAM_PATH;
    protected static final String SERVICE_URL_PROP = "serviceUrl";
    protected static final String PROCESS_ARRAY_PROP = "processArray";
    protected static final String HAS_NEXT_PROP = "hasNext";
    protected static final String NEXT_URL_PROP = "nextUrl";
    protected static final String PAGINATION_INTERVAL_PROP = "paginationInterval";
    protected static final int MIN_PROPERTY_LENGTH = 1;
    private static final Set<ResponseEntityType> RESPONSE_ENTITY_TYPES;
    protected ExpressionProperty serviceUrlProperty;
    protected List<Pair<ExpressionProperty, ExpressionProperty>> httpHeaders = new ArrayList();
    protected boolean trustAllCerts;
    private final String httpMethod;
    private URLResolver urlResolver;
    private boolean processArray = false;
    private static final String TIMEOUT = "timeout";
    private int timeout;
    protected boolean rawData;
    protected ResponseEntityType responseEntityType;
    protected int maxRetries;
    protected int retryInterval;
    protected ExpressionProperty hasNextExpression;
    protected ExpressionProperty nextUrlExpression;
    protected int paginationInterval;
    protected boolean showAllHeaders;
    protected boolean followRedirects;
    private KeyManager[] keyManagers;
    private TrustManager[] trustManagers;
    @Inject
    protected Account<Header> account;
    @Inject
    private RestHttpClient restHttpClient;
    @Inject
    private XmlUtils xmlUtils;
    private RestRequestExecutor restRequestExecutor;

    public RestCommon(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Module getManagedModule() {
        return new AbstractModule() {
            protected void configure() {
                this.bind(XmlUtils.class).to(XmlUtilsImpl.class);
            }
        };
    }

    public Module getManagedAccountModule(final Account<Header> account) {
        return new AbstractModule() {
            protected void configure() {
                if(account == null) {
                    this.bind(Key.get(new TypeLiteral() {
                    })).to(RestDefaultAccount.class).in(Scopes.SINGLETON);
                } else {
                    this.bind(Key.get(new TypeLiteral() {
                    })).toInstance(account);
                }

            }
        };
    }

    public final void defineProperties(PropertyBuilder builder) {
        this.initProperties(builder);
        builder.describe("trustAllCerts", "Trust all certificates", "Trust all certificates, such as self-signed certificates.").type(SnapType.BOOLEAN).defaultValue(Boolean.valueOf(false)).required().add();
        builder.describe("followRedirects", "Follow redirects", "HTTP redirects will be followed when enabled").type(SnapType.BOOLEAN).defaultValue(Boolean.TRUE).required().add();
        SnapProperty queryParam = builder.describe("queryParam", "Query parameter", "A query parameter").withMinLength(1).build();
        SnapProperty queryParamValue = builder.describe("queryParamValue", "Query parameter value", "A query parameter value.").expression(DecoratorType.ACCEPTS_SCHEMA).withMinLength(1).build();
        builder.describe("queryParams", "Query parameters", "Define query parameters and their values. The query parameters will be attached to the URL using the http query parameter syntax, separated by & and added as key=value").type(SnapType.TABLE).withEntry(queryParam).withEntry(queryParamValue).add();
        SnapProperty headerKey = builder.describe("headerKey", "Key", "An HTTP header key.").expression(DecoratorType.ACCEPTS_SCHEMA).withMinLength(1).build();
        SnapProperty headerValue = builder.describe("headerValue", "Value", "An HTTP header value.").expression(DecoratorType.ACCEPTS_SCHEMA).withMinLength(1).build();
        builder.describe("header", "HTTP header", "An HTTP header key-value pairs.").type(SnapType.TABLE).withEntry(headerKey).withEntry(headerValue).add();
        builder.describe("responseEntityType", "Response entity type", "Select the response entity type in the output document. \'TEXT\' produces an entity of string type and \'BINARY\', byte array type.").withAllowedValues(RESPONSE_ENTITY_TYPES).defaultValue(DEFAULT_RESPONSE_ENTITY_TYPE).add();
        builder.describe("rawData", "Raw data", "If checked, HTTP response content is written to the output view as is. If unchecked, it is parsed according to the content type specified in the response header. This property is ignored if the Process array property is checked.").type(SnapType.BOOLEAN).defaultValue(Boolean.valueOf(false)).hide().add();
        builder.describe("timeout", "Timeout", "The time in seconds to wait before aborting the request.").type(SnapType.INTEGER).withMinValue(0L).defaultValue(Integer.valueOf(900)).add();
        builder.describe("retry", "Maximum request attempts", "The maximum number of requests to attempt in case of connection failure.").type(SnapType.INTEGER).withMinValue(1L).defaultValue(Integer.valueOf(5)).add();
        builder.describe("retryInterval", "Retry request interval", "The time in seconds to wait before retrying the request.").type(SnapType.INTEGER).withMinValue(0L).defaultValue(Integer.valueOf(3)).add();
        this.addProperties(builder);
    }

    protected void addProperties(PropertyBuilder builder) {
    }

    public void configure(PropertyValues propertyValues) throws ConfigurationException {
        this.serviceUrlProperty = propertyValues.getAsExpression("serviceUrl");
        this.trustAllCerts = ((Boolean)propertyValues.get("trustAllCerts")).booleanValue();
        this.followRedirects = Boolean.TRUE.equals(propertyValues.get("followRedirects"));
        BigInteger timeoutValue = (BigInteger)propertyValues.get("timeout");
        this.timeout = timeoutValue != null && timeoutValue.intValue() != 0?timeoutValue.intValue():900;
        List params = (List)propertyValues.get("queryParams");
        ExpressionProperty keyProp;
        if(CollectionUtils.isNotEmpty(params)) {
            this.queryParams = Lists.newArrayListWithExpectedSize(params.size());
            Iterator httpHeader = params.iterator();

            while(httpHeader.hasNext()) {
                Map value = (Map)httpHeader.next();
                String object = (String)JsonPath.read(value, QUERY_PARAM_PATH, new Filter[0]);
                keyProp = propertyValues.getExpressionPropertyFor(value, "queryParamValue");
                this.queryParams.add(Pair.of(object, keyProp));
            }
        }

        List httpHeader1 = (List)propertyValues.get("header");
        if(httpHeader1 != null) {
            Iterator value1 = httpHeader1.iterator();

            while(value1.hasNext()) {
                Map object1 = (Map)value1.next();
                keyProp = propertyValues.getExpressionPropertyFor(object1, "headerKey");
                ExpressionProperty valueProp = propertyValues.getExpressionPropertyFor(object1, "headerValue");
                this.httpHeaders.add(Pair.of(keyProp, valueProp));
            }
        }

        this.configureAdditional(propertyValues);
        this.processArray = Boolean.TRUE.equals(propertyValues.get("processArray"));
        this.rawData = Boolean.TRUE.equals(propertyValues.get("rawData"));
        this.responseEntityType = this.getResponseEntityType(propertyValues);
        if(propertyValues.inImmediateMode() && this.timeout == 0) {
            throw (new ConfigurationException("Unsupported snap configuration")).withReason("A timeout must be set when running in an Ultra task").withResolution("Set a non-zero timeout value");
        } else {
            BigInteger value2 = (BigInteger)propertyValues.get("retry");
            this.maxRetries = value2 == null?5:value2.intValue();
            value2 = (BigInteger)propertyValues.get("retryInterval");
            this.retryInterval = value2 == null?0:value2.intValue();
            this.hasNextExpression = propertyValues.getAsExpression("hasNext");
            this.nextUrlExpression = propertyValues.getAsExpression("nextUrl");
            Object object2 = propertyValues.get("paginationInterval");
            if(object2 instanceof BigInteger) {
                this.paginationInterval = ((BigInteger)object2).intValue();
            }

            if(this.account instanceof RestSSLAccount) {
                this.keyManagers = ((RestSSLAccount)this.account).getKeyManagers();
                this.trustManagers = ((RestSSLAccount)this.account).getTrustManagers();
            }

        }
    }

    protected void configureAdditional(PropertyValues propertyValues) {
    }

    protected abstract void initProperties(PropertyBuilder var1);

    protected Header[] buildHeaders(Document document, Account<Header> account, DocumentUtility documentUtility, ErrorViews errorViews, List<Pair<ExpressionProperty, ExpressionProperty>> httpHeaderExpressionPairs) {
        Map oAuth2Properties = this.createOAuth2Properties(account);
        Map documentData = documentUtility.getAsMap(document, errorViews);
        if(documentData != null && oAuth2Properties != null) {
            documentData.putAll(oAuth2Properties);
        }

        List headerList = HttpHeaderUtils.buildHeaders(httpHeaderExpressionPairs, document, documentData, new BasicHeader("Accept", "*/*"));
        if(account != null) {
            Header authHeader = (Header)account.connect();
            if(authHeader != null) {
                headerList.add(authHeader);
            }
        }

        return (Header[])headerList.toArray(new Header[0]);
    }

    protected Map<String, Object> createOAuth2Properties(Account<Header> account) {
        return account instanceof Oauth2Provider?((Oauth2Provider)account).createURLAuthProperties():null;
    }

    protected final RestRequestExecutor getRequestExecutor(Document document) {
        if(this.restRequestExecutor == null) {
            Object headers;
            if(this.processArray) {
                headers = new RestResponseStreamProcessor(this.xmlUtils, this.httpMethod, this.outputViews, this.errorViews, this.documentUtility);
            } else {
                headers = (new RestResponseProcessor(this.xmlUtils, this.httpMethod, this.outputViews, this.errorViews, this.documentUtility)).withRawData(this.rawData).withResponseEntityType(this.responseEntityType);
            }

            ((RestResponseProcessor)headers).withShowAllHeaders(this.showAllHeaders);
            this.restRequestExecutor = this.createRestRequestExecutor((RestResponseProcessor)headers, this.xmlUtils, this.restHttpClient, this.documentUtility, this.outputViews, this.errorViews, this.httpMethod, this.timeout, this.trustAllCerts, this.maxRetries, this.retryInterval, this.followRedirects);
        }

        Header[] headers1 = this.buildHeaders(document, this.account, this.documentUtility, this.errorViews, this.httpHeaders);
        this.restRequestExecutor.withHeaders(headers1);
        this.setKeyAndTrustManagersForSSLAccounts(this.restHttpClient, this.account, this.keyManagers, this.trustManagers);
        return this.restRequestExecutor;
    }

    protected void setKeyAndTrustManagersForSSLAccounts(RestHttpClient restHttpClient, Account account, KeyManager[] keyManagers, TrustManager[] trustManagers) {
        if(account instanceof RestSSLAccount) {
            restHttpClient.withKeyManagers(keyManagers);
            restHttpClient.withTrustManagers(trustManagers);
        }

    }

    protected RestRequestExecutor createRestRequestExecutor(RestResponseProcessor restResponseProcessor, XmlUtils xmlUtils, RestHttpClient restHttpClient, DocumentUtility documentUtility, OutputViews outputViews, ErrorViews errorViews, String httpMethod, int timeout, boolean trustAllCerts, int maxRetries, int retryInterval, boolean followRedirects) {
        return (new RestRequestExecutor(xmlUtils, restHttpClient, documentUtility, outputViews, errorViews)).withMethod(httpMethod).withSocketTimeout(timeout).withTrustCerts(trustAllCerts).withResponseProcessor(restResponseProcessor).withMaxRetries(maxRetries).withRetryInterval(retryInterval).withFollowRedirects(followRedirects);
    }

    protected final URLResolver createURLResolver() {
        final Map authProperties = this.createOAuth2Properties(this.account);
        if(this.urlResolver == null) {
            this.urlResolver = new URLResolver() {
                public String resolveUrl(Document document) {
                    if(authProperties != null) {
                        Map url = RestCommon.this.documentUtility.getAsMap(document, RestCommon.this.errorViews);
                        if(url != null) {
                            authProperties.putAll(url);
                        }
                    }

                    String url1 = RestCommon.this.eval(document, authProperties, RestCommon.this.serviceUrlProperty);

                    try {
                        URIBuilder e = new URIBuilder(url1);
                        if(RestCommon.this.queryParams != null) {
                            Iterator msg1 = RestCommon.this.queryParams.iterator();

                            while(msg1.hasNext()) {
                                Pair paramPair = (Pair)msg1.next();
                                String paramKey = (String)paramPair.getLeft();
                                if(paramKey != null) {
                                    ExpressionProperty paramValueProp = (ExpressionProperty)paramPair.getRight();
                                    String paramValue = RestCommon.this.eval(document, authProperties, paramValueProp);
                                    e.addParameter(paramKey, paramValue);
                                }
                            }
                        }

                        return e.build().toString();
                    } catch (URISyntaxException var9) {
                        String msg = String.format("The provided URI is invalid: %s", new Object[]{url1});
                        throw (new ExecutionException(var9, msg)).withReason(msg).withResolution("Please verify the provided URI and parameters are syntactically correct");
                    }
                }
            };
        }

        return this.urlResolver;
    }

    private String eval(Document document, Map<String, Object> authProperties, ExpressionProperty expressionProperty) {
        Object value;
        if(authProperties != null) {
            value = expressionProperty.eval(document, authProperties);
        } else {
            value = expressionProperty.eval(document);
        }

        return value == null?null:String.valueOf(value);
    }

    public void defineViews(ViewBuilder viewBuilder) {
        ((ViewBuilder)viewBuilder.describe("input0")).type(ViewType.DOCUMENT).add(ViewCategory.INPUT);
        ((ViewBuilder)viewBuilder.describe("output0")).type(ViewType.DOCUMENT).add(ViewCategory.OUTPUT);
    }

    private ResponseEntityType getResponseEntityType(PropertyValues propertyValues) {
        String action = (String)propertyValues.get("responseEntityType");
        ResponseEntityType ResponseEntityType;
        if(StringUtils.isBlank(action)) {
            ResponseEntityType = DEFAULT_RESPONSE_ENTITY_TYPE;
        } else {
            ResponseEntityType = (ResponseEntityType)Enum.valueOf(ResponseEntityType.class, action);
        }

        return ResponseEntityType;
    }

    static {
        DEFAULT_RESPONSE_ENTITY_TYPE = ResponseEntityType.DEFAULT;
        QUERY_PARAM_PATH = (new JsonPathBuilder("queryParam")).appendValueElement().build();
        RESPONSE_ENTITY_TYPES = ImmutableSet.of(ResponseEntityType.DEFAULT, ResponseEntityType.TEXT, ResponseEntityType.BINARY);
    }
}



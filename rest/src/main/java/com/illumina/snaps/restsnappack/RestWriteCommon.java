package com.illumina.snaps.restsnappack;



import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.snaplogic.api.ConfigurationException;
import com.snaplogic.api.LifecycleCallback;
import com.snaplogic.api.LifecycleEvent;
import com.snaplogic.common.SnapType;
import com.snaplogic.common.properties.SnapProperty.DecoratorType;
import com.snaplogic.common.properties.builders.PropertyBuilder;
import com.snaplogic.snap.api.Document;
import com.snaplogic.snap.api.ExpressionProperty;
import com.snaplogic.snap.api.PropertyValues;
import com.snaplogic.snap.api.SnapCategory;
import com.snaplogic.snap.api.capabilities.Category;
import com.snaplogic.snap.api.capabilities.Errors;
import com.snaplogic.snap.api.capabilities.Inputs;
import com.snaplogic.snap.api.capabilities.Outputs;
import com.snaplogic.snap.api.capabilities.ViewType;
import com.snaplogic.snap.api.rest.RequestDataHandler;
import com.snaplogic.snap.api.rest.URLResolver;
import com.illumina.snaps.restsnappack.RestCommon;
import com.snaplogic.util.SnapObjectMapper;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

@Inputs(
        min = 0,
        max = 1,
        accepts = {ViewType.DOCUMENT}
)
@Outputs(
        min = 0,
        max = 1,
        offers = {ViewType.DOCUMENT}
)
@Errors(
        min = 1,
        max = 1,
        offers = {ViewType.DOCUMENT}
)
@Category(
        snap = SnapCategory.WRITE
)
public abstract class RestWriteCommon extends RestCommon implements LifecycleCallback {
    private static final String HTTP_ENTITY_PROP = "entity";
    private static final String SHOW_ALL_HEADERS_PROP = "showAllHeaders";
    private static final String HTTP_ENTITY_DEFAULT = "$";
    private static final String BATCH_SIZE = "batchSize";
    protected static final String UPLOAD_FILE_PROP = "uploadFile";
    protected static final String UPLOAD_FILE_KEY_PROP = "uploadFileKey";
    private int batchSize;
    private List<Object> batch;
    private Document lastBatchDocument;
    protected RequestDataHandler requestHandler;
    private ExpressionProperty httpEntity;
    protected URLResolver urlResolver;
    protected ExpressionProperty uploadFileExpr;
    protected ExpressionProperty uploadFileKeyExpr;
    @Inject
    private SnapObjectMapper snapObjectMapper;

    RestWriteCommon(String httpMethod) {
        super(httpMethod);
    }

    protected void initProperties(PropertyBuilder builder) {
        builder.describe("serviceUrl", "Service URL", "The service endpoint URL of the REST API.").required().expression(DecoratorType.ACCEPTS_SCHEMA).withMinLength(1).add();
        builder.describe("entity", "HTTP entity", "JSON path to the HTTP entity data in the input Map data, e.g. \'$entity\' if the value of the \'entity\' key in the input Map data is the HTTP entity data. Leave this property blank if there is no entity to send.").defaultValue("$").expression(DecoratorType.ENABLED_EXPRESSION).schemaAware(DecoratorType.ACCEPTS_SCHEMA).add();
        builder.describe("batchSize", "Batch size", "Defines the batch size of the request. The incoming documents will be accumulated in a list up to the defined batch size before it is submitted to the endpoint. Make sure to only set this if your REST endpoint expects a list.").type(SnapType.INTEGER).add();
        builder.describe("showAllHeaders", "Show all headers", "This property may be selected if multiple response headers have the same header name so that all header values can be shown in the output document. Please refer to the Snap Reference document for more details.").type(SnapType.BOOLEAN).add();
        this.initAdditionalProperties(builder);
    }

    protected void initAdditionalProperties(PropertyBuilder builder) {
    }

    protected void configureAdditional(PropertyValues propertyValues) {
        this.httpEntity = propertyValues.getAsExpression("entity");
        this.showAllHeaders = Boolean.TRUE.equals(propertyValues.get("showAllHeaders"));
        this.batchSize = propertyValues.getInt("batchSize").intValue();
        if(this.batchSize > 0) {
            this.requestHandler = new RestRequestHandler(this.httpEntity, this.snapObjectMapper, true);
            this.batch = Lists.newArrayListWithExpectedSize(this.batchSize);
        } else {
            this.requestHandler = new RestRequestHandler(this.httpEntity, this.snapObjectMapper, false);
        }

        this.urlResolver = this.createURLResolver();
        if(propertyValues.inImmediateMode() && this.batchSize > 1) {
            throw (new ConfigurationException("Unsupported snap configuration")).withReason("Batching is not supported in an Ultra task").withResolution("Clear the batch size property");
        } else {
            this.uploadFileExpr = propertyValues.getAsExpression("uploadFile");
            this.uploadFileKeyExpr = propertyValues.getAsExpression("uploadFileKey");
        }
    }

    protected void process(Document document, String inputViewName) {
        if(this.batch != null && this.batch.size() == this.batchSize) {
            Document result = this.documentUtility.newDocumentFor(document, this.batch);
            this.getRequestExecutor(this.lastBatchDocument).execute(this.lastBatchDocument, result, this.urlResolver, this.requestHandler);
            this.batch.clear();
        }

        if(this.batch != null && this.batch.size() < this.batchSize) {
            this.lastBatchDocument = document;
            Object result1 = this.httpEntity.eval(document);
            this.batch.add(result1);
        } else {
            this.getRequestExecutor(document).execute(document, this.urlResolver, this.requestHandler);
        }

    }

    public void handle(LifecycleEvent event) {
        switch(event.ordinal()) {
            case 1:
                if(!CollectionUtils.isEmpty(this.batch)) {
                    Document batchDocument = this.documentUtility.newDocument(this.batch);
                    this.getRequestExecutor(this.lastBatchDocument).execute(this.lastBatchDocument, batchDocument, this.urlResolver, this.requestHandler);
                }

                this.lastBatchDocument = null;
            default:
        }
    }
}

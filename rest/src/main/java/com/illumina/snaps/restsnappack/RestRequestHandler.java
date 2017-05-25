package com.illumina.snaps.restsnappack;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.snap.api.Document;
import com.snaplogic.snap.api.ExpressionProperty;
import com.snaplogic.snap.api.rest.RequestDataHandler;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.StringEntity;

public class RestRequestHandler implements RequestDataHandler<StringEntity> {
    private final ObjectMapper objectMapper;
    private static final String BATCH = "batch";
    private final ExpressionProperty expressionProperty;
    private final boolean isBatch;

    public RestRequestHandler(ExpressionProperty expressionProperty, ObjectMapper objectMapper, boolean isBatch) {
        this.expressionProperty = expressionProperty;
        this.isBatch = isBatch;
        this.objectMapper = objectMapper;
    }

    public StringEntity createEntity(Document document) {
        if(this.expressionProperty != null) {
            if(this.expressionProperty.isEmpty()) {
                return null;
            } else if(this.isBatch) {
                List evalResult1;
                try {
                    evalResult1 = (List)document.get(List.class);
                } catch (ClassCastException var6) {
                    throw (new ExecutionException(var6, "Batch document must be of type List.")).withReason("Batch document must be of type List.").withResolutionAsDefect();
                }

                HashMap e = Maps.newHashMap();
                e.put("batch", document.get());
                document.set(e);

                try {
                    return this.createEntity(this.objectMapper.writeValueAsString(evalResult1));
                } catch (IOException var5) {
                    throw (new ExecutionException(var5, "Failed to convert input data to JSON string: %s")).formatWith(new Object[]{evalResult1.toString()}).withReason("Cannot serialize input data").withResolution("Please check the input data");
                }
            } else {
                Object evalResult = this.expressionProperty.eval(document);

                try {
                    return evalResult instanceof String?this.createEntity((String)evalResult):this.createEntity(this.objectMapper.writeValueAsString(evalResult));
                } catch (IOException var7) {
                    throw (new ExecutionException(var7, "Failed to convert input data to JSON string: %s")).formatWith(new Object[]{evalResult.toString()}).withReason("Cannot serialize input data").withResolution("Please check the input data");
                }
            }
        } else {
            return null;
        }
    }

    private StringEntity createEntity(String contents) throws UnsupportedEncodingException {
        return StringUtils.isNotBlank(contents)?new StringEntity(contents, "UTF-8"):null;
    }
}

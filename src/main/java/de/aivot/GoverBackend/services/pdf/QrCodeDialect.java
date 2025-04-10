package de.aivot.GoverBackend.services.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import de.aivot.GoverBackend.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class QrCodeDialect extends AbstractDialect implements IExpressionObjectDialect {
    public static final String id = "qrcode";


    public QrCodeDialect() {
        super(id);
    }

    public String create(Object link) throws WriterException, IOException {
        if (link == null || StringUtils.isNullOrEmpty(link.toString())) {
            return "";
        }

        var dimensions = 256;
        var bitMatrix = new QRCodeWriter().encode(link.toString(), BarcodeFormat.QR_CODE, dimensions, dimensions);

        var pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

        var image = Base64.encodeBase64String(pngOutputStream.toByteArray());

        return "data:image/png;base64," + image;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {

            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Collections.singleton(id);
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return new QrCodeDialect();
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}

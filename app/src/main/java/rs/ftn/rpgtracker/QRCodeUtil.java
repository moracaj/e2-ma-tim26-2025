package rs.ftn.rpgtracker;
import android.graphics.Bitmap;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;
public class QRCodeUtil {
    public static Bitmap generate(String content, int size) throws Exception {
        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, size, size);
    }
}
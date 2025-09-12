
package rs.ftn.rpgtracker;
import android.graphics.Bitmap;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;
public class QRCodeUtil {
  public static Bitmap generate(String content,int size) throws Exception{
    BarcodeEncoder enc=new BarcodeEncoder();
    return enc.encodeBitmap(content, BarcodeFormat.QR_CODE, size, size);
  }
}

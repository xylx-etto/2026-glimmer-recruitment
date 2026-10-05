import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class File {
    static void main(String[] args) {
        try (FileInputStream fileInputStream = new FileInputStream("doro.jpg");
             FileOutputStream fileOutputStream = new FileOutputStream("doro_copy.jpg")) {
            byte[] bytes=new byte[1024];
            int len;
            //System.out.println(fileInputStream.available());
            while ((len=fileInputStream.read(bytes))!=-1){
                fileOutputStream.write(bytes,0,len);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}

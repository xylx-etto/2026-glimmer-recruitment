import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Name {
    static void main(String[] args) {
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream("name.txt"), StandardCharsets.UTF_8));
            BufferedWriter bufferedWriter =new BufferedWriter(new OutputStreamWriter(new FileOutputStream("name_sorted.txt"),StandardCharsets.UTF_8))){
            List<String> str=new ArrayList<>();
            String l;
            while ((l=bufferedReader.readLine())!=null){
                if(!l.isBlank()){
                    String trim = l.trim();
                    str.add(trim);
                }
            }
            str.sort(Comparator.naturalOrder());
            for (String line : str){
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
